import client from './axiosClient.js';

export const authApi = {
  login: ({ username, password }) => client.post('/public/login', { username, password }, { auth: false, responseType: 'text' }),
  register: ({ username, password, fullName, phone, cccd }) => client.post('/public/register', {
    username, password, fullName, cccd, role: 'MEMBER', ...(phone ? { phone } : {}),
  }, { auth: false }),
};
