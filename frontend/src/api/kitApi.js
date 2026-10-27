import client from './axiosClient.js';

export const kitApi = {
  addToHouse: (houseId, values) => client.post(`/houses/${encodeURIComponent(houseId)}/kits`, values),
  getHouseKits: houseId => client.post(`/house/${encodeURIComponent(houseId)}/kit`),
  getMyHouseKits: () => client.post('/me/house/kit'),
  sendCommand: (kitId, { buzz, pump, led }) => client.put(
    `/kits/${encodeURIComponent(kitId)}/command`,
    { buzz: Boolean(buzz), pump: Boolean(pump), led: Boolean(led) },
  ),
};
