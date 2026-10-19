import PageHeader from '../../components/common/PageHeader';
import HouseForm from './HouseForm';

export default function CreateHousePage() {
  return <>
    <PageHeader title="Thêm ngôi nhà mới" description="Đăng ký thông tin ngôi nhà vào hệ thống."
      crumbs={[{ title: 'Nhà ở', to: '/app/houses' }, { title: 'Thêm mới' }]} />
    <HouseForm />
  </>;
}
