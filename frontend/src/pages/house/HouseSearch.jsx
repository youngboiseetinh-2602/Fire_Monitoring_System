import { Button, Card, Col, Flex, Form, Input, Row } from 'antd';
import { SearchOutlined } from '@ant-design/icons';

export default function HouseSearch({ filters, onSearch, onReset }) {
  const [form] = Form.useForm();
  return <Card title="Tìm kiếm ngôi nhà" className="content-card">
    <p className="filter-description">Tìm theo thành phố, phường hoặc địa chỉ. Khớp một trong các điều kiện (OR).</p>
    <Form form={form} layout="vertical" initialValues={filters} onFinish={onSearch}>
      <Row gutter={16}>
        <Col xs={24} md={8}><Form.Item name="city" label="Thành phố / Tỉnh"><Input allowClear placeholder="Nhập thành phố / tỉnh" /></Form.Item></Col>
        <Col xs={24} md={8}><Form.Item name="ward" label="Phường / Xã"><Input allowClear placeholder="Nhập phường / xã" /></Form.Item></Col>
        <Col xs={24} md={8}><Form.Item name="detail" label="Địa chỉ chi tiết">
          <Input.Search allowClear placeholder="Số nhà, tên đường…" onSearch={() => form.submit()} />
        </Form.Item></Col>
      </Row>
      <Flex justify="end" gap={12} wrap>
        <Button onClick={() => { form.resetFields(); onReset(); }}>Xóa bộ lọc</Button>
        <Button type="primary" htmlType="submit" icon={<SearchOutlined />} aria-label="Tìm kiếm nhà">Tìm kiếm</Button>
      </Flex>
    </Form>
  </Card>;
}
