import { App } from 'antd';
import { FireFilled } from '@ant-design/icons';
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { houseApi } from '../api/houseApi';
import { kitApi } from '../api/kitApi';
import { useAuth } from '../auth/AuthContext';

const TelemetryContext = createContext(null);
const wsBase = import.meta.env.VITE_API_BASE_URL || window.location.origin;
const flameConfirmationCount = 3;

function parseFrame(frame) {
  const separator = frame.indexOf('\n\n');
  const head = separator < 0 ? frame : frame.slice(0, separator);
  const lines = head.replace(/\r/g, '').split('\n');
  const command = lines.shift() || '';
  const headers = Object.fromEntries(lines.filter(Boolean).map(line => {
    const colon = line.indexOf(':');
    return colon < 0 ? [line, ''] : [line.slice(0, colon), line.slice(colon + 1)];
  }));
  return { command, headers, body: separator < 0 ? '' : frame.slice(separator + 2) };
}

function connectStomp({ houseIds, token, onData, onReady, onStatus }) {
  const url = wsBase.replace(/^http/, 'ws').replace(/\/$/, '') + '/ws';
  let socket;
  let heartbeat;
  let retryTimer;
  let retryDelay = 1000;
  let disposed = false;
  let buffer = '';
  const send = frame => {
    if (socket?.readyState === WebSocket.OPEN) socket.send(`${frame}\0`);
  };

  const connect = () => {
    if (disposed) return;
    buffer = '';
    socket = new WebSocket(url);
    socket.onopen = () => send(`CONNECT\naccept-version:1.2\nheart-beat:0,10000\nAuthorization:Bearer ${token}\n\n`);
    socket.onmessage = event => {
      if (event.data === '\n' || event.data === '\r\n') return;
      buffer = (buffer + event.data).replace(/^[\r\n]+/, '');
      const frames = buffer.split('\0');
      buffer = frames.pop() || '';
      frames.filter(Boolean).forEach(rawFrame => {
        const { command, headers, body } = parseFrame(rawFrame.replace(/^[\r\n]+/, ''));
        if (command === 'CONNECTED') {
          retryDelay = 1000;
          onStatus(true);
          houseIds.forEach(houseId => send(`SUBSCRIBE\nid:house-${houseId}\ndestination:/topic/houses/${houseId}/data\nack:auto\n\n`));
          heartbeat = window.setInterval(() => {
            if (socket?.readyState === WebSocket.OPEN) socket.send('\n');
          }, 10000);
          onReady();
        } else if (command === 'MESSAGE') {
          const match = (headers.subscription || '').match(/^house-(\d+)$/)
            || (headers.destination || '').match(/houses\/(\d+)/);
          if (!match) return;
          try {
            onData(Number(match[1]), JSON.parse(body));
          } catch {
            // Ignore malformed server frames without interrupting the live connection.
          }
        } else if (command === 'ERROR') {
          socket.close();
        }
      });
    };
    socket.onclose = () => {
      window.clearInterval(heartbeat);
      onStatus(false);
      if (!disposed) {
        retryTimer = window.setTimeout(connect, retryDelay);
        retryDelay = Math.min(retryDelay * 2, 15000);
      }
    };
    socket.onerror = () => socket.close();
  };

  connect();
  return () => {
    disposed = true;
    window.clearTimeout(retryTimer);
    window.clearInterval(heartbeat);
    if (socket?.readyState === WebSocket.OPEN) send('DISCONNECT\n\n');
    socket?.close();
  };
}

async function loadAllHouses(signal) {
  const first = await houseApi.findAll(0, signal);
  const houses = [...(first.content || [])];
  const totalPages = Number(first.totalPages) || 1;
  for (let page = 1; page < totalPages; page++) {
    const result = await houseApi.findAll(page, signal);
    houses.push(...(result.content || []));
  }
  return houses;
}

