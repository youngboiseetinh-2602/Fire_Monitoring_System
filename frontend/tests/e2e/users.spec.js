import { test, expect } from '@playwright/test';
import { mockBackend, signIn } from './helpers';

test('admin searches users, views detail and updates own profile', async ({ page }) => {
  const state = await mockBackend(page);
  await signIn(page, '/app/users');
  await expect(page.getByRole('table')).toContainText('Nguyen An');
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('draft');
  await page.getByRole('button', { name: 'Xóa bộ lọc' }).click();
  await expect(page.getByLabel('Tên đăng nhập', { exact: true })).toHaveValue('');
  await page.getByLabel('Họ và tên', { exact: true }).fill('Nguyen');
  await page.getByRole('button', { name: /Tìm kiếm/ }).click();
  await expect.poll(() => state.calls.some(call => call.path === '/users' && call.params.fullName === 'Nguyen')).toBe(true);
  await page.getByRole('button', { name: 'Xem Nguyen An' }).click();
  await expect(page.getByRole('heading', { name: 'Chi tiết người dùng' })).toBeVisible();
  await expect(page.getByText('001234567890')).toBeVisible();
  await page.getByRole('menuitem', { name: /Hồ sơ cá nhân/ }).click();
  await page.getByRole('button', { name: /Chỉnh sửa hồ sơ/ }).click();
  await page.getByLabel('Họ và tên', { exact: true }).fill('Nguyen Binh');
  await page.getByRole('button', { name: /Lưu thay đổi/ }).click();
  await expect(page).toHaveURL(/\/app\/profile$/);
  await expect(page.getByRole('heading', { name: 'Nguyen Binh' })).toBeVisible();
  expect(state.calls.find(call => call.path === '/users/me' && call.method === 'PUT').body)
    .toEqual({ fullName: 'Nguyen Binh', phone: '0912345678', cccd: '001234567890' });
});

test('member cannot open the administrator user list', async ({ page }) => {
  const state = await mockBackend(page, 'MEMBER');
  await signIn(page, '/app/users');
  await expect(page.getByText('Không có quyền truy cập')).toBeVisible();
  expect(state.calls.filter(call => call.path === '/users')).toHaveLength(0);
  await expect(page.getByRole('menuitem', { name: /Người dùng/ })).toHaveCount(0);
});

test('expired session returns to login without exposing stale profile', async ({ page }) => {
  const state = await mockBackend(page);
  await signIn(page, '/app/profile');
  await expect(page.getByRole('heading', { name: 'Nguyen An' })).toBeVisible();
  state.expired = true;
  await page.getByRole('button', { name: /Chỉnh sửa hồ sơ/ }).click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByText('Phiên đã hết hạn. Vui lòng đăng nhập lại.')).toBeVisible();
});
