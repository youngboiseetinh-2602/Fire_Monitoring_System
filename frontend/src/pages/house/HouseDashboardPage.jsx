import { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { Alert, Badge, Button, Card, Col, Empty, Flex, Form, Input, Modal, Row, Select, Space, Statistic, Switch, Table, Tabs, Tag, Typography, message } from 'antd';
import { AlertOutlined, BulbOutlined, CloudOutlined, DashboardOutlined, FireOutlined, PlusOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { kitApi } from '../../api/kitApi';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import { useTelemetry } from '../../realtime/TelemetryContext';

const initial = { kitId: null, deviceUid: '', temperature: null, humidity: null, gas: null, flame: false, status: 'NORMAL', confidence: null, location: '' };
const historyLimit = 100;

export default function HouseDashboardPage() {
  const { houseId } = useParams();
  const location = useLocation();
  const { user } = useAuth();
  const { connected, latestByHouse } = useTelemetry();
  const lastEvent = latestByHouse[Number(houseId)];
  const houseName = location.state?.houseName || `Nhà #${houseId}`;
  const admin = user?.role === 'ADMIN';
  const owner = user?.role === 'OWNER';
  const canAddKit = admin || owner;
  const [kitIds, setKitIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [kitError, setKitError] = useState(null);
  const [kitRetry, setKitRetry] = useState(0);
  const [sensor, setSensor] = useState(initial);
  const [hasTelemetry, setHasTelemetry] = useState(false);
  const [readings, setReadings] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [kitModal, setKitModal] = useState(false);
  const [commandBusy, setCommandBusy] = useState('');
  const [deviceState, setDeviceState] = useState({ pump: false, buzz: false, led: false });
  const [selectedKitId, setSelectedKitId] = useState(null);
  const [form] = Form.useForm();

  useEffect(() => {
    setLoading(true);
    setKitIds([]);
    setSelectedKitId(null);
    setSensor(initial);
    setHasTelemetry(false);
    setReadings([]);
    setAlerts([]);
    setDeviceState({ pump: false, buzz: false, led: false });
  }, [houseId]);

  useEffect(() => {
    let active = true;
    setKitError(null);
    (async () => {
      try {
        const kits = await kitApi.getByHouse(houseId);
        const ids = Array.isArray(kits) ? kits.map(kit => kit.id) : [];
        if (active) {
          setKitIds(ids);
          setSelectedKitId(ids[0] ?? null);
        }
        kitApi.startHouseTelemetry(houseId).catch(() => {});
      } catch (error) {
        // Dashboard access is separate from device-management permission.
        // A background telemetry 403 must not turn the whole page into an access error.
        if (active && ![403, 404].includes(error.status)) setKitError(error);
        if (active) setKitIds([]);
      } finally { if (active) setLoading(false); }
    })();
    return () => { active = false; };
  }, [houseId, admin, kitRetry]);

  useEffect(() => {
    if (!lastEvent) return;
    const reading = { ...initial, ...lastEvent.data, eventId: lastEvent.sequence };
    const isFire = reading.fireConfirmed === true;
    setHasTelemetry(true);
    setSensor(reading);
    setReadings(current => [reading, ...current].slice(0, historyLimit));
    if (isFire) {
      setAlerts(current => [{ ...reading, id: lastEvent.sequence }, ...current].slice(0, historyLimit));
    }
  }, [houseId, lastEvent]);

  const fire = sensor.fireConfirmed === true;
  const gasWarning = sensor.gasWarning === true && !fire;
  const statusColor = fire ? 'red' : gasWarning ? 'orange' : 'green';
  const statusLabel = fire ? 'PHÁT HIỆN CHÁY' : gasWarning ? 'CẢNH BÁO KHÍ GAS' : 'BÌNH THƯỜNG';

  async function addKit(values) {
    try {
      await kitApi.addToHouse(houseId, values);
      setKitModal(false); form.resetFields();
      message.success('Đã thêm kit thành công.');
      const kits = await kitApi.getByHouse(houseId);
      const ids = Array.isArray(kits) ? kits.map(kit => kit.id) : [];
      setKitIds(ids);
      setSelectedKitId(ids[0] ?? null);
      kitApi.startHouseTelemetry(houseId).catch(() => {});
    } catch (error) { message.error(error.message || 'Không thêm được kit.'); }
  }

  async function command(field, enabled) {
    const kitId = selectedKitId;
    if (admin || !kitId) return;
    setCommandBusy(field);
    const states = { ...deviceState, [field]: enabled };
    try { await kitApi.sendCommand(kitId, states); setDeviceState(states); message.success('Đã gửi lệnh đến kit.'); }
    catch (error) { message.error(error.message || 'Gửi lệnh thất bại.'); }
    finally { setCommandBusy(''); }
  }

  return <>
    <PageHeader title={`Giám sát: ${houseName}`} description={<Space><Badge status={connected ? 'success' : 'default'} text={connected ? 'Đang nhận dữ liệu realtime' : 'Chưa kết nối realtime'} />{kitIds.length > 0 && <Tag color="blue">{kitIds.length} thiết bị</Tag>}</Space>} />
    {fire && <Alert type="error" showIcon icon={<FireOutlined />} message="CẢNH BÁO CHÁY" description="Thiết bị phát hiện dấu hiệu cháy. Hãy kiểm tra khu vực ngay." banner style={{ marginBottom: 20 }} />}
    {gasWarning && (
      <Alert
        type="warning"
        showIcon
        message="CẢNH BÁO KHÍ GAS"
        description={`Giá trị MQ-2: ${sensor.gas} ADC. Vui lòng kiểm tra khu vực.`}
        banner
        style={{ marginBottom: 20 }}
      />
    )}
    {kitError && <Alert type="error" showIcon message={kitError.message || 'Không tải được danh sách thiết bị.'} action={<Button size="small" onClick={() => setKitRetry(value => value + 1)}>Thử lại</Button>} style={{ marginBottom: 16 }} />}
    {!loading && kitIds.length === 0 && <Alert type="info" showIcon message="Nhà chưa có kit được liên kết." description={canAddKit ? 'Thêm kit để bắt đầu nhận dữ liệu cảm biến.' : 'Chưa tìm thấy thiết bị trong các nhà bạn tham gia.'} action={canAddKit && <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => setKitModal(true)}>Thêm kit</Button>} style={{ marginBottom: 20 }} />}
    <Flex justify="space-between" align="center" wrap gap="small"><Typography.Title level={4}>Thông số môi trường</Typography.Title>{canAddKit && <Button icon={<PlusOutlined />} onClick={() => setKitModal(true)}>Thêm kit</Button>}</Flex>
    {!hasTelemetry && kitIds.length > 0 && <Alert type="info" showIcon message="Đang chờ dữ liệu cảm biến đầu tiên" description="Các chỉ số sẽ xuất hiện khi kit gửi telemetry và kết nối realtime hoạt động." style={{ marginBottom: 16 }} />}
    <Row gutter={[16, 16]}>
      <Col xs={12} lg={6}><Card><Statistic title="Nhiệt độ" value={sensor.temperature ?? '—'} suffix={sensor.temperature == null ? '' : '°C'} prefix={<DashboardOutlined />} /></Card></Col>
      <Col xs={12} lg={6}><Card><Statistic title="Độ ẩm" value={sensor.humidity ?? '—'} suffix={sensor.humidity == null ? '' : '%'} prefix={<CloudOutlined />} /></Card></Col>
      <Col xs={12} lg={6}><Card><Statistic title="Nồng độ gas" value={sensor.gas ?? '—'} suffix={sensor.gas == null ? '' : 'ppm'} prefix={<AlertOutlined />} /></Card></Col>
      <Col xs={12} lg={6}><Card><Statistic title="Trạng thái" value={hasTelemetry ? statusLabel : 'Chưa có dữ liệu'} valueStyle={{ color: hasTelemetry ? statusColor : undefined, fontSize: 20 }} prefix={<FireOutlined />} /></Card></Col>
    </Row>
    <Card title="Điều khiển thiết bị" style={{ marginTop: 24 }} extra={<Space>{sensor.location && <Tag>{sensor.location}</Tag>}{!admin && kitIds.length > 1 && <Select aria-label="Chọn kit điều khiển" value={selectedKitId} onChange={setSelectedKitId} options={kitIds.map(id => ({ value: id, label: `Kit #${id}` }))} style={{ minWidth: 140 }} />}</Space>}>
      {!kitIds.length ? <Typography.Text type="secondary">Chưa có thiết bị để điều khiển.</Typography.Text> : admin ? <Alert type="info" showIcon message="ADMIN chỉ giám sát hệ thống." description="Chỉ OWNER hoặc MEMBER thuộc ngôi nhà mới có thể điều khiển thiết bị." /> : <Row gutter={[16, 16]}>
        {[["pump", "Máy bơm", <ThunderboltOutlined />], ["buzz", "Còi báo động", <AlertOutlined />], ["led", "Đèn cảnh báo", <BulbOutlined />]].map(([key, label, icon]) => <Col xs={24} md={8} key={key}><Card size="small" title={<Space>{icon}{label}</Space>}><Flex justify="space-between" align="center"><span>{deviceState[key] ? 'Đang bật' : 'Đang tắt'}</span><Switch checked={deviceState[key]} loading={commandBusy === key} disabled={!!commandBusy} onChange={value => command(key, value)} /></Flex></Card></Col>)}
      </Row>}
    </Card>
    <Card title="Lịch sử cảm biến và cảnh báo" style={{ marginTop: 24 }}>
      <Tabs items={[
        { key: 'readings', label: `Cảm biến (${readings.length})`, children: <Table rowKey="eventId" size="small" scroll={{ x: 'max-content' }} dataSource={readings} locale={{ emptyText: <Empty description="Chưa nhận được dữ liệu cảm biến" /> }} pagination={{ pageSize: 8, showSizeChanger: false }} columns={[
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
          { title: 'Cảm biến lửa', dataIndex: 'flame', render: value => <Tag color={value ? 'orange' : 'default'}>{value ? 'Phát hiện' : 'Không'}</Tag> },
          { title: 'Dự đoán AI', dataIndex: 'status', render: value => <Tag color={['FIRE', 'EXTREME_FIRE'].includes(value) ? 'red' : value === 'DANGER' ? 'orange' : value === 'UNNORMAL' ? 'gold' : 'green'}>{value}</Tag> },
        ]} /> },
      ]} />
    </Card>
    <Modal title="Thêm kit cho nhà" open={kitModal} onCancel={() => setKitModal(false)} onOk={() => form.submit()} okText="Thêm kit">
      <Form form={form} layout="vertical" onFinish={addKit}>
        <Form.Item name="location" label="Vị trí lắp đặt" rules={[{ required: true, whitespace: true, message: 'Nhập vị trí lắp đặt' }, { max: 100 }]}><Input placeholder="Ví dụ: Phòng khách" /></Form.Item>
        <Form.Item name="deviceUid" label="Mã thiết bị (Device UID)" rules={[{ required: true, whitespace: true, message: 'Nhập mã thiết bị' }, { max: 100 }]}><Input placeholder="Mã duy nhất của kit" /></Form.Item>
      </Form>
    </Modal>
  </>;
}
