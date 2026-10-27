import client from './axiosClient.js';

// These endpoints manage members of the current owner's house.
export const houseMemberApi = {
  getMembers: (options = {}) => client.get('/houses/me/people', options),
  addMember: userId => client.post('/houses/me/people', { userId }),
  removeMember: userId => client.delete('/houses/me/people', { data: { userId } }),
};
