import { useState } from 'react';
import { Button, Card, Flex, Form, InputNumber, Space, Typography, message } from 'antd';
import { UserAddOutlined, UserDeleteOutlined } from '@ant-design/icons';
import { houseApi } from '../../api/houseApi';
import { useAuth } from '../../auth/AuthContext';

export default function HouseMembers() {
  const { user } = useAuth();
  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState(false);
  const [addForm] = Form.useForm();
  const [removeForm] = Form.useForm();
  if (user?.role !== 'OWNER') return null;

  async function submit(action, values, setBusy, form) {
    setBusy(true);
    try {
      await action({ userId: values.userId });
      message.success('Đã cập nhật thành viên trong nhà.');
      form.resetFields();
    } catch (error) { message.error(error.message || 'Không thể cập nhật thành viên.'); }
    finally { setBusy(false); }
  }

  return <Card title="Quản lý thành viên" style={{ marginTop: 24 }}>
    <Typography.Paragraph type="secondary">Backend hiện nhận mã người dùng để thêm hoặc gỡ thành viên; danh sách thành viên chưa có API đọc.</Typography.Paragraph>
    <Flex gap={24} wrap>
      <Form form={addForm} layout="inline" onFinish={values => submit(houseApi.addPeople, values, setAdding, addForm)}>
        <Form.Item name="userId" rules={[{ required: true, message: 'Nhập mã người dùng' }]}><InputNumber min={1} placeholder="Mã người dùng" aria-label="Mã người dùng cần thêm" /></Form.Item>
        <Button htmlType="submit" type="primary" icon={<UserAddOutlined />} loading={adding}>Thêm thành viên</Button>
      </Form>
      <Form form={removeForm} layout="inline" onFinish={values => submit(houseApi.removePeople, values, setRemoving, removeForm)}>
        <Form.Item name="userId" rules={[{ required: true, message: 'Nhập mã người dùng' }]}><InputNumber min={1} placeholder="Mã người dùng" aria-label="Mã người dùng cần gỡ" /></Form.Item>
        <Button htmlType="submit" danger icon={<UserDeleteOutlined />} loading={removing}>Gỡ thành viên</Button>
      </Form>
    </Flex>
  </Card>;
}
