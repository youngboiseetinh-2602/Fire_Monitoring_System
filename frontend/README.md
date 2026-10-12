# Fire Monitoring — React frontend

Giao diện React + Bootstrap, chạy bằng Vite. Trang chủ dựa trên ảnh tham khảo, hỗ trợ desktop, tablet và điện thoại. Login/Register mở hộp thoại có thể thao tác bằng bàn phím.

## Chạy local

Yêu cầu Node.js 20.19+ hoặc 22.12+.

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Mở http://localhost:5173. Trên PowerShell dùng `npm.cmd` nếu execution policy chặn `npm.ps1`.

Chạy backend Spring Boot và MySQL với cấu hình của dự án để dùng đăng nhập/đăng ký. Vite chuyển tiếp `/public/*` đến `http://localhost:8080`. Nếu backend dùng cổng khác, sao chép `.env.example` thành `.env.local` rồi sửa `API_PROXY_TARGET`.

## Build

```powershell
npm.cmd run build
npm.cmd run preview
```

Kết quả nằm trong `dist/`. Khi triển khai, cấu hình reverse proxy `/public/*` đến backend hoặc đặt `VITE_API_BASE_URL` trước khi build và cho phép origin frontend trong cấu hình CORS backend. Proxy Vite chỉ áp dụng khi chạy dev/preview.

## API và phạm vi

- `POST /public/login`: gửi `username`, `password`; nhận JWT dạng text.
- `POST /public/register`: gửi `username`, `password`, `fullName`, `cccd`, `role: MEMBER` và `phone` nếu có.
- Hiển thị lỗi validation từ `ProblemDetail.validationErrors`, lỗi trùng dữ liệu, sai thông tin đăng nhập và lỗi kết nối.
- Token chỉ giữ trong bộ nhớ React, không lưu mật khẩu/token vào localStorage. Reload cần đăng nhập lại; Log out xóa phiên trong bộ nhớ.
- Sau đăng nhập hiển thị lời chào. Dashboard giám sát, bản đồ và cảnh báo trực tiếp chưa thuộc giao diện khởi đầu này.

## Ảnh nền

Asset: `public/images/forest-fire.jpg`, tạo bằng công cụ imagegen tích hợp rồi chuyển PNG sang JPEG để giảm dung lượng. Đây là ảnh minh họa AI, không phải dữ liệu cháy thực tế. Prompt đầy đủ ở `docs/hero-image-prompt.txt`.
