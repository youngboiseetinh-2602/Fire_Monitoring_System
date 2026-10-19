import { useState } from 'react';
import { Alert, Button, Card, Divider, Form, Input, Typography } from 'antd';
import { ArrowRightOutlined, LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { ApiError } from '../../components/common/Feedback';
import { applyFieldErrors, maxLength, required } from '../../components/common/formRules';

export default function LoginPage() {
  const [form] = Form.useForm();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);
  const { login, expired } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  async function submit(values) {
    setPending(true); setError(null);
    try {
      await login(values);
      const from = location.state?.from;
      navigate(typeof from === 'string' && from.startsWith('/app/') ? from : '/app/houses', { replace: true });
    } catch (failure) { setError(failure); applyFieldErrors(form, failure); }
    finally { setPending(false); }
  }

  return <Card className="auth-card">
    <div className="section-icon"><SafetyCertificateOutlined /></div>
    <span className="eyebrow">FIRE MONITORING SYSTEM</span>
    <Typography.Title level={2}>Chào mừng trở lại</Typography.Title>
    <Typography.Paragraph type="secondary">Đăng nhập để quản lý nhà ở và thông tin của bạn.</Typography.Paragraph>
    {expired && <Alert showIcon type="warning" message="Phiên đã hết hạn. Vui lòng đăng nhập lại." className="api-error" />}
    {location.state?.registered && <Alert showIcon type="success" message="Đăng ký thành công. Bạn có thể đăng nhập ngay." className="api-error" />}
    <ApiError error={error} />
    <Form form={form} layout="vertical" onFinish={submit} disabled={pending} requiredMark="optional">
      <Form.Item name="username" label="Tên đăng nhập" rules={[required('Tên đăng nhập'), maxLength(50)]}>
        <Input prefix={<UserOutlined />} autoComplete="username" maxLength={50} placeholder="Nhập tên đăng nhập" />
      </Form.Item>
      <Form.Item name="password" label="Mật khẩu" rules={[required('Mật khẩu')]}>
        <Input.Password prefix={<LockOutlined />} autoComplete="current-password" placeholder="Nhập mật khẩu" />
      </Form.Item>
      <Button block type="primary" htmlType="submit" loading={pending} aria-label="Đăng nhập" icon={<ArrowRightOutlined />}>Đăng nhập</Button>
    </Form>
    <Divider plain>Bạn mới tham gia?</Divider>
    <Link to="/register" aria-disabled={pending} onClick={event => pending && event.preventDefault()}>
      <Button block disabled={pending} icon={<UserOutlined />}>Tạo tài khoản mới</Button>
    </Link>
  </Card>;
}
