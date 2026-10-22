import client from './axiosClient.js';

export const kitApi = {
  sendCommand: (kitId, { buzz, pump, led }) => client.put(
    `/kits/${encodeURIComponent(kitId)}/command`,
    { buzz: Boolean(buzz), pump: Boolean(pump), led: Boolean(led) },
  ),
};
