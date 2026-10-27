import { afterEach, test } from 'node:test';
import assert from 'node:assert/strict';
import client, { normalizeError, setAccessToken, setSessionExpiredHandler } from '../src/api/axiosClient.js';
import { authApi } from '../src/api/authApi.js';
import { userApi } from '../src/api/userApi.js';
import { houseApi } from '../src/api/houseApi.js';

const adapter = client.defaults.adapter;
afterEach(() => { client.defaults.adapter = adapter; setAccessToken(null); });
function capture(check, data = {}) {
  client.defaults.adapter = async config => {
    check(config);
    return { status: 200, data, headers: {}, config };
  };
}

test('public login sends only credentials and accepts a plain JWT', async () => {
  capture(config => {
    assert.equal(config.url, '/public/login');
    assert.equal(config.headers.Authorization, undefined);
    assert.deepEqual(JSON.parse(config.data), { username: 'an', password: 'password' });
  }, 'jwt.token');
  assert.equal(await authApi.login({ username: 'an', password: 'password', role: 'ADMIN' }), 'jwt.token');
});

test('registration always sends MEMBER and excludes unsupported inputs', async () => {
  capture(config => {
    assert.equal(config.url, '/public/register');
    assert.deepEqual(JSON.parse(config.data), {
      username: 'an', password: 'password', fullName: 'An', cccd: '001234567890', role: 'MEMBER',
    });
  });
  await authApi.register({ username: 'an', password: 'password', fullName: 'An', cccd: '001234567890', role: 'ADMIN', email: 'x@y.com', confirmPassword: 'password', phone: '' });
});

test('house list and OR search are separate endpoints with server pagination', async () => {
  capture(config => { assert.equal(config.url, '/houses'); assert.deepEqual(config.params, { page: 2 }); });
  await houseApi.findAll(2);
  capture(config => {
    assert.equal(config.url, '/houses/search');
    assert.deepEqual(config.params, { city: 'Hà Nội', detail: '25 & 27', page: 1 });
  });
  await houseApi.search({ city: ' Hà Nội ', ward: ' ', detail: '25 & 27', status: 'FIRE' }, 1);
});

test('create and update house use exact DTO fields and bearer token', async () => {
  setAccessToken('test-token');
  const values = { houseName: 'Nhà A', city: 'Hà Nội', ward: 'Ba Đình', addressDetail: '12 A' };
  capture(config => {
    assert.equal(config.url, '/houses/createHouse');
    assert.equal(config.headers.Authorization, 'Bearer test-token');
    assert.deepEqual(JSON.parse(config.data), values);
  });
  await houseApi.create({ ...values, status: 'FIRE', id: 10 });
  capture(config => {
    assert.equal(config.url, '/houses/updateHouse/10');
    assert.equal(config.method, 'put');
    assert.deepEqual(JSON.parse(config.data), { ...values, status: 'FIRE' });
  });
  await houseApi.update(10, { ...values, status: 'FIRE', id: 10 });
});

test('owner member management uses the current house endpoints', async () => {
  capture(config => { assert.equal(config.url, '/houses/me/people'); assert.equal(config.method, 'get'); });
  await houseApi.getMembers();
  capture(config => { assert.equal(config.url, '/houses/me/people'); assert.deepEqual(JSON.parse(config.data), { userId: 8 }); });
  await houseApi.addPeople({ userId: 8 });
  capture(config => { assert.equal(config.url, '/houses/me/people'); assert.equal(config.method, 'delete'); assert.deepEqual(JSON.parse(config.data), { userId: 8 }); });
  await houseApi.removePeople({ userId: 8 });
});

test('user search only sends supported fields; profile cannot change role or username', async () => {
  capture(config => {
    assert.equal(config.url, '/users');
    assert.deepEqual(config.params, { fullName: 'An', page: 0 });
  });
  await userApi.search({ fullName: 'An', role: 'ADMIN' });
  capture(config => {
    assert.equal(config.url, '/users/me');
    assert.deepEqual(JSON.parse(config.data), { fullName: 'An', phone: null, cccd: '001234567890' });
  });
  await userApi.updateMe({ fullName: 'An', phone: '', cccd: '001234567890', role: 'ADMIN', username: 'other' });
});

test('validation maps, HTTP statuses and network errors are normalized', () => {
  const error = normalizeError({ response: { status: 400, data: { cccd: 'Invalid CCCD' } } });
  assert.equal(error.fields.cccd, 'Invalid CCCD');
  for (const status of [401, 403, 404, 409, 500]) {
    assert.equal(normalizeError({ response: { status } }).status, status);
  }
  assert.match(normalizeError({}).message, /kết nối/);
});

test('protected 401 clears the current session; a stale 401 does not', async () => {
  let expirations = 0;
  const unsubscribe = setSessionExpiredHandler(() => { expirations++; });
  setAccessToken('new');
  client.defaults.adapter = async config => Promise.reject({ response: { status: 401 }, config });
  await assert.rejects(client.get('/houses', { headers: { Authorization: 'Bearer old' } }));
  assert.equal(expirations, 0);
  await assert.rejects(client.get('/houses'));
  assert.equal(expirations, 1);
  unsubscribe();
});
