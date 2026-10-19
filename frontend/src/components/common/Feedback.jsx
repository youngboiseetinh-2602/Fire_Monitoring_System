import { Alert, Button, Card, Skeleton } from 'antd';

export function Loading() {
  return <Card role="status" aria-label="Đang tải dữ liệu" className="content-card">
    <Skeleton active paragraph={{ rows: 4 }} />
  </Card>;
}

export function ApiError({ error, retry }) {
  if (!error) return null;
  return <Alert className="api-error" showIcon type="error" message={error.message}
    action={retry && <Button size="small" onClick={retry}>Thử lại</Button>} />;
}
