import { useState } from 'react';
import { Alert, App, Button, Card, Col, Flex, Form, Input, Row, Select, Typography } from 'antd';
import { HomeOutlined, SaveOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { houseApi } from '../../api/houseApi';
import { ApiError } from '../../components/common/Feedback';
import { applyFieldErrors, maxLength, required } from '../../components/common/formRules';

export default function HouseForm({ house, returnTo = '/app/houses' }) {
  const editing = Boolean(house);
  const [form] = Form.useForm();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);
  const { notification, modal } = App.useApp();
  const navigate = useNavigate();

  async function save(values) {
    setPending(true); setError(null);
    try {
      if (editing) await houseApi.update(house.id, values);
      else await houseApi.create(values);
      notification.success({ message: editing ? 'Đã cập nhật ngôi nhà' : 'Đã tạo ngôi nhà mới' });
      navigate(returnTo, { replace: true });
    } catch (failure) { setError(failure); applyFieldErrors(form, failure); }
    finally { setPending(false); }
  }

  function submit(values) {
    if (editing && values.status !== house.status) {
      modal.confirm({ title: 'Xác nhận thay đổi trạng thái', okText: 'Xác nhận', cancelText: 'Hủy', centered: true,
        content: `Đổi trạng thái ngôi nhà sang “${values.status === 'FIRE' ? 'Cháy' : 'Bình thường'}”?`, onOk: () => save(values) });
    } else save(values);
  }

  function cancel() {
    if (form.isFieldsTouched()) modal.confirm({ title: 'Bỏ thay đổi chưa lưu?', content: 'Thông tin vừa nhập sẽ không được lưu.',
      okText: 'Xác nhận', cancelText: 'Hủy', centered: true, onOk: () => navigate(returnTo) });
    else navigate(returnTo);
  }

  return <Row gutter={[24, 24]}>
    <Col xs={24} lg={16}>
      <Card title="Thông tin ngôi nhà" className="content-card">
        <ApiError error={error} />
        <Form form={form} layout="vertical" initialValues={house} onFinish={submit} disabled={pending}>
          <Form.Item name="houseName" label="Tên ngôi nhà" rules={[required('Tên ngôi nhà'), maxLength(100)]}>
            <Input prefix={<HomeOutlined />} maxLength={100} placeholder="Ví dụ: Nhà An Bình" />
          </Form.Item>
          <Row gutter={20}>
            <Col xs={24} md={12}><Form.Item name="city" label="Thành phố / Tỉnh" rules={[required('Thành phố / Tỉnh'), maxLength(100)]}><Input maxLength={100} /></Form.Item></Col>
            <Col xs={24} md={12}><Form.Item name="ward" label="Phường / Xã" rules={[required('Phường / Xã'), maxLength(100)]}><Input maxLength={100} /></Form.Item></Col>
          </Row>
          <Form.Item name="addressDetail" label="Địa chỉ chi tiết" rules={[required('Địa chỉ chi tiết'), maxLength(255)]}>
            <Input maxLength={255} placeholder="Số nhà, tên đường…" />
          </Form.Item>
          {editing ? <Form.Item name="status" label="Trạng thái" rules={[{ required: true, message: 'Vui lòng chọn trạng thái.' }]}>
            <Select options={[{ value: 'NORMAL', label: 'Bình thường (NORMAL)' }, { value: 'FIRE', label: 'Cháy (FIRE)' }]} />
          </Form.Item> : <Alert className="api-error" type="info" showIcon message="Ngôi nhà mới có trạng thái mặc định là Bình thường." />}
          <Flex justify="end" gap={12} className="form-actions">
            <Button onClick={cancel}>Hủy</Button>
            <Button type="primary" htmlType="submit" loading={pending} icon={<SaveOutlined />}>{editing ? 'Lưu thay đổi' : 'Tạo ngôi nhà'}</Button>
          </Flex>
        </Form>
      </Card>
    </Col>
    <Col xs={24} lg={8} className="illustration-column">
      <Card className="illustration-card" cover={<div className="landscape-card" />}>
        <div className="section-icon"><HomeOutlined /></div>
        <Typography.Title level={3}>Một mái nhà.<br />Một sự an tâm.</Typography.Title>
        <Typography.Paragraph type="secondary">{editing ? `Bạn đang cập nhật nhà #${house.id}. Kiểm tra thông tin địa chỉ và trạng thái trước khi lưu.` : 'Thêm thông tin ngôi nhà để dễ dàng quản lý và theo dõi trạng thái trong hệ thống.'}</Typography.Paragraph>
      </Card>
    </Col>
  </Row>;
}
