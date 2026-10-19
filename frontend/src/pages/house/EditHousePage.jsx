import { Button, Result } from 'antd';
import { Link, useLocation, useParams } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import HouseForm from './HouseForm';

export default function EditHousePage() {
  const { houseId } = useParams();
  const { state } = useLocation();
  const house = state?.house;
  const returnTo = /^\/app\/houses(?:\?|$)/.test(state?.returnTo || '') ? state.returnTo : '/app/houses';
  // There is no GET /houses/{id}; editing starts from a loaded list record.
  if (!house || String(house.id) !== houseId) return <Result status="info" title="Chọn ngôi nhà cần chỉnh sửa"
    subTitle="Mở danh sách nhà ở rồi chọn Sửa để tải thông tin hiện tại."
    extra={<Link to="/app/houses"><Button type="primary">Danh sách nhà ở</Button></Link>} />;
  return <>
    <PageHeader title="Chỉnh sửa ngôi nhà" description="Cập nhật thông tin địa chỉ và trạng thái ngôi nhà."
      crumbs={[{ title: 'Nhà ở', to: returnTo }, { title: `Nhà #${house.id}` }, { title: 'Chỉnh sửa' }]} />
    <HouseForm house={house} returnTo={returnTo} />
  </>;
}
