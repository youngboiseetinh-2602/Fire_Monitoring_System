import client from './axiosClient.js';
import { cleanFilters } from './query.js';

function housePayload({ houseName, city, ward, addressDetail }) {
  return { houseName, city, ward, addressDetail };
}

export const houseApi = {
  getMembers: (options = {}) => client.get('/houses/me/people', options),
  addPeople: ({ userId }) => client.post('/houses/me/people', { userId }),
  removePeople: ({ userId }) => client.delete('/houses/me/people', { data: { userId } }),
  findAll: (page = 0, signal) => client.get('/houses', { params: { page }, signal }),
  search: (filters = {}, page = 0, signal) => client.get('/houses/search', {
    params: { ...cleanFilters(filters, ['city', 'ward', 'detail']), page }, signal,
  }),
  create: values => client.post('/houses/createHouse', housePayload(values)),
  update: (id, values) => client.put(`/houses/updateHouse/${encodeURIComponent(id)}`, {
    ...housePayload(values), status: values.status,
  }),
};
