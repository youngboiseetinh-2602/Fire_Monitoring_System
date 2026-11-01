import client from './axiosClient.js';
import { cleanFilters } from './query.js';

function housePayload({ houseName, city, ward, addressDetail }) {
  return { houseName, city, ward, addressDetail };
}

export const houseApi = {
  getMembers: (houseId, options = {}) => client.get(`/houses/${encodeURIComponent(houseId)}/people`, options),
  addPeople: (houseId, { userId }) => client.post(`/houses/${encodeURIComponent(houseId)}/people`, { userId }),
  removePeople: (houseId, { userId }) => client.delete(`/houses/${encodeURIComponent(houseId)}/people`, { data: { userId } }),
  findAll: (page = 0, signal) => client.get('/houses', { params: { page }, signal }),
  search: (filters = {}, page = 0, signal) => client.get('/houses/search', {
    params: { ...cleanFilters(filters, ['city', 'ward', 'detail']), page }, signal,
  }),
  create: values => client.post('/houses/createHouse', housePayload(values)),
  update: (id, values) => client.put(`/houses/updateHouse/${encodeURIComponent(id)}`, {
    ...housePayload(values), status: values.status,
  }),
};
