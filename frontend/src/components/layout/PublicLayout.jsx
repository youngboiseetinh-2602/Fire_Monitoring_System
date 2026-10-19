import { Col, Row, Typography } from 'antd';
import { HomeOutlined, SafetyCertificateOutlined, TeamOutlined } from '@ant-design/icons';
import { Outlet, useLocation } from 'react-router-dom';
import Brand from '../common/Brand';

export default function PublicLayout() {
  const { pathname } = useLocation();
  return <div className="auth-page">
    <header className="public-header"><Brand /><p className="quote">“An tâm hôm nay,<br />vững vàng ngày mai.”</p></header>
    <main id="main" className="auth-layout">
      <Row gutter={[{ xs: 0, md: 32, xl: 48 }, 28]} align="middle">
        <Col xs={24} md={12} className="story-column">
          <section className="auth-story">
            <span className="eyebrow">CÙNG NHAU VÌ CỘNG ĐỒNG AN TOÀN</span>
            <h1>Một ngày mai<br />an toàn hơn.<br /><em>Bắt đầu từ bạn.</em></h1>
            <Typography.Paragraph>Quản lý thông tin ngôi nhà và kết nối với cộng đồng của bạn, trong một không gian chung.</Typography.Paragraph>
            <div className="story-features">
              <span><HomeOutlined />Quản lý nhà ở</span>
              <span><SafetyCertificateOutlined />Theo dõi trạng thái</span>
              <span><TeamOutlined />Kết nối cộng đồng</span>
            </div>
            <div className="story-note"><img src="/flame.svg" alt="" /><div>
              <strong>Mỗi ngôi nhà, một sự quan tâm.</strong>
              <p>Cùng xây dựng một cộng đồng an toàn hơn.</p>
            </div></div>
          </section>
        </Col>
        <Col xs={24} md={12}><div key={pathname} className="page-enter"><Outlet /></div></Col>
      </Row>
    </main>
    <footer className="auth-footer">Fire Monitoring System <span>Vì những ngôi nhà an toàn hơn.</span></footer>
  </div>;
}
