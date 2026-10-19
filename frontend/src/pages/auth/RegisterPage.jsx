import { useState } from 'react';
import { App, Button, Card, Col, Divider, Form, Input, Row, Typography } from 'antd';
import { TeamOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import { ApiError } from '../../components/common/Feedback';
import { applyFieldErrors, cccdRule, maxLength, phoneRule, required } from '../../components/common/formRules';

export default function RegisterPage() {
  const [form] = Form.useForm();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);
  const { message } = App.useApp();
  const navigate = useNavigate();

  async function submit(values) {
    setPending(true); setError(null);
    try {
      await authApi.register(values);
      message.success('Tạo tài khoản thành công.');
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (failure) { setError(failure); applyFieldErrors(form, failure); }
    finally { setPending(false); }
  }

  return <Card className="auth-card">
    <div className="section-icon"><TeamOutlined /></div>
    <Typography.Title level={2}>Tạo tài khoản</Typography.Title>
    <Typography.Paragraph type="secondary">Bắt đầu hành trình cùng cộng đồng của bạn.</Typography.Paragraph>
    <ApiError error={error} />
    <Form form={form} layout="vertical" onFinish={submit} disabled={pending} requiredMark="optional">
      <Row gutter={16}>
        <Col xs={24}><Form.Item name="fullName" label="Họ và tên" rules={[required('Họ và tên'), maxLength(100)]}>
          <Input autoComplete="name" maxLength={100} />
        </Form.Item></Col>
        <Col xs={24} md={12}><Form.Item name="username" label="Tên đăng nhập" rules={[required('Tên đăng nhập'), maxLength(50)]}>
          <Input autoComplete="username" maxLength={50} />
        </Form.Item></Col>
        <Col xs={24} md={12}><Form.Item name="phone" label="Số điện thoại" rules={[phoneRule]}>
          <Input type="tel" autoComplete="tel" maxLength={10} placeholder="0912345678" />
        </Form.Item></Col>
        <Col xs={24}><Form.Item name="cccd" label="CCCD" rules={[required('CCCD'), cccdRule]}>
          <Input inputMode="numeric" maxLength={12} placeholder="12 chữ số" />
        </Form.Item></Col>
        <Col xs={24} md={12}><Form.Item name="password" label="Mật khẩu" rules={[required('Mật khẩu'), { min: 8, message: 'Mật khẩu cần ít nhất 8 ký tự.' }, maxLength(100)]}>
          <Input.Password autoComplete="new-password" maxLength={100} />
        </Form.Item></Col>
        <Col xs={24} md={12}><Form.Item name="confirmPassword" label="Xác nhận mật khẩu" dependencies={['password']}
          rules={[required('Xác nhận mật khẩu'), ({ getFieldValue }) => ({ validator(_, value) {
            return !value || getFieldValue('password') === value ? Promise.resolve() : Promise.reject(new Error('Mật khẩu xác nhận chưa khớp.'));
          } })]}>
          <Input.Password autoComplete="new-password" maxLength={100} />
        </Form.Item></Col>
      </Row>
      <Typography.Paragraph type="secondary" className="form-hint">Tài khoản mới có vai trò thành viên.</Typography.Paragraph>
      <Button block type="primary" htmlType="submit" loading={pending}>Tạo tài khoản</Button>
    </Form>
    <Divider plain>Đã có tài khoản?</Divider>
    <Link to="/login" aria-disabled={pending} onClick={event => pending && event.preventDefault()}><Button block disabled={pending}>Quay lại đăng nhập</Button></Link>
  </Card>;
}
