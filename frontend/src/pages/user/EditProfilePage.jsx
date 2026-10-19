import { useEffect, useState } from 'react';
import { App, Button, Card, Col, Flex, Form, Input, Row } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { userApi } from '../../api/userApi';
import { ApiError, Loading } from '../../components/common/Feedback';
import PageHeader from '../../components/common/PageHeader';
import { applyFieldErrors, cccdRule, maxLength, phoneRule, required } from '../../components/common/formRules';

function ProfileForm({ user }) {
  const [form] = Form.useForm();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);
  const { notification, modal } = App.useApp();
  const { updateUser } = useAuth();
  const navigate = useNavigate();

  async function submit(values) {
    setPending(true); setError(null);
    try {
      await userApi.updateMe(values);
      updateUser({ ...user, ...values, phone: values.phone || null });
      notification.success({ message: 'Đã cập nhật hồ sơ', description: 'Thông tin cá nhân của bạn đã được lưu.' });
      navigate('/app/profile', { replace: true });
    } catch (failure) { setError(failure); applyFieldErrors(form, failure); }
    finally { setPending(false); }
  }

  function cancel() {
    if (form.isFieldsTouched()) modal.confirm({ title: 'Bỏ thay đổi chưa lưu?', content: 'Thông tin vừa chỉnh sửa sẽ không được lưu.',
      okText: 'Xác nhận', cancelText: 'Hủy', centered: true, onOk: () => navigate('/app/profile') });
    else navigate('/app/profile');
  }

  return <Card title="Thông tin cá nhân" className="content-card">
    <ApiError error={error} />
    <Form form={form} layout="vertical" initialValues={user} onFinish={submit} disabled={pending} requiredMark="optional">
      <Row gutter={24}>
        <Col xs={24} md={12}><Form.Item name="fullName" label="Họ và tên" rules={[required('Họ và tên'), maxLength(100)]}>
          <Input autoComplete="name" maxLength={100} />
        </Form.Item></Col>
        <Col xs={24} md={12}><Form.Item name="phone" label="Số điện thoại" rules={[phoneRule]}>
          <Input type="tel" autoComplete="tel" maxLength={10} />
        </Form.Item></Col>
        <Col xs={24} md={12}><Form.Item name="cccd" label="CCCD" rules={[required('CCCD'), cccdRule]}>
          <Input inputMode="numeric" maxLength={12} />
        </Form.Item></Col>
      </Row>
      <Flex justify="end" gap={12} className="form-actions"><Button onClick={cancel}>Hủy</Button><Button type="primary" htmlType="submit" loading={pending} icon={<SaveOutlined />}>Lưu thay đổi</Button></Flex>
    </Form>
  </Card>;
}

export default function EditProfilePage() {
  const [query, setQuery] = useState({ loading: true });
  const [retryCount, setRetryCount] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setQuery({ loading: true });
    async function loadProfile() {
      try {
        const data = await userApi.getMe({ signal: controller.signal });
        if (!controller.signal.aborted) setQuery({ data, loading: false });
      } catch (error) {
        if (!controller.signal.aborted) setQuery({ error, loading: false });
      }
    }
    loadProfile();
    return () => controller.abort();
  }, [retryCount]);
  return <>
    <PageHeader title="Chỉnh sửa hồ sơ" description="Cập nhật thông tin cá nhân và số điện thoại liên hệ."
      crumbs={[{ title: 'Hồ sơ cá nhân', to: '/app/profile' }, { title: 'Chỉnh sửa' }]} />
    {query.loading ? <Loading /> : query.error ? <ApiError error={query.error} retry={() => setRetryCount(count => count + 1)} /> : <ProfileForm user={query.data} />}
  </>;
}
