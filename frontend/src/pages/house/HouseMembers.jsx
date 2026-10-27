import { useEffect, useState } from 'react';
import { Alert, Button, Card, Empty, Flex, Form, InputNumber, Popconfirm, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined, UserAddOutlined, UserDeleteOutlined } from '@ant-design/icons';
import { houseMemberApi } from '../../api/houseMemberApi';
import { useAuth } from '../../auth/AuthContext';

export default function HouseMembers() {
  const { user } = useAuth();
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [retry, setRetry] = useState(0);
  const [adding, setAdding] = useState(false);
  const [removingId, setRemovingId] = useState(null);
  const [addForm] = Form.useForm();

  useEffect(() => {
    if (user?.role !== 'OWNER') return undefined;
    const controller = new AbortController();
    setLoading(true);
    setLoadError(null);
    houseMemberApi.getMembers({ signal: controller.signal })
      .then(data => {
        if (!controller.signal.aborted) setMembers(Array.isArray(data) ? data : data?.content || []);
      })
      .catch(error => {
        if (!controller.signal.aborted) setLoadError(error);
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [user?.role, retry]);

  if (user?.role !== 'OWNER') return null;

  async function addMember(values) {
    setAdding(true);
    try {
      await houseMemberApi.addMember(values.userId);
      message.success('Đã thêm thành viên vào nhà.');
      addForm.resetFields();
      setRetry(value => value + 1);
    } catch (error) { message.error(error.message || 'Không thể thêm thành viên.'); }
    finally { setAdding(false); }
  }

  async function removeMember(member) {
    setRemovingId(member.id);
    try {
      await houseMemberApi.removeMember(member.id);
      message.success(`Đã gỡ ${member.fullName || member.username || `người dùng #${member.id}`} khỏi nhà.`);
      setMembers(current => current.filter(item => item.id !== member.id));
    } catch (error) { message.error(error.message || 'Không thể gỡ thành viên.'); }
    finally { setRemovingId(null); }
  }

  const columns = [
    { title: 'Mã người dùng', dataIndex: 'id', width: 130, render: id => `#${id}` },
    { title: 'Tên đăng nhập', dataIndex: 'username', render: value => value || '—' },
    { title: 'Họ tên', dataIndex: 'fullName', render: value => value || '—' },
    { title: 'Vai trò', dataIndex: 'role', render: value => <Tag color={value === 'OWNER' ? 'blue' : 'default'}>{value || 'MEMBER'}</Tag> },
    { title: 'Thao tác', key: 'action', width: 130, render: (_, member) => (
      <Popconfirm title="Gỡ thành viên này khỏi nhà?" okText="Gỡ thành viên" cancelText="Hủy" onConfirm={() => removeMember(member)}>
        <Button danger icon={<UserDeleteOutlined />} loading={removingId === member.id} disabled={member.id === user.id}>Gỡ</Button>
      </Popconfirm>
    ) },
  ];

  return <Card title="Quản lý thành viên" style={{ marginTop: 24 }}>
    <Typography.Paragraph type="secondary">API xác định ngôi nhà từ tài khoản chủ nhà và hiện chỉ hỗ trợ một nhà liên kết. Bạn có thể thêm bằng mã người dùng hoặc gỡ thành viên khỏi nhà.</Typography.Paragraph>
    {loadError && <Alert type="error" showIcon message={loadError.message || 'Không tải được danh sách thành viên.'} action={<Button size="small" icon={<ReloadOutlined />} onClick={() => setRetry(value => value + 1)}>Thử lại</Button>} style={{ marginBottom: 16 }} />}
    <Flex justify="space-between" align="center" wrap gap="middle" style={{ marginBottom: 12 }}>
      <Typography.Title level={5} style={{ margin: 0 }}>Danh sách thành viên</Typography.Title>
      <Button icon={<ReloadOutlined />} onClick={() => setRetry(value => value + 1)} loading={loading}>Tải lại</Button>
    </Flex>
    <Table
      rowKey="id"
      size="small"
      loading={loading}
      dataSource={members}
      columns={columns}
      scroll={{ x: 'max-content' }}
      pagination={{ pageSize: 8, showSizeChanger: false }}
      locale={{ emptyText: <Empty description={loadError ? 'Không có dữ liệu để hiển thị' : 'Nhà chưa có thành viên'} /> }}
    />
    <Form form={addForm} layout="inline" onFinish={addMember} style={{ marginTop: 16 }}>
      <Form.Item name="userId" rules={[{ required: true, message: 'Nhập mã người dùng' }]}>
        <InputNumber min={1} placeholder="Mã người dùng" aria-label="Mã người dùng cần thêm" />
      </Form.Item>
      <Button htmlType="submit" type="primary" icon={<UserAddOutlined />} loading={adding}>Thêm thành viên</Button>
    </Form>
  </Card>;
}