export function TelemetryProvider({ children }) {
  const { user } = useAuth();
  const { modal, notification } = App.useApp();
  const navigate = useNavigate();
  const [houses, setHouses] = useState([]);
  const [connected, setConnected] = useState(false);
  const [latestByHouse, setLatestByHouse] = useState({});
  const [refreshVersion, setRefreshVersion] = useState(0);
  const eventSequence = useRef(0);
  const consecutiveFlameReadings = useRef(new Map());
  const previousFireByKit = useRef(new Map());

  useEffect(() => {
    const controller = new AbortController();
    setHouses([]);
    setConnected(false);
    loadAllHouses(controller.signal)
      .then(result => {
        if (!controller.signal.aborted) setHouses(result);
      })
      .catch(() => {
        if (!controller.signal.aborted) setHouses([]);
      });
    return () => controller.abort();
  }, [refreshVersion, user?.id]);

  const refreshHouses = useCallback(() => setRefreshVersion(version => version + 1), []);

  const houseIds = useMemo(() => houses.map(house => Number(house.id)).filter(Number.isSafeInteger), [houses]);
  const houseNames = useMemo(() => new Map(houses.map(house => [Number(house.id), house.houseName])), [houses]);

  useEffect(() => {
    const token = user?.accessToken || user?.token;
    if (!token || houseIds.length === 0) return undefined;

    return connectStomp({
      houseIds,
      token,
      onStatus: setConnected,
      onReady: () => {
        if (user.role === 'ADMIN') {
          Promise.allSettled(houseIds.map(houseId => kitApi.startHouseTelemetry(houseId)));
        } else {
          kitApi.startMyHouseTelemetry().catch(() => {});
        }
      },
      onData: (houseId, data) => {
        const reading = { ...data, receivedAt: new Date().toISOString() };
        const deviceKey = reading.kitId || reading.deviceUid || reading.location || 'unknown';
        const fireKey = `${houseId}:${deviceKey}`;
        const flameCount = reading.flame
          ? (consecutiveFlameReadings.current.get(fireKey) || 0) + 1
          : 0;
        consecutiveFlameReadings.current.set(fireKey, flameCount);
        const flameConfirmed = flameCount >= flameConfirmationCount;
        const modelFire = ['FIRE', 'EXTREME_FIRE'].includes(reading.status);
        const isFire = flameConfirmed || modelFire;
        const normalizedReading = {
          ...reading,
          flameConfirmed,
          alertSource: modelFire ? 'AI' : flameConfirmed ? 'FLAME_SENSOR' : null,
        };
        const sequence = ++eventSequence.current;
        setLatestByHouse(current => ({
          ...current,
          [houseId]: { houseId, data: normalizedReading, sequence },
        }));

        if (isFire && !previousFireByKit.current.get(fireKey)) {
          const houseName = houseNames.get(houseId) || `Nhà #${houseId}`;
          const location = reading.location ? ` tại ${reading.location}` : '';
          notification.error({
            key: `fire-${fireKey}`,
            message: `Cảnh báo cháy: ${houseName}`,
            description: `Phát hiện dấu hiệu cháy${location}. Hãy kiểm tra khu vực ngay.`,
            duration: 0,
            placement: 'topRight',
          });
          modal.confirm({
            title: 'CẢNH BÁO CHÁY',
            icon: <FireFilled style={{ color: '#EF4444' }} />,
            content: <div>
              <p><strong>{houseName}</strong>{location} đang có dấu hiệu cháy.</p>
              <p>Nhiệt độ: <strong>{reading.temperature ?? '—'} °C</strong> · Gas: <strong>{reading.gas ?? '—'} ppm</strong></p>
              <p>Hãy kiểm tra khu vực và thực hiện biện pháp an toàn ngay.</p>
            </div>,
            okText: 'Mở giám sát',
            cancelText: 'Đóng',
            centered: true,
            okButtonProps: { danger: true },
            onOk: () => navigate(`/app/houses/${houseId}/dashboard`, { state: { houseName } }),
          });
          if ('Notification' in window && window.Notification.permission === 'granted') {
            new window.Notification(`Cảnh báo cháy: ${houseName}`, {
              body: `Phát hiện dấu hiệu cháy${location}.`,
              icon: '/flame.svg',
              tag: `fire-${fireKey}`,
            });
          }
        }
        previousFireByKit.current.set(fireKey, isFire);
      },
    });
  }, [houseIds, houseNames, modal, navigate, notification, user?.accessToken, user?.role, user?.token]);

  const value = useMemo(
    () => ({ connected, latestByHouse, refreshHouses }),
    [connected, latestByHouse, refreshHouses],
  );
  return <TelemetryContext.Provider value={value}>{children}</TelemetryContext.Provider>;
}

export function useTelemetry() {
  const context = useContext(TelemetryContext);
  if (!context) throw new Error('useTelemetry must be used inside TelemetryProvider');
  return context;
}
