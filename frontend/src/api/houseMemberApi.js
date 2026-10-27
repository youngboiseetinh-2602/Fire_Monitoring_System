import client from './axiosClient.js';

// These endpoints manage members of the current owner's house; no house ID or list endpoint exists yet.
export const houseMemberApi = {
  addMember: userId => client.post('/houses/me/people', { userId }),
  removeMember: userId => client.delete('/houses/me/people', { data: { userId } }),
};
