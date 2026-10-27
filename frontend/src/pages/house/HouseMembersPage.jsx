import { useCallback, useEffect, useState } from 'react';
import {
  App, Avatar, Button, Card, Col, Empty, Flex, Form, Input, Modal, Popconfirm, Row, Table, Tag, Typography,
} from 'antd';
import { DeleteOutlined, PlusOutlined, SearchOutlined, UserAddOutlined, UserOutlined } from '@ant-design/icons';
import { houseApi } from '../../api/houseApi';
import { userApi } from '../../api/userApi';
import { cleanFilters } from '../../api/query';
import { useAuth } from '../../auth/AuthContext';
import { ApiError, Loading } from '../../components/common/Feedback';
import PageHeader from '../../components/common/PageHeader';
import StatusTag from '../../components/common/StatusTag';

const searchFields = ['username', 'fullName', 'cccd'];

export default function HouseMembersPage() {
  const { user } = useAuth();
  const { message } = App.useApp();
  const [members, setMembers] = useState({ loading: true, data: [] });
  const [selected, setSelected] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const [search, setSearch] = useState(null);
  const [searchPage, setSearchPage] = useState(0);
  const [searchFilters, setSearchFilters] = useState({});
  const [addingId, setAddingId] = useState(null);
  const [removing, setRemoving] = useState(false);
  const [form] = Form.useForm();

  const loadMembers = useCallback(async () => {
    setMembers(current => ({ ...current, loading: true, error: null }));
    try {
      const data = await houseApi.getMembers();
      setMembers({ loading: false, data: Array.isArray(data) ? data : [] });
    } catch (error) {
      setMembers(current => ({ ...current, loading: false, error }));
    }
  }, []);

  useEffect(() => { loadMembers(); }, [loadMembers]);

  async function findUsers(filters = searchFilters, page = searchPage) {
    setSearch(current => ({ ...current, loading: true, error: null }));
    try {
      const data = await userApi.search(filters, page);
      setSearch({ loading: false, data });
    } catch (error) {
      setSearch({ loading: false, error });
    }
  }

  function submitSearch(values) {
    const filters = cleanFilters(values, searchFields);
    setSearchFilters(filters);
    setSearchPage(0);
    findUsers(filters, 0);
  }

  async function addMember(candidate) {
    setAddingId(candidate.id);
    try {
      await houseApi.addPeople({ userId: candidate.id });
      message.success(`Đã thêm ${candidate.fullName || candidate.username} vào nhà.`);
      await loadMembers();
    } catch (error) {
      message.error(error.message || 'Không thể thêm thành viên.');
    } finally {
      setAddingId(null);
    }
  }

  async function removeMember() {
    if (!selected) return;
    setRemoving(true);
    try {
      await houseApi.removePeople({ userId: selected.id });
      message.success(`Đã xóa ${selected.fullName || selected.username} khỏi nhà.`);
      setSelected(null);
      await loadMembers();
    } catch (error) {
      message.error(error.message || 'Không thể xóa thành viên.');
    } finally {
      setRemoving(false);
    }
  }

  const memberIds = new Set(members.data.map(member => member.id));
  const memberColumns = [
    { title: 'Người dùng', key: 'user', render: (_, member) => <div className="table-person"><Avatar icon={<UserOutlined />} className="user-avatar" /><div><strong>{member.fullName || member.username}</strong><small>@{member.username}</small></div></div> },
    { title: 'Điện thoại', dataIndex: 'phone', render: value => value || '—' },
    { title: 'CCCD', dataIndex: 'cccd', render: value => value || '—' },
    { title: 'Vai trò', dataIndex: 'role', render: value => <StatusTag value={value} /> },
  ];
  const resultColumns = [
    { title: 'Người dùng', key: 'user', render: (_, candidate) => <div className="table-person"><Avatar icon={<UserOutlined />} className="user-avatar" /><div><strong>{candidate.fullName || candidate.username}</strong><small>@{candidate.username}</small></div></div> },
    { title: 'CCCD', dataIndex: 'cccd', render: value => value || '—' },
    { title: 'Vai trò', dataIndex: 'role', render: value => <Tag>{value}</Tag> },
    { title: 'Thao tác', key: 'action', render: (_, candidate) => <Button type="primary" icon={<UserAddOutlined />} loading={addingId === candidate.id} disabled={memberIds.has(candidate.id) || candidate.role === 'ADMIN'} onClick={() => addMember(candidate)}>{memberIds.has(candidate.id) ? 'Đã trong nhà' : 'Thêm'}</Button> },
  ];

  return <>
    <PageHeader title="Thành viên trong nhà" description="Xem, thêm hoặc xóa người dùng thuộc ngôi nhà của bạn." />
    <div className="content-stack">
      <Card className="content-card" title="Danh sách thành viên" extra={<Button type="primary" shape="circle" icon={<PlusOutlined />} aria-label="Thêm thành viên" onClick={() => setAddOpen(true)} />}>
        {members.loading ? <Loading /> : members.error ? <ApiError error={members.error} retry={loadMembers} /> : <Table rowKey="id" columns={memberColumns} dataSource={members.data} scroll={{ x: 'max-content' }} locale={{ emptyText: <Empty description="Nhà chưa có thành viên" /> }} pagination={{ pageSize: 8, showSizeChanger: false }} onRow={member => ({ onClick: () => setSelected(member), style: { cursor: 'pointer' } })} />}
      </Card>
    </div>

    <Modal title="Thông tin thành viên" open={Boolean(selected)} onCancel={() => setSelected(null)} footer={null}>
      {selected && <Flex vertical gap={12}>
        <Typography.Title level={4} style={{ margin: 0 }}>{selected.fullName || selected.username}</Typography.Title>
        <Typography.Text>Tên đăng nhập: {selected.username}</Typography.Text>
        <Typography.Text>CCCD: {selected.cccd || '—'}</Typography.Text>
        <Typography.Text>Điện thoại: {selected.phone || '—'}</Typography.Text>
        <Popconfirm title="Xóa thành viên này khỏi nhà?" okText="Xóa" cancelText="Hủy" onConfirm={removeMember} disabled={selected.id === user.id}>
          <Button danger icon={<DeleteOutlined />} loading={removing} disabled={selected.id === user.id}>{selected.id === user.id ? 'Không thể tự xóa' : 'Xóa khỏi nhà'}</Button>
        </Popconfirm>
      </Flex>}
    </Modal>

    <Modal title="Tìm và thêm thành viên" open={addOpen} width={900} footer={null} destroyOnHidden onCancel={() => { setAddOpen(false); setSearch(null); form.resetFields(); }}>
      <Form form={form} layout="vertical" onFinish={submitSearch}>
        <Row gutter={16}>
          <Col xs={24} md={8}><Form.Item name="username" label="Tên đăng nhập"><Input allowClear placeholder="Nhập tên đăng nhập" /></Form.Item></Col>
          <Col xs={24} md={8}><Form.Item name="fullName" label="Họ và tên"><Input allowClear placeholder="Nhập họ và tên" /></Form.Item></Col>
          <Col xs={24} md={8}><Form.Item name="cccd" label="CCCD"><Input allowClear placeholder="Nhập CCCD" /></Form.Item></Col>
        </Row>
        <Flex justify="end" gap={12}>
          <Button onClick={() => { form.resetFields(); setSearch(null); setSearchFilters({}); }}>Xóa bộ lọc</Button>
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>Tìm kiếm</Button>
        </Flex>
      </Form>
      <div style={{ marginTop: 20 }}>
        {!search ? <Empty description="Nhập điều kiện rồi nhấn Tìm kiếm" /> : search.loading ? <Loading /> : search.error ? <ApiError error={search.error} retry={() => findUsers()} /> : <Table rowKey="id" columns={resultColumns} dataSource={search.data.content} scroll={{ x: 'max-content' }} locale={{ emptyText: <Empty description="Không tìm thấy người dùng" /> }} pagination={{ current: searchPage + 1, pageSize: search.data.size, total: search.data.totalElements, showSizeChanger: false, onChange: page => { const next = page - 1; setSearchPage(next); findUsers(searchFilters, next); } }} />}
      </div>
    </Modal>
  </>;
}
