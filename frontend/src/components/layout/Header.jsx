import { Avatar, Button, Dropdown, Flex, Layout } from 'antd';
import { DownOutlined, LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import Brand from '../common/Brand';

export default function Header({ onMenu, collapsed, mobile }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return <Layout.Header className="app-header">
    <Flex align="center" gap={12}>
      <Button type="text" className="menu-toggle" icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        aria-label={mobile ? 'Mở menu' : 'Thu gọn hoặc mở rộng menu'} onClick={onMenu} />
      <Brand />
    </Flex>
    <Dropdown trigger={['click']} menu={{ items: [
      { key: 'profile', icon: <UserOutlined />, label: 'Hồ sơ cá nhân' },
      { type: 'divider' },
      { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất' },
    ], onClick: ({ key }) => {
      if (key === 'logout') { logout(); navigate('/login', { replace: true }); }
      else navigate('/app/profile');
    } }}>
      <Button type="text" className="account-button" aria-label="Menu tài khoản">
        <Avatar className="user-avatar" icon={<UserOutlined />} />
        <span className="account-name"><strong>{user.fullName}</strong><small>{user.username}</small></span>
        <DownOutlined />
      </Button>
    </Dropdown>
  </Layout.Header>;
}
