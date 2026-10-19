import { Button, Result } from 'antd';
import { Link, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import PublicLayout from '../components/layout/PublicLayout';

import MainLayout from '../components/layout/MainLayout';
import LoginPage from '../pages/auth/LoginPage';
import RegisterPage from '../pages/auth/RegisterPage';
import UserListPage from '../pages/user/UserListPage';
import UserDetailPage from '../pages/user/UserDetailPage';
import EditProfilePage from '../pages/user/EditProfilePage';
import HouseListPage from '../pages/house/HouseListPage';
import CreateHousePage from '../pages/house/CreateHousePage';
import EditHousePage from '../pages/house/EditHousePage';

function RequireAuth({ admin = false }) {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  if (admin && user.role !== 'ADMIN') return <Result status="403" title="Không có quyền truy cập"
    subTitle="Trang này dành cho quản trị viên." extra={<Link to="/app/houses"><Button>Về nhà ở</Button></Link>} />;
  return <Outlet />;
}

function PublicOnly() {
  const { user } = useAuth();
  const { state } = useLocation();
  const destination = typeof state?.from === 'string' && state.from.startsWith('/app/') ? state.from : '/app/houses';
  return user ? <Navigate to={destination} replace /> : <PublicLayout />;
}

export default function AppRoutes() {
  return <Routes>
    <Route element={<PublicOnly />}>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
    </Route>
    <Route element={<RequireAuth />}>
      <Route path="/app" element={<MainLayout />}>
        <Route index element={<Navigate to="houses" replace />} />
        <Route path="houses" element={<HouseListPage />} />
        <Route path="houses/new" element={<CreateHousePage />} />
        <Route path="houses/:houseId/edit" element={<EditHousePage />} />
        <Route path="profile" element={<UserDetailPage own />} />
        <Route path="profile/edit" element={<EditProfilePage />} />
        <Route element={<RequireAuth admin />}>
          <Route path="users" element={<UserListPage />} />
          <Route path="users/:userId" element={<UserDetailPage />} />
        </Route>
      </Route>
    </Route>
    <Route path="/" element={<Navigate to="/app/houses" replace />} />
    <Route path="*" element={<Result status="404" title="Không tìm thấy trang" extra={<Link to="/"><Button type="primary">Về trang chủ</Button></Link>} />} />
  </Routes>;
}
