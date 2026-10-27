import { useEffect, useState } from 'react';
import { Avatar, Button, Card, Empty, Flex, Table, Typography } from 'antd';
import { EditOutlined, HomeOutlined, PlusOutlined, DashboardOutlined } from '@ant-design/icons';
import { Link, useLocation, useSearchParams } from 'react-router-dom';
import { houseApi } from '../../api/houseApi';
import { cleanFilters } from '../../api/query';
import { ApiError, Loading } from '../../components/common/Feedback';
import PageHeader from '../../components/common/PageHeader';
import StatusTag from '../../components/common/StatusTag';
import HouseSearch from './HouseSearch';

export default function HouseListPage() {
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState({ loading: true });
  const [retryCount, setRetryCount] = useState(0);
  const location = useLocation();
  const filters = cleanFilters(Object.fromEntries(params), ['city', 'ward', 'detail']);
  const filterKey = JSON.stringify(filters);
  const filtered = Object.keys(filters).length > 0;
  const requestedPage = Number(params.get('page') || 0);
  const page = Number.isSafeInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;

  useEffect(() => {
    const controller = new AbortController();
    setQuery({ loading: true });
    async function loadHouses() {
      try {
        const criteria = JSON.parse(filterKey);
        const data = Object.keys(criteria).length
          ? await houseApi.search(criteria, page, controller.signal)
          : await houseApi.findAll(page, controller.signal);
        if (!controller.signal.aborted) setQuery({ data, loading: false });
      } catch (error) {
        if (!controller.signal.aborted) setQuery({ error, loading: false });
      }
    }
    loadHouses();
    return () => controller.abort();
  }, [filterKey, page, retryCount]);

  function search(values) {
    setParams({ ...cleanFilters(values, ['city', 'ward', 'detail']), page: '0' });
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70, render: id => `#${id}` },
    { title: 'Ngôi nhà', key: 'house', render: (_, house) => <div className="table-person">
      <Avatar shape="square" size={44} className="house-avatar" icon={<HomeOutlined />} />
      <div><strong>{house.houseName}</strong><small>{house.addressDetail}</small></div>
    </div> },
    { title: 'Thành phố / Tỉnh', dataIndex: 'city' },
    { title: 'Phường / Xã', dataIndex: 'ward' },
    { title: 'Trạng thái', dataIndex: 'status', render: value => <StatusTag value={value} /> },
    { title: 'Thao tác', key: 'actions', render: (_, house) => (
      <Flex gap="small">
        {/* Nút Giám sát mới được thêm vào */}
        <Link to={`/app/houses/${house.id}/dashboard`} state={{ houseName: house.houseName }}>
          <Button type="primary" icon={<DashboardOutlined />}>Giám sát</Button>
        </Link>
        <Link to={`/app/houses/${house.id}/edit`} state={{ house, returnTo: location.pathname + location.search }}>
          <Button icon={<EditOutlined />} aria-label={`Sửa ${house.houseName}`}>Sửa</Button>
        </Link>
      </Flex>
    ) },
  ];

  return <>
    <PageHeader title="Quản lý nhà ở" description="Một nơi để theo dõi và quản lý các ngôi nhà trong hệ thống." />
    <Flex justify="space-between" align="center" wrap className="section-toolbar">
      <Typography.Title level={2}>Danh sách ngôi nhà</Typography.Title>
      <Link to="/app/houses/new" className="create-house-link"><Button type="primary" icon={<PlusOutlined />}>Thêm ngôi nhà</Button></Link>
    </Flex>
    <div className="content-stack">
      <HouseSearch key={filterKey} filters={filters} onSearch={search} onReset={() => setParams({})} />
      {query.loading ? <Loading /> : query.error ? <ApiError error={query.error} retry={() => setRetryCount(count => count + 1)} /> : <Card className="content-card"
        title={filtered ? 'Kết quả tìm kiếm' : 'Tất cả ngôi nhà'} extra={<span className="load-count">{query.data.totalElements} kết quả</span>}>
        <Table rowKey="id" columns={columns} dataSource={query.data.content} scroll={{ x: 'max-content' }}
          locale={{ emptyText: <Empty description={filtered ? 'Không có ngôi nhà phù hợp' : 'Chưa có ngôi nhà nào'} /> }}
          pagination={{ current: page + 1, pageSize: query.data.size, total: query.data.totalElements, showSizeChanger: false,
            onChange: number => setParams({ ...filters, page: String(number - 1) }), showTotal: total => `${total} ngôi nhà` }} />
      </Card>}
    </div>
  </>;
}
