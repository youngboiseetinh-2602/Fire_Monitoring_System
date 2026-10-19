import { expect } from '@playwright/test';

export async function mockBackend(page, role = 'ADMIN') {
  const state = {
    user: { id: 1, username: 'admin', fullName: 'Nguyen An', phone: '0912345678', cccd: '001234567890', role },
    houses: Array.from({ length: 21 }, (_, i) => ({ id: i + 1, houseName: `Nhà ${i + 1}`, city: 'Hà Nội', ward: 'Ba Đình', addressDetail: `${i + 1} Nguyễn Trãi`, status: i === 0 ? 'FIRE' : 'NORMAL' })),
    calls: [], expired: false, houseError: 0, empty: false,
  };
  await page.route('**/*', async route => {
    const url = new URL(route.request().url());
    if (!/^\/(public|users|houses)(\/|\?|$)/.test(url.pathname)) return route.continue();
    const method = route.request().method();
    const body = route.request().postDataJSON();
    state.calls.push({ path: url.pathname, params: Object.fromEntries(url.searchParams), method, body, auth: route.request().headers().authorization });
    const json = (data, status = 200) => route.fulfill({ status, json: data });
    if (url.pathname === '/public/login') return route.fulfill({ status: 200, body: 'fake.jwt.token' });
    if (state.expired) return json({}, 401);
    if (url.pathname === '/users/me') {
      if (method === 'PUT') { state.user = { ...state.user, ...body }; return route.fulfill({ body: 'update sucessfull' }); }
      return json(state.user);
    }
    if (url.pathname === '/users') return json({ content: [state.user], size: 20, totalElements: 1, totalPages: 1 });
    if (url.pathname === '/users/1') return json(state.user);
    if (url.pathname === '/houses/createHouse') {
      const house = { ...body, status: 'NORMAL', id: state.houses.length + 1 };
      state.houses.unshift(house);
      return json(house, 201);
    }
    if (url.pathname.startsWith('/houses/updateHouse/')) {
      const id = Number(url.pathname.split('/').pop());
      const house = { id, ...body };
      state.houses = state.houses.map(item => item.id === id ? house : item);
      return json(house);
    }
    if (url.pathname === '/houses' || url.pathname === '/houses/search') {
      if (state.houseError) return json({}, state.houseError);
      const pageNumber = Number(url.searchParams.get('page') || 0);
      const houses = state.empty ? [] : state.houses;
      return json({ content: houses.slice(pageNumber * 20, (pageNumber + 1) * 20), size: 20, totalElements: houses.length, totalPages: Math.ceil(houses.length / 20) });
    }
    return json({}, 404);
  });
  return state;
}

export async function signIn(page, path = '/app/houses') {
  await page.goto(path);
  await expect(page).toHaveURL(/\/login$/);
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('admin');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('password123');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(new RegExp(path.replace(/[?]/g, '\\?') + '$'));
}
