import { Breadcrumb, Typography } from 'antd';
import { Link } from 'react-router-dom';

export default function PageHeader({ title, description, crumbs = [] }) {
  return <section className="page-banner">
    {crumbs.length > 0 && <Breadcrumb items={crumbs.map(item => ({ title: item.to ? <Link to={item.to}>{item.title}</Link> : item.title }))} />}
    <Typography.Title level={1}>{title}</Typography.Title>
    <Typography.Paragraph>{description}</Typography.Paragraph>
  </section>;
}
