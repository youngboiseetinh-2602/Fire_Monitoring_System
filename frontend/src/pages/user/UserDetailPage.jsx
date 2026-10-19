import { useEffect, useState } from 'react';
import { Avatar, Button, Card, Descriptions, Flex, Typography } from 'antd';
import { ArrowLeftOutlined, EditOutlined, UserOutlined } from '@ant-design/icons';
import { Link, useParams } from 'react-router-dom';
import { userApi } from '../../api/userApi';
import { ApiError, Loading } from '../../components/common/Feedback';
import PageHeader from '../../components/common/PageHeader';
import StatusTag from '../../components/common/StatusTag';

export default function UserDetailPage({ own = false }) {
  const { userId } = useParams();
  const [query, setQuery] = useState({ loading: true });
  const [retryCount, setRetryCount] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setQuery({ loading: true });
    async function loadUser() {
      try {
        const data = own ? await userApi.getMe({ signal: controller.signal })
          : await userApi.getInfo(userId, controller.signal);
        if (!controller.signal.aborted) setQuery({ data, loading: false });
      } catch (error) {
        if (!controller.signal.aborted) setQuery({ error, loading: false });
      }
    }
    loadUser();
    return () => controller.abort();
  }, [own, userId, retryCount]);
  const user = query.data;
  return <>
    <PageHeader title={own ? 'Hồ sơ của tôi' : 'Chi tiết người dùng'} description="Thông tin cá nhân và tài khoản trong hệ thống."
      crumbs={own ? [] : [{ title: 'Người dùng', to: '/app/users' }, { title: 'Chi tiết' }]} />
    {!own && <Link to="/app/users"><Button className="back-button" icon={<ArrowLeftOutlined />}>Danh sách người dùng</Button></Link>}
    {query.loading ? <Loading /> : query.error ? <ApiError error={query.error} retry={() => setRetryCount(count => count + 1)} /> : <div className="content-stack">
      <Card className="profile-card">
        <Flex align="center" gap={24} wrap>
          <Avatar size={88} icon={<UserOutlined />} />
          <div className="profile-identity">
            <span className="eyebrow">TÀI KHOẢN #{user.id}</span>
            <Typography.Title level={2}>{user.fullName}</Typography.Title>
            <Typography.Text type="secondary">@{user.username}</Typography.Text>
            <StatusTag value={user.role} />
          </div>
          {own && <Link className="profile-action" to="/app/profile/edit"><Button type="primary" icon={<EditOutlined />}>Chỉnh sửa hồ sơ</Button></Link>}
        </Flex>
      </Card>
      <Card title="Thông tin cá nhân" className="content-card">
        <Descriptions column={{ xs: 1, sm: 1, md: 2 }} layout="vertical" items={[
          { key: 'name', label: 'Họ và tên', children: user.fullName },
          { key: 'username', label: 'Tên đăng nhập', children: user.username },
          { key: 'phone', label: 'Số điện thoại', children: user.phone || 'Chưa bổ sung' },
          { key: 'cccd', label: 'CCCD', children: user.cccd },
        ]} />
      </Card>
    </div>}
  </>;
}
