import { App as AntApp, ConfigProvider } from 'antd';
import viVN from 'antd/locale/vi_VN';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import AppRoutes from './routes/AppRoutes';

const theme = {
  token: {
    colorPrimary: '#FF5A2F', colorInfo: '#FF5A2F', colorSuccess: '#22C55E',
    colorWarning: '#F59E0B', colorError: '#EF4444', colorBgLayout: '#FFF7F2',
    colorBgContainer: '#FFFFFF', colorText: '#1F2937', borderRadius: 12,
    fontFamily: 'Inter, "Segoe UI", sans-serif', controlHeight: 44,
  },
  components: {
    Button: { borderRadius: 12, primaryShadow: '0 4px 16px #ff5a2f20' },
    Input: { borderRadius: 12, activeShadow: '0 0 0 3px #ff5a2f18' },
    Card: { borderRadiusLG: 20, paddingLG: 26 },
    Table: { headerBg: '#FAF7F4', rowHoverBg: '#FFF7F0', borderColor: '#F0E7DF' },
    Layout: { headerBg: '#FFFDFA', siderBg: '#FFFDFA', bodyBg: '#FFF7F2' },
    Menu: { itemBg: 'transparent', itemSelectedColor: '#FFFFFF', itemSelectedBg: '#FF5A2F', itemHoverBg: '#FFF0E5', itemHeight: 48 },
  },
};

export default function App() {
  return <ConfigProvider theme={theme} locale={viVN}>
    <AntApp><BrowserRouter><AuthProvider>
      <a className="skip-link" href="#main">Đến nội dung chính</a>
      <AppRoutes />
    </AuthProvider></BrowserRouter></AntApp>
  </ConfigProvider>;
}
