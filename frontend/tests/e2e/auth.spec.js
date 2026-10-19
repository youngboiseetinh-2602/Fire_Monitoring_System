import { test, expect } from '@playwright/test';

test('login shows server failure without navigation', async ({ page }) => {
  await page.route('**/public/login', route => route.fulfill({ status: 401, body: '' }));
  await page.goto('/login');
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('an');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('wrong');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('không đúng');
  await expect(page).toHaveURL(/\/login$/);
});

test('registration matches DTO and validates confirmation', async ({ page }) => {
  let sent;
  await page.route('**/public/register', async route => {
    sent = route.request().postDataJSON();
    await route.fulfill({ status: 200, body: 'register successfully' });
  });
  await page.goto('/register');
  await page.getByLabel('Họ và tên', { exact: true }).fill('Nguyen An');
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('an');
  await page.getByLabel('CCCD', { exact: true }).fill('001234567890');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('password123');
  await page.getByLabel('Xác nhận mật khẩu', { exact: true }).fill('different');
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.getByText('Mật khẩu xác nhận chưa khớp.')).toBeVisible();
  expect(sent).toBeUndefined();
  await page.getByLabel('Xác nhận mật khẩu', { exact: true }).fill('password123');
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  expect(sent).toEqual({ fullName: 'Nguyen An', username: 'an', password: 'password123', cccd: '001234567890', role: 'MEMBER' });
});

test('public form fits mobile with reduced motion', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 });
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto('/register');
  await expect(page.getByRole('heading', { name: 'Tạo tài khoản' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  const animation = await page.locator('.page-enter').evaluate(el => getComputedStyle(el).animationName);
  expect(animation).toBe('none');
});
