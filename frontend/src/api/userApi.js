import client from './axiosClient.js';
import { cleanFilters } from './query.js';

export const userApi = {
  search: (filters = {}, page = 0, signal) => client.get('/users', {
    params: { ...cleanFilters(filters, ['username', 'fullName', 'cccd']), page }, signal,
  }),
  getInfo: (id, signal) => client.get(`/users/${encodeURIComponent(id)}`, { signal }),
  getMe: (options = {}) => client.get('/users/me', options),
  updateMe: ({ fullName, phone, cccd }) => client.put('/users/me', { fullName, phone: phone || null, cccd }),
};
