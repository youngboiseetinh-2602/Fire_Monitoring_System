# Fire Monitoring — frontend

Giao diện React 19/Vite bằng tiếng Việt, sử dụng **Ant Design 5**, Ant Design Icons, React Router và Axios. Theme kem–cam được cấu hình qua ConfigProvider; font Inter được đóng gói local. Tận dụng minh họa SVG hiện có. Chỉ sử dụng các API đã tồn tại trong backend; không dùng dữ liệu mẫu trong ứng dụng.

Sidebar cố định trên desktop, thu gọn trên tablet và chuyển thành Drawer dưới 768px. Các form sử dụng Row/Col của Ant Design. Bảng cuộn ngang bên trong vùng nội dung. Animation CSS hỗ trợ `prefers-reduced-motion`.

## Chạy local

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Mở địa chỉ Vite in ra (mặc định http://localhost:5173). Chạy backend Spring Boot và database riêng. Vite proxy `/public`, `/users`, `/houses` tới `http://localhost:8080`. Có thể đặt `API_PROXY_TARGET` trong `.env.local`; xem `.env.example`.

## Các màn hình và API

| Màn hình | API |
| --- | --- |
| Đăng nhập | `POST /public/login`, sau đó `GET /users/me` |
| Đăng ký | `POST /public/register` với `role: MEMBER` |
| Danh sách nhà | `GET /houses?page=0` |
| Tìm nhà | `GET /houses/search?city=...&ward=...&detail=...&page=0` |
| Tạo nhà | `POST /houses/createHouse` |
| Sửa nhà | `PUT /houses/updateHouse/{houseId}` |
| Danh sách/tìm người dùng (ADMIN) | `GET /users?fullName=...&username=...&cccd=...&page=0` |
| Chi tiết người dùng | `GET /users/{userId}` |
| Hồ sơ cá nhân | `GET /users/me`, `PUT /users/me` |

Tìm kiếm sử dụng OR theo backend. Danh sách có phân trang 20 bản ghi, trạng thái tải/rỗng/lỗi và thử lại. Tổng số kết quả lấy từ `Page.totalElements`.

Form sửa nhà được mở từ dữ liệu dòng trong danh sách, không gọi API chi tiết nhà chưa tồn tại. Tạo nhà chỉ gửi tên và ba trường địa chỉ; trạng thái mặc định do server gán. Sửa nhà gửi thêm `status` (`NORMAL` hoặc `FIRE`). Hồ sơ chỉ sửa `fullName`, `phone`, `cccd`.

Không có chức năng email, upload avatar, quên mật khẩu, cảnh báo trực tiếp, thiết bị, xóa nhà, chỉnh sửa tài khoản người khác hoặc thống kê tổng hợp chưa được API hỗ trợ. Avatar mặc định dùng biểu tượng người dùng. Nền SVG chỉ là minh họa.

Token lưu trong bộ nhớ, gửi qua Bearer header và bị xóa khi đăng xuất hoặc tải lại trang. Menu và route danh sách người dùng chỉ dành cho ADMIN; OWNER/MEMBER có nhà ở và hồ sơ. Backend vẫn là nơi kiểm soát quyền. Khi API được bảo vệ trả 401, phiên bị xóa và chuyển về đăng nhập; sau đăng nhập sẽ quay lại đường dẫn đang yêu cầu. Không ghi JWT hoặc mật khẩu vào localStorage.

Frontend dùng các đường dẫn `/login`, `/register`, `/app/houses`, `/app/houses/new`, `/app/houses/:houseId/edit`, `/app/users`, `/app/users/:userId`, `/app/profile`, `/app/profile/edit`. Prefix `/app` giúp đường dẫn trang không trùng endpoint backend. Router import trực tiếp các trang để dễ theo dõi luồng điều hướng.

Form sửa nhà dùng bản ghi nhận từ danh sách qua router state. Nếu thiếu bản ghi khi mở trực tiếp đường dẫn, trang hướng dẫn quay lại danh sách thay vì gọi endpoint chi tiết không tồn tại. Đổi trạng thái nhà có Modal xác nhận. Bộ lọc và trang danh sách được giữ trong query string của URL.

## Tổ chức source

- `src/api/`: Axios instance, interceptor và API theo module.
- `src/auth/`: phiên đăng nhập trong bộ nhớ.
- `src/components/layout/`: PublicLayout, MainLayout, Header, Sidebar.
- `src/components/common/`: trạng thái tải/lỗi trong `Feedback.jsx`, tiêu đề trang, Tag và validation dùng chung.
- `src/pages/auth`, `src/pages/user`, `src/pages/house`: màn hình theo module.
- Các trang tự quản lý state và gọi API trong `useEffect`, có hủy request khi rời trang; không có lớp custom hook trung gian. Bộ lọc/phân trang đọc trực tiếp từ URL.
- `src/routes/`: bảo vệ route và kiểm tra quyền.
- `src/styles/`: theme bổ sung, animation và responsive.

Đọc theo luồng: `AppRoutes.jsx` → trang trong `pages/` → module trong `api/`. `UserDetailPage` hiển thị cả hồ sơ của mình (`own`) và chi tiết người dùng; `HouseForm` dùng chung cho tạo/sửa nhà. Các modal xác nhận được gọi trực tiếp tại form qua `App.useApp()` của Ant Design.

## Kiểm tra và build

```powershell
npm.cmd test
npm.cmd run test:e2e
npm.cmd run build
npm.cmd run preview
```

`npm test` dùng Axios adapter giả lập để kiểm tra endpoint, DTO, JWT và lỗi HTTP. `npm run test:e2e` dùng Playwright với Edge headless được cài trên Windows, khởi chạy Vite ở cổng 5178 và chặn request bằng dữ liệu kiểm thử. Không gọi database hoặc tạo tài khoản thật. Có thể đặt `PLAYWRIGHT_CHANNEL=chrome` khi dùng Chrome; cấu hình trong `playwright.config.js`.

Kiểm thử trình duyệt gồm đăng nhập lỗi, đăng ký/validation, danh sách/tìm kiếm/phân trang, tạo/sửa nhà, chi tiết người dùng, sửa hồ sơ, phân quyền, hết phiên, lỗi/empty state và viewport 1440px, 900px, 375px. Screenshot và trace được ghi trong `test-results/` (đã ignore). Kiểm thử với backend thật cần chạy backend/database và tài khoản của bạn.

Build ở `dist/`. Khi triển khai, cấu hình SPA fallback về `index.html` cho đường dẫn frontend và reverse proxy cả ba prefix `/public`, `/users`, `/houses`, hoặc đặt `VITE_API_BASE_URL` trước khi build với origin đã được backend cho phép. Proxy Vite chỉ áp dụng cho dev/preview. Không thay đổi SecurityConfig/CORS trong lần triển khai frontend này.
