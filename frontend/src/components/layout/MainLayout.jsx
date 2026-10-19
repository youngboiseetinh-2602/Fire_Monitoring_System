import { useEffect, useRef, useState } from 'react';
import { Drawer, Grid, Layout } from 'antd';
import { Outlet, useLocation } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';

export default function MainLayout() {
  const screens = Grid.useBreakpoint();
  const mobile = !screens.md;
  const [collapsed, setCollapsed] = useState(false);
  const [drawer, setDrawer] = useState(false);
  const { pathname } = useLocation();
  const main = useRef(null);
  useEffect(() => { setCollapsed(!screens.xl); }, [screens.xl]);
  useEffect(() => { if (!mobile) setDrawer(false); }, [mobile]);
  useEffect(() => { main.current?.focus(); window.scrollTo(0, 0); }, [pathname]);
  const sidebarWidth = mobile ? 0 : collapsed ? 80 : 236;

  return <Layout className="main-layout">
    <Header mobile={mobile} collapsed={mobile || collapsed}
      onMenu={() => mobile ? setDrawer(true) : setCollapsed(value => !value)} />
    {!mobile && <Layout.Sider className="desktop-sidebar" width={236} collapsedWidth={80}
      theme="light" collapsed={collapsed} trigger={null}>
      <Sidebar collapsed={collapsed} />
    </Layout.Sider>}
    <Drawer title="Điều hướng" placement="left" width={280} open={mobile && drawer}
      onClose={() => setDrawer(false)} className="mobile-drawer">
      <Sidebar onNavigate={() => setDrawer(false)} />
    </Drawer>
    <Layout.Content className="workspace" style={{ marginLeft: sidebarWidth }}>
      <div id="main" ref={main} tabIndex={-1} className="page-enter" key={pathname}>
        <Outlet />
      </div>
    </Layout.Content>
  </Layout>;
}
