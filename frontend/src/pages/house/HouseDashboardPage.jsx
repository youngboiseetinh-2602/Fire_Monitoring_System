import { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { Alert, Badge, Button, Card, Col, Flex, Form, Input, Modal, Row, Space, Statistic, Switch, Tag, Typography, message } from 'antd';
import { AlertOutlined, BulbOutlined, CloudOutlined, DashboardOutlined, FireOutlined, PlusOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { kitApi } from '../../api/kitApi';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import HouseMembers from './HouseMembers';

const initial = { temperature: null, humidity: null, gas: null, flame: false, status: 'NORMAL', confidence: null, location: '' };
const wsBase = import.meta.env.VITE_API_BASE_URL || window.location.origin;

function connectStomp(houseId, token, onData, onStatus) {
  const url = wsBase.replace(/^http/, 'ws').replace(/\/$/, '') + '/ws';
  const socket = new WebSocket(url);
  let heartbeat;
  let buffer = '';
  const send = frame => socket.send(`${frame}\0`);
  socket.onopen = () => send(`CONNECT\naccept-version:1.2\nheart-beat:0,10000\nAuthorization:Bearer ${token}\n\n`);
  socket.onmessage = event => {
    buffer += event.data;
    const frames = buffer.split('\0');
    buffer = frames.pop() || '';
    frames.forEach(frame => {
      const separator = frame.indexOf('\n\n');
      const command = (separator < 0 ? frame : frame.slice(0, separator)).split('\n')[0];
      if (command === 'CONNECTED') {
        onStatus(true);
        send(`SUBSCRIBE\nid:house-${houseId}\ndestination:/topic/houses/${houseId}/data\nack:auto\n\n`);
        heartbeat = window.setInterval(() => { if (socket.readyState === WebSocket.OPEN) socket.send('\n'); }, 10000);
      } else if (command === 'MESSAGE' && separator >= 0) {
        try { onData(JSON.parse(frame.slice(separator + 2))); } catch { /* Ignore malformed frames. */ }
      } else if (command === 'ERROR') onStatus(false);
    });
  };
  socket.onclose = () => { window.clearInterval(heartbeat); onStatus(false); };
  socket.onerror = () => onStatus(false);
  return () => { window.clearInterval(heartbeat); if (socket.readyState === WebSocket.OPEN) send('DISCONNECT\n\n'); socket.close(); };
}

export default function HouseDashboardPage() {
  const { houseId } = useParams();
  const location = useLocation();
  const { user } = useAuth();
  const houseName = location.state?.houseName || `Nhà #${houseId}`;
  const admin = user?.role === 'ADMIN';
  const [kitIds, setKitIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [connected, setConnected] = useState(false);
  const [sensor, setSensor] = useState(initial);
  const [kitModal, setKitModal] = useState(false);
  const [commandBusy, setCommandBusy] = useState('');
  const [deviceState, setDeviceState] = useState({ pump: false, buzz: false, led: false });
  const [form] = Form.useForm();

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const ids = admin ? await kitApi.getHouseKits(houseId) : await kitApi.getMyHouseKits();
        if (active) setKitIds(Array.isArray(ids) ? ids : []);
      } catch (error) {
        if (active && error.status !== 404) message.error(error.message || 'Không tải được thiết bị của nhà.');
        if (active) setKitIds([]);
      } finally { if (active) setLoading(false); }
    })();
    return () => { active = false; };
  }, [houseId, admin]);

  useEffect(() => {
    if (!kitIds.length) return undefined;
    return connectStomp(houseId, user?.accessToken || user?.token || '', data => setSensor(previous => ({ ...previous, ...data })), setConnected);
  }, [houseId, kitIds.length, user]);

  const fire = sensor.flame || ['FIRE', 'EXTREME_FIRE'].includes(sensor.status);
  const statusColor = fire ? 'red' : sensor.status === 'DANGER' ? 'orange' : sensor.status === 'UNNORMAL' ? 'gold' : 'green';
  const statusLabel = fire ? 'PHÁT HIỆN CHÁY' : ({ NORMAL: 'An toàn', UNNORMAL: 'Bất thường', DANGER: 'Nguy hiểm' }[sensor.status] || sensor.status);

  async function addKit(values) {
    try {
      const kit = await kitApi.addToHouse(houseId, values);
      setKitIds(current => [...current, kit.id]);
      setKitModal(false); form.resetFields();
      message.success(`Đã thêm kit ${kit.deviceUid}.`);
      // Opening the admin kit endpoint starts its telemetry stream.
      const ids = await kitApi.getHouseKits(houseId); setKitIds(ids || []);
    } catch (error) { message.error(error.message || 'Không thêm được kit.'); }
  }

  async function command(field, enabled) {
    const kitId = kitIds[0];
    if (!kitId) return;
    setCommandBusy(field);
    const states = { ...deviceState, [field]: enabled };
    try { await kitApi.sendCommand(kitId, states); setDeviceState(states); message.success('Đã gửi lệnh đến kit.'); }
    catch (error) { message.error(error.message || 'Gửi lệnh thất bại.'); }
    finally { setCommandBusy(''); }
  }

  return <>
    <PageHeader title={`Giám sát: ${houseName}`} description={<Space><Badge status={connected ? 'success' : 'default'} text={connected ? 'Đang nhận dữ liệu realtime' : 'Chưa kết nối realtime'} />{kitIds.length > 0 && <Tag color="blue">{kitIds.length} thiết bị</Tag>}</Space>} />
    {fire && <Alert type="error" showIcon icon={<FireOutlined />} message="CẢNH BÁO CHÁY" description="Thiết bị phát hiện dấu hiệu cháy. Hãy kiểm tra khu vực ngay." banner style={{ marginBottom: 20 }} />}
    {!loading && kitIds.length === 0 && <Alert type="info" showIcon message="Nhà chưa có kit được liên kết." description={admin ? 'Thêm kit để bắt đầu nhận dữ liệu cảm biến.' : 'Chưa tìm thấy thiết bị trong các nhà bạn tham gia.'} action={admin && <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => setKitModal(true)}>Thêm kit</Button>} style={{ marginBottom: 20 }} />}
    <Flex justify="space-between" align="center" wrap gap="small"><Typography.Title level={4}>Thông số môi trường</Typography.Title>{admin && <Button icon={<PlusOutlined />} onClick={() => setKitModal(true)}>Thêm kit</Button>}</Flex>
    <Row gutter={[16, 16]}>
      <Col xs={12} lg={6}><Card><Statistic title="Nhiệt độ" value={sensor.temperature ?? '—'} suffix={sensor.temperature == null ? '' : '°C'} prefix={<DashboardOutlined />} /></Card></Col>
      <Col xs={12} lg={6}><Card><Statistic title="Độ ẩm" value={sensor.humidity ?? '—'} suffix={sensor.humidity == null ? '' : '%'} prefix={<CloudOutlined />} /></Card></Col>
      <Col xs={12} lg={6}><Card><Statistic title="Nồng độ gas" value={sensor.gas ?? '—'} suffix={sensor.gas == null ? '' : 'ppm'} prefix={<AlertOutlined />} /></Card></Col>
      <Col xs={12} lg={6}><Card><Statistic title="Trạng thái" value={statusLabel} valueStyle={{ color: statusColor, fontSize: 20 }} prefix={<FireOutlined />} /></Card></Col>
    </Row>
    <Card title="Điều khiển thiết bị" style={{ marginTop: 24 }} extra={sensor.location && <Tag>{sensor.location}</Tag>}>
      {!kitIds.length ? <Typography.Text type="secondary">Chưa có thiết bị để điều khiển.</Typography.Text> : <Row gutter={[16, 16]}>
        {[["pump", "Máy bơm", <ThunderboltOutlined />], ["buzz", "Còi báo động", <AlertOutlined />], ["led", "Đèn cảnh báo", <BulbOutlined />]].map(([key, label, icon]) => <Col xs={24} md={8} key={key}><Card size="small" title={<Space>{icon}{label}</Space>}><Flex justify="space-between" align="center"><span>{deviceState[key] ? 'Đang bật' : 'Đang tắt'}</span><Switch checked={deviceState[key]} loading={commandBusy === key} disabled={!!commandBusy} onChange={value => command(key, value)} /></Flex></Card></Col>)}
      </Row>}
    </Card>
    <HouseMembers houseId={houseId} />
    <Modal title="Thêm kit cho nhà" open={kitModal} onCancel={() => setKitModal(false)} onOk={() => form.submit()} okText="Thêm kit">
      <Form form={form} layout="vertical" onFinish={addKit}>
        <Form.Item name="location" label="Vị trí lắp đặt" rules={[{ required: true, whitespace: true, message: 'Nhập vị trí lắp đặt' }, { max: 100 }]}><Input placeholder="Ví dụ: Phòng khách" /></Form.Item>
        <Form.Item name="deviceUid" label="Mã thiết bị (Device UID)" rules={[{ required: true, whitespace: true, message: 'Nhập mã thiết bị' }, { max: 100 }]}><Input placeholder="Mã duy nhất của kit" /></Form.Item>
      </Form>
    </Modal>
  </>;
}
