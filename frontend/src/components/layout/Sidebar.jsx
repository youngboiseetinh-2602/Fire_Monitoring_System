import { Menu } from 'antd';
import { HomeOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

export default function Sidebar({ collapsed = false, onNavigate = () => {} }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const selected = pathname.startsWith('/app/users') ? '/app/users'
    : pathname.startsWith('/app/profile') ? '/app/profile' : '/app/houses';
  const items = [
    { key: '/app/houses', icon: <HomeOutlined />, label: 'Nhà ở' },
    ...(user.role === 'ADMIN' ? [{ key: '/app/users', icon: <TeamOutlined />, label: 'Người dùng' }] : []),
    { key: '/app/profile', icon: <UserOutlined />, label: 'Hồ sơ cá nhân' },
  ];
  return <div className="sidebar-inner">
    {!collapsed && <div className="nav-label">KHÔNG GIAN CỦA BẠN</div>}
    <nav aria-label="Điều hướng chính"><Menu mode="inline" selectedKeys={[selected]} items={items}
      onClick={({ key }) => { navigate(key); onNavigate(); }} /></nav>
    {!collapsed && <div className="sidebar-message">
      <img src="/flame.svg" alt="" />
      <strong>Cộng đồng an toàn.<br />Bắt đầu từ chúng ta.</strong>
      <p>Quan tâm ngôi nhà.<br />Kết nối con người.</p>
    </div>}
  </div>;
}
