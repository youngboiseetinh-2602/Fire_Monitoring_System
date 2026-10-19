import { Tag } from 'antd';
import { FireFilled } from '@ant-design/icons';

export default function StatusTag({ value }) {
  const labels = { NORMAL: 'Bình thường', FIRE: 'Cháy', ADMIN: 'Quản trị viên', OWNER: 'Chủ nhà', MEMBER: 'Thành viên' };
  const colors = { NORMAL: 'green', FIRE: 'red', ADMIN: 'purple', OWNER: 'orange', MEMBER: 'blue' };
  return <Tag className={`status-tag status-${value}`} color={colors[value]}>
    {value === 'FIRE' ? <FireFilled /> : value === 'NORMAL' ? <span className="status-dot" /> : null}
    {labels[value] || value}
  </Tag>;
}
