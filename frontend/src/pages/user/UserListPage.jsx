import { useEffect, useState } from 'react';
import { Avatar, Button, Card, Col, Empty, Flex, Form, Input, Row, Table, Typography } from 'antd';
import { EyeOutlined, SearchOutlined, UserOutlined } from '@ant-design/icons';
import { Link, useSearchParams } from 'react-router-dom';
import { userApi } from '../../api/userApi';
import { cleanFilters } from '../../api/query';
import { ApiError, Loading } from '../../components/common/Feedback';
import PageHeader from '../../components/common/PageHeader';
import StatusTag from '../../components/common/StatusTag';

export default function UserListPage() {
  const [form] = Form.useForm();
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState({ loading: true });
  const [retryCount, setRetryCount] = useState(0);
  const filters = cleanFilters(Object.fromEntries(params), ['fullName', 'username', 'cccd']);
  const filterKey = JSON.stringify(filters);
  const requestedPage = Number(params.get('page') || 0);
  const page = Number.isSafeInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;

  useEffect(() => {
    form.setFieldsValue({ fullName: '', username: '', cccd: '', ...JSON.parse(filterKey) });
  }, [filterKey, form]);

  useEffect(() => {
    const controller = new AbortController();
    setQuery({ loading: true });
    async function loadUsers() {
      try {
        const data = await userApi.search(JSON.parse(filterKey), page, controller.signal);
        if (!controller.signal.aborted) setQuery({ data, loading: false });
      } catch (error) {
        if (!controller.signal.aborted) setQuery({ error, loading: false });
      }
    }
    loadUsers();
    return () => controller.abort();
  }, [filterKey, page, retryCount]);

  function search(values) {
    setParams({ ...cleanFilters(values, ['fullName', 'username', 'cccd']), page: '0' });
  }

  function reset() {
    form.setFieldsValue({ fullName: '', username: '', cccd: '' });
    setParams({});
  }
  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70, render: id => `#${id}` },
    { title: 'Người dùng', dataIndex: 'fullName', render: name => <div className="table-person"><Avatar icon={<UserOutlined />} className="user-avatar" /><strong>{name}</strong></div> },
    { title: 'Tên đăng nhập', dataIndex: 'username' },
    { title: 'Điện thoại', dataIndex: 'phone', render: value => value || '—' },
    { title: 'Vai trò', dataIndex: 'role', render: value => <StatusTag value={value} /> },
    { title: 'Thao tác', key: 'actions', render: (_, user) => <Link to={`/app/users/${user.id}`}><Button icon={<EyeOutlined />} aria-label={`Xem ${user.fullName}`}>Xem</Button></Link> },
  ];

  return <>
    <PageHeader title="Người dùng" description="Tìm kiếm và xem thông tin người dùng trong cộng đồng." />
    <div className="content-stack">
      <Card title="Tìm kiếm người dùng" className="content-card">
        <p className="filter-description">Khớp một trong các điều kiện. Để trống để xem tất cả.</p>
        <Form form={form} layout="vertical" initialValues={filters} onFinish={search}>
          <Row gutter={16}>
            <Col xs={24} md={8}><Form.Item name="fullName" label="Họ và tên"><Input.Search placeholder="Nhập họ và tên" allowClear onSearch={() => form.submit()} /></Form.Item></Col>
            <Col xs={24} md={8}><Form.Item name="username" label="Tên đăng nhập"><Input placeholder="Nhập tên đăng nhập" allowClear /></Form.Item></Col>
            <Col xs={24} md={8}><Form.Item name="cccd" label="CCCD"><Input placeholder="Nhập CCCD" allowClear /></Form.Item></Col>
          </Row>
          <Flex justify="end" gap={12} wrap>
            <Button onClick={reset}>Xóa bộ lọc</Button>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>Tìm kiếm</Button>
          </Flex>
        </Form>
      </Card>
      {query.loading ? <Loading /> : query.error ? <ApiError error={query.error} retry={() => setRetryCount(count => count + 1)} /> : <Card className="content-card" title="Danh sách người dùng" extra={<Typography.Text type="secondary">{query.data.totalElements} kết quả</Typography.Text>}>
        <Table rowKey="id" columns={columns} dataSource={query.data.content} scroll={{ x: 'max-content' }}
          locale={{ emptyText: <Empty description="Không tìm thấy người dùng" /> }}
          pagination={{ current: page + 1, pageSize: query.data.size, total: query.data.totalElements, showSizeChanger: false,
            onChange: number => setParams({ ...filters, page: String(number - 1) }), showTotal: total => `${total} người dùng` }} />
      </Card>}
    </div>
  </>;
}
