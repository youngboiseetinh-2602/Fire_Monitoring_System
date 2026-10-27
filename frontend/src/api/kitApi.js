import client from './axiosClient.js';

export const kitApi = {
  addToHouse: (houseId, values) => client.post(`/houses/${encodeURIComponent(houseId)}/kits`, values),
  startHouseTelemetry: houseId => client.post(`/houses/${encodeURIComponent(houseId)}/telemetry/start`),
  startMyHouseTelemetry: () => client.post('/me/houses/telemetry/start'),
  sendCommand: (kitId, { buzz, pump, led }) => client.put(
    `/kits/${encodeURIComponent(kitId)}/command`,
    { buzz: Boolean(buzz), pump: Boolean(pump), led: Boolean(led) },
  ),
};
