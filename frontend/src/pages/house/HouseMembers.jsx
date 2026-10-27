import { useState } from 'react';
import { Button, Card, Flex, Form, InputNumber, Popconfirm, Typography, message } from 'antd';
import { UserAddOutlined, UserDeleteOutlined } from '@ant-design/icons';
import { houseMemberApi } from '../../api/houseMemberApi';
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
      await action(values.userId);
      message.success('Đã cập nhật thành viên trong nhà.');
      form.resetFields();
    } catch (error) { message.error(error.message || 'Không thể cập nhật thành viên.'); }
    finally { setBusy(false); }
  }

  return <Card title="Quản lý thành viên" style={{ marginTop: 24 }}>
    <Typography.Paragraph type="secondary">Nhập mã người dùng để thêm hoặc gỡ thành viên. API hiện chỉ quản lý ngôi nhà duy nhất gắn với tài khoản chủ nhà và chưa hỗ trợ đọc danh sách thành viên.</Typography.Paragraph>
    <Flex gap={24} wrap>
      <Form form={addForm} layout="inline" onFinish={values => submit(houseMemberApi.addMember, values, setAdding, addForm)}>
        <Form.Item name="userId" rules={[{ required: true, message: 'Nhập mã người dùng' }]}><InputNumber min={1} placeholder="Mã người dùng" aria-label="Mã người dùng cần thêm" /></Form.Item>
        <Button htmlType="submit" type="primary" icon={<UserAddOutlined />} loading={adding}>Thêm thành viên</Button>
      </Form>
      <Form form={removeForm} layout="inline" onFinish={() => {}}>
        <Form.Item name="userId" rules={[{ required: true, message: 'Nhập mã người dùng' }]}><InputNumber min={1} placeholder="Mã người dùng" aria-label="Mã người dùng cần gỡ" /></Form.Item>
        <Popconfirm title="Gỡ người dùng này khỏi nhà?" description="Thao tác này có hiệu lực ngay sau khi xác nhận." okText="Gỡ thành viên" cancelText="Hủy" onConfirm={() => removeForm.validateFields().then(values => submit(houseMemberApi.removeMember, values, setRemoving, removeForm))}>
          <Button htmlType="button" danger icon={<UserDeleteOutlined />} loading={removing}>Gỡ thành viên</Button>
        </Popconfirm>
      </Form>
    </Flex>
  </Card>;
}
