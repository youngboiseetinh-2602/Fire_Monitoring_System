import { Tag } from 'antd';
import { FireFilled } from '@ant-design/icons';

export default function StatusTag({ value }) {
  const labels = {
    NORMAL: 'Bình thường', UNNORMAL: 'Bất thường', DANGER: 'Nguy hiểm',
    FIRE: 'Cháy', EXTREME_FIRE: 'Cháy nghiêm trọng',
    ADMIN: 'Quản trị viên', OWNER: 'Chủ nhà', MEMBER: 'Thành viên',
  };
  const colors = {
    NORMAL: 'green', UNNORMAL: 'gold', DANGER: 'orange',
    FIRE: 'red', EXTREME_FIRE: 'volcano',
    ADMIN: 'purple', OWNER: 'orange', MEMBER: 'blue',
  };
  return <Tag className={`status-tag status-${value}`} color={colors[value]}>
    {value === 'FIRE' || value === 'EXTREME_FIRE'
      ? <FireFilled />
      : value === 'NORMAL' ? <span className="status-dot" /> : null}
    {labels[value] || value}
  </Tag>;
}
