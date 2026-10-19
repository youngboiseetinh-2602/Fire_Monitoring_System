import axios from 'axios';

let accessToken = null;
let onSessionExpired = () => {};

export function setAccessToken(token) { accessToken = token; }
export function setSessionExpiredHandler(handler) {
  onSessionExpired = handler;
  return () => { if (onSessionExpired === handler) onSessionExpired = () => {}; };
}

export function normalizeError(failure) {
  const status = failure.response?.status;
  const data = failure.response?.data;
  const messages = {
    400: 'Vui lòng kiểm tra thông tin đã nhập.',
    401: 'Tên đăng nhập hoặc mật khẩu không đúng, hoặc phiên đăng nhập đã hết hạn.',
    403: 'Bạn không có quyền truy cập chức năng này.',
    404: 'Không tìm thấy thông tin yêu cầu.',
    409: 'Thông tin này đã được sử dụng. Vui lòng kiểm tra lại.',
  };
  const message = status >= 500 ? 'Máy chủ gặp lỗi. Vui lòng thử lại sau.'
    : messages[status] || (status ? 'Không thể thực hiện yêu cầu.' : 'Không thể kết nối máy chủ. Vui lòng thử lại.');
  const error = new Error(message);
  error.status = status;
  error.fields = data?.validationErrors || (status === 400 && data && typeof data === 'object' && !data.status
    ? Object.fromEntries(Object.entries(data).filter(([, value]) => typeof value === 'string')) : {});
  return error;
}

const axiosClient = axios.create({
  baseURL: (import.meta.env?.VITE_API_BASE_URL || '').replace(/\/$/, ''),
  timeout: 15000,
});

axiosClient.interceptors.request.use(config => {
  if (config.auth !== false && accessToken && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

axiosClient.interceptors.response.use(response => response.data, failure => {
  if (axios.isCancel(failure)) return Promise.reject(failure);
  // A late response from a previous session must not log out a newly signed-in user.
  if (failure.response?.status === 401 && accessToken && failure.config?.auth !== false
      && failure.config?.headers?.Authorization === `Bearer ${accessToken}`) {
    accessToken = null;
    onSessionExpired();
  }
  return Promise.reject(normalizeError(failure));
});

export default axiosClient;
