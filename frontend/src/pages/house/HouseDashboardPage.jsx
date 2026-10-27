import { useEffect, useRef, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { Alert, Badge, Button, Card, Col, Empty, Flex, Form, Input, Modal, Row, Select, Space, Statistic, Switch, Table, Tabs, Tag, Typography, message } from 'antd';
import { AlertOutlined, BulbOutlined, CloudOutlined, DashboardOutlined, FireOutlined, PlusOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { kitApi } from '../../api/kitApi';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import HouseMembers from './HouseMembers';

const initial = { temperature: null, humidity: null, gas: null, flame: false, status: 'NORMAL', confidence: null, location: '' };
const wsBase = import.meta.env.VITE_API_BASE_URL || window.location.origin;
const historyLimit = 100;

function connectStomp(houseId, token, onData, onStatus) {
  const url = wsBase.replace(/^http/, 'ws').replace(/\/$/, '') + '/ws';
  let socket;
  let heartbeat;
  let retryTimer;
  let retryDelay = 1000;
  let disposed = false;
  let buffer = '';
  const send = frame => { if (socket?.readyState === WebSocket.OPEN) socket.send(`${frame}\0`); };
  const connect = () => {
    if (disposed) return;
    buffer = '';
    socket = new WebSocket(url);
    socket.onopen = () => send(`CONNECT\naccept-version:1.2\nheart-beat:0,10000\nAuthorization:Bearer ${token}\n\n`);
    socket.onmessage = event => {
      buffer += event.data;
      const frames = buffer.split('\0');
      buffer = frames.pop() || '';
      frames.forEach(frame => {
        const separator = frame.indexOf('\n\n');
        const command = (separator < 0 ? frame : frame.slice(0, separator)).split('\n')[0];
        if (command === 'CONNECTED') {
          retryDelay = 1000;
          onStatus(true);
          send(`SUBSCRIBE\nid:house-${houseId}\ndestination:/topic/houses/${houseId}/data\nack:auto\n\n`);
          heartbeat = window.setInterval(() => { if (socket?.readyState === WebSocket.OPEN) socket.send('\n'); }, 10000);
        } else if (command === 'MESSAGE' && separator >= 0) {
          try { onData(JSON.parse(frame.slice(separator + 2))); } catch { /* Ignore malformed frames. */ }
        } else if (command === 'ERROR') socket.close();
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

export default function HouseDashboardPage() {
  const { houseId } = useParams();
  const location = useLocation();
  const { user } = useAuth();
  const houseName = location.state?.houseName || `Nhà #${houseId}`;
  const admin = user?.role === 'ADMIN';
  const [kitIds, setKitIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [kitError, setKitError] = useState(null);
  const [kitRetry, setKitRetry] = useState(0);
  const [connected, setConnected] = useState(false);
  const [sensor, setSensor] = useState(initial);
  const [hasTelemetry, setHasTelemetry] = useState(false);
  const [readings, setReadings] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [kitModal, setKitModal] = useState(false);
  const [commandBusy, setCommandBusy] = useState('');
  const [deviceState, setDeviceState] = useState({ pump: false, buzz: false, led: false });
  const [selectedKitId, setSelectedKitId] = useState(null);
  const previousFire = useRef(false);
  const [form] = Form.useForm();

  useEffect(() => {
    setLoading(true);
    setKitIds([]);
    setSelectedKitId(null);
    setConnected(false);
    setSensor(initial);
    setHasTelemetry(false);
    setReadings([]);
    setAlerts([]);
    setDeviceState({ pump: false, buzz: false, led: false });
    previousFire.current = false;
  }, [houseId]);

  useEffect(() => {
    let active = true;
    setKitError(null);
    (async () => {
      try {
        const ids = admin ? await kitApi.getHouseKits(houseId) : await kitApi.getMyHouseKits();
        if (active) {
          const validIds = Array.isArray(ids) ? ids : [];
          setKitIds(validIds);
          setSelectedKitId(validIds[0] ?? null);
        }
      } catch (error) {
        if (active && error.status !== 404) setKitError(error);
        if (active) setKitIds([]);
      } finally { if (active) setLoading(false); }
    })();
    return () => { active = false; };
  }, [houseId, admin, kitRetry]);

  useEffect(() => {
    if (!kitIds.length) return undefined;
    return connectStomp(houseId, user?.accessToken || user?.token || '', data => {
      const reading = { ...initial, ...data, receivedAt: new Date().toISOString() };
      const isFire = reading.flame || ['FIRE', 'EXTREME_FIRE'].includes(reading.status);
      setHasTelemetry(true);
      setSensor(reading);
      setReadings(current => [reading, ...current].slice(0, historyLimit));
      if (isFire) {
        setAlerts(current => [{ ...reading, id: `${reading.receivedAt}-${reading.location}` }, ...current].slice(0, historyLimit));
        if (!previousFire.current) message.error(`Cảnh báo cháy tại ${reading.location || houseName}.`);
      }
      previousFire.current = isFire;
    }, setConnected);
  }, [houseId, kitIds.length, user]);

  const fire = sensor.flame || ['FIRE', 'EXTREME_FIRE'].includes(sensor.status);
  const statusColor = fire ? 'red' : sensor.status === 'DANGER' ? 'orange' : sensor.status === 'UNNORMAL' ? 'gold' : 'green';
  const statusLabel = fire ? 'PHÁT HIỆN CHÁY' : ({ NORMAL: 'An toàn', UNNORMAL: 'Bất thường', DANGER: 'Nguy hiểm' }[sensor.status] || sensor.status);

  async function addKit(values) {
    try {
      const kit = await kitApi.addToHouse(houseId, values);
      setKitIds(current => [...current, kit.id]);
      setSelectedKitId(kit.id);
      setKitModal(false); form.resetFields();
      message.success(`Đã thêm kit ${kit.deviceUid}.`);
      // Opening the admin kit endpoint starts its telemetry stream.
      const ids = await kitApi.getHouseKits(houseId); setKitIds(ids || []);
    } catch (error) { message.error(error.message || 'Không thêm được kit.'); }
  }

  async function command(field, enabled) {
    const kitId = selectedKitId;
    if (!admin || !kitId) return;
    setCommandBusy(field);
    const states = { ...deviceState, [field]: enabled };
    try { await kitApi.sendCommand(kitId, states); setDeviceState(states); message.success('Đã gửi lệnh đến kit.'); }
    catch (error) { message.error(error.message || 'Gửi lệnh thất bại.'); }
    finally { setCommandBusy(''); }
  }

  return <>
    <PageHeader title={`Giám sát: ${houseName}`} description={<Space><Badge status={connected ? 'success' : 'default'} text={connected ? 'Đang nhận dữ liệu realtime' : 'Chưa kết nối realtime'} />{kitIds.length > 0 && <Tag color="blue">{kitIds.length} thiết bị</Tag>}</Space>} />
    {fire && <Alert type="error" showIcon icon={<FireOutlined />} message="CẢNH BÁO CHÁY" description="Thiết bị phát hiện dấu hiệu cháy. Hãy kiểm tra khu vực ngay." banner style={{ marginBottom: 20 }} />}
    {kitError && <Alert type="error" showIcon message={kitError.message || 'Không tải được danh sách thiết bị.'} action={<Button size="small" onClick={() => setKitRetry(value => value + 1)}>Thử lại</Button>} style={{ marginBottom: 16 }} />}
    {!loading && kitIds.length === 0 && <Alert type="info" showIcon message="Nhà chưa có kit được liên kết." description={admin ? 'Thêm kit để bắt đầu nhận dữ liệu cảm biến.' : 'Chưa tìm thấy thiết bị trong các nhà bạn tham gia.'} action={admin && <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => setKitModal(true)}>Thêm kit</Button>} style={{ marginBottom: 20 }} />}
    <Flex justify="space-between" align="center" wrap gap="small"><Typography.Title level={4}>Thông số môi trường</Typography.Title>{admin && <Button icon={<PlusOutlined />} onClick={() => setKitModal(true)}>Thêm kit</Button>}</Flex>
    {!hasTelemetry && kitIds.length > 0 && <Alert type="info" showIcon message="Đang chờ dữ liệu cảm biến đầu tiên" description="Các chỉ số sẽ xuất hiện khi kit gửi telemetry và kết nối realtime hoạt động." style={{ marginBottom: 16 }} />}
    <Row gutter={[16, 16]}>
      <Col xs={12} lg={6}><Card><Statistic title="Nhiệt độ" value={sensor.temperature ?? '—'} suffix={sensor.temperature == null ? '' : '°C'} prefix={<DashboardOutlined />} /></Card></Col>
      <Col xs={12} lg={6}><Card><Statistic title="Độ ẩm" value={sensor.humidity ?? '—'} suffix={sensor.humidity == null ? '' : '%'} prefix={<CloudOutlined />} /></Card></Col>
      <Col xs={12} lg={6}><Card><Statistic title="Nồng độ gas" value={sensor.gas ?? '—'} suffix={sensor.gas == null ? '' : 'ppm'} prefix={<AlertOutlined />} /></Card></Col>
      <Col xs={12} lg={6}><Card><Statistic title="Trạng thái" value={hasTelemetry ? statusLabel : 'Chưa có dữ liệu'} valueStyle={{ color: hasTelemetry ? statusColor : undefined, fontSize: 20 }} prefix={<FireOutlined />} /></Card></Col>
    </Row>
    <Card title="Điều khiển thiết bị" style={{ marginTop: 24 }} extra={<Space>{sensor.location && <Tag>{sensor.location}</Tag>}{admin && kitIds.length > 1 && <Select aria-label="Chọn kit điều khiển" value={selectedKitId} onChange={setSelectedKitId} options={kitIds.map(id => ({ value: id, label: `Kit #${id}` }))} style={{ minWidth: 140 }} />}</Space>}>
      {!kitIds.length ? <Typography.Text type="secondary">Chưa có thiết bị để điều khiển.</Typography.Text> : !admin ? <Alert type="info" showIcon message="Chỉ ADMIN có thể gửi lệnh điều khiển từ màn hình này." description="API hiện tại chỉ trả mã kit theo tài khoản, chưa gắn từng kit với nhà đang xem để gửi lệnh an toàn." /> : <Row gutter={[16, 16]}>
        {[["pump", "Máy bơm", <ThunderboltOutlined />], ["buzz", "Còi báo động", <AlertOutlined />], ["led", "Đèn cảnh báo", <BulbOutlined />]].map(([key, label, icon]) => <Col xs={24} md={8} key={key}><Card size="small" title={<Space>{icon}{label}</Space>}><Flex justify="space-between" align="center"><span>{deviceState[key] ? 'Đang bật' : 'Đang tắt'}</span><Switch checked={deviceState[key]} loading={commandBusy === key} disabled={!!commandBusy} onChange={value => command(key, value)} /></Flex></Card></Col>)}
      </Row>}
    </Card>
    <Card title="Lịch sử cảm biến và cảnh báo" style={{ marginTop: 24 }}>
      <Typography.Paragraph type="secondary">Lịch sử được giữ trong phiên trình duyệt hiện tại; backend chưa cung cấp API lưu và đọc lịch sử.</Typography.Paragraph>
      <Tabs items={[
        { key: 'readings', label: `Cảm biến (${readings.length})`, children: <Table rowKey="receivedAt" size="small" scroll={{ x: 'max-content' }} dataSource={readings} locale={{ emptyText: <Empty description="Chưa nhận được dữ liệu cảm biến" /> }} pagination={{ pageSize: 8, showSizeChanger: false }} columns={[
          { title: 'Thời gian nhận', dataIndex: 'receivedAt', render: value => new Date(value).toLocaleString('vi-VN') },
          { title: 'Vị trí', dataIndex: 'location', render: value => value || '—' },
          { title: 'Nhiệt độ', dataIndex: 'temperature', render: value => `${value} °C` },
          { title: 'Độ ẩm', dataIndex: 'humidity', render: value => `${value}%` },
          { title: 'Gas', dataIndex: 'gas', render: value => `${value} ppm` },
          { title: 'Trạng thái', dataIndex: 'status', render: value => <Tag color={['FIRE', 'EXTREME_FIRE'].includes(value) ? 'red' : value === 'DANGER' ? 'orange' : value === 'UNNORMAL' ? 'gold' : 'green'}>{value}</Tag> },
        ]} /> },
        { key: 'alerts', label: `Cảnh báo (${alerts.length})`, children: <Table rowKey="id" size="small" scroll={{ x: 'max-content' }} dataSource={alerts} locale={{ emptyText: <Empty description="Chưa ghi nhận cảnh báo cháy trong phiên này" /> }} pagination={{ pageSize: 8, showSizeChanger: false }} columns={[
          { title: 'Thời gian', dataIndex: 'receivedAt', render: value => new Date(value).toLocaleString('vi-VN') },
          { title: 'Vị trí', dataIndex: 'location', render: value => value || '—' },
          { title: 'Nhiệt độ', dataIndex: 'temperature', render: value => `${value} °C` },
          { title: 'Gas', dataIndex: 'gas', render: value => `${value} ppm` },
          { title: 'Kết quả', dataIndex: 'status', render: value => <Tag color="red">{value}</Tag> },
          { title: 'Độ tin cậy', dataIndex: 'confidence', render: value => value == null ? '—' : `${(Number(value) * 100).toFixed(1)}%` },
        ]} /> },
      ]} />
    </Card>
    <HouseMembers />
    <Modal title="Thêm kit cho nhà" open={kitModal} onCancel={() => setKitModal(false)} onOk={() => form.submit()} okText="Thêm kit">
      <Form form={form} layout="vertical" onFinish={addKit}>
        <Form.Item name="location" label="Vị trí lắp đặt" rules={[{ required: true, whitespace: true, message: 'Nhập vị trí lắp đặt' }, { max: 100 }]}><Input placeholder="Ví dụ: Phòng khách" /></Form.Item>
        <Form.Item name="deviceUid" label="Mã thiết bị (Device UID)" rules={[{ required: true, whitespace: true, message: 'Nhập mã thiết bị' }, { max: 100 }]}><Input placeholder="Mã duy nhất của kit" /></Form.Item>
      </Form>
    </Modal>
  </>;
}
