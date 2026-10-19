import { test, expect } from '@playwright/test';
import { mockBackend, signIn } from './helpers';

test('house list paginates, searches OR fields and resets to findAll', async ({ page }) => {
  const state = await mockBackend(page);
  await signIn(page);
  await expect(page.getByRole('table')).toContainText('Nhà 1');
  await page.locator('.ant-pagination-item-2').click();
  await expect(page.getByRole('table')).toContainText('Nhà 21');
  expect(state.calls.at(-1).params.page).toBe('1');
  await page.getByLabel('Thành phố / Tỉnh', { exact: true }).fill(' Hà Nội ');
  await page.getByLabel('Phường / Xã', { exact: true }).fill('Ba Đình');
  await page.getByLabel('Địa chỉ chi tiết', { exact: true }).fill('Nguyễn');
  await page.getByRole('button', { name: 'Tìm kiếm nhà', exact: true }).click();
  await expect.poll(() => state.calls.at(-1).path).toBe('/houses/search');
  expect(state.calls.at(-1).params).toEqual({ city: 'Hà Nội', ward: 'Ba Đình', detail: 'Nguyễn', page: '0' });
  await page.getByRole('button', { name: 'Xóa bộ lọc' }).click();
  await expect.poll(() => state.calls.at(-1).path).toBe('/houses');
  await expect(page.getByLabel('Thành phố / Tỉnh', { exact: true })).toHaveValue('');
});

test('create and edit house only use implemented APIs', async ({ page }) => {
  const state = await mockBackend(page);
  await signIn(page);
  await page.getByRole('button', { name: /Thêm ngôi nhà/ }).click();
  await page.getByLabel('Tên ngôi nhà', { exact: true }).fill('Nhà mới');
  await page.getByLabel('Thành phố / Tỉnh', { exact: true }).fill('Hà Nội');
  await page.getByLabel('Phường / Xã', { exact: true }).fill('Ba Đình');
  await page.getByLabel('Địa chỉ chi tiết', { exact: true }).fill('25 Nguyễn Trãi');
  await page.getByRole('button', { name: /Tạo ngôi nhà/ }).click();
  await expect(page).toHaveURL(/\/app\/houses$/);
  expect(state.calls.find(call => call.path === '/houses/createHouse').body).toEqual({ houseName: 'Nhà mới', city: 'Hà Nội', ward: 'Ba Đình', addressDetail: '25 Nguyễn Trãi' });
  await page.getByRole('button', { name: 'Sửa Nhà mới', exact: true }).click();
  await expect(page.getByLabel('Tên ngôi nhà', { exact: true })).toHaveValue('Nhà mới');
  await page.getByLabel('Tên ngôi nhà', { exact: true }).fill('Nhà đã sửa');
  await page.getByLabel('Trạng thái', { exact: true }).focus();
  await page.getByLabel('Trạng thái', { exact: true }).press('ArrowDown');
  await page.getByText('Cháy (FIRE)', { exact: true }).click();
  await page.getByRole('button', { name: /Lưu thay đổi/ }).click();
  await expect(page.getByRole('dialog', { name: 'Xác nhận thay đổi trạng thái' })).toBeVisible();
  await page.getByRole('button', { name: 'Xác nhận', exact: true }).click();
  await expect(page).toHaveURL(/\/app\/houses$/);
  expect(state.calls.find(call => call.method === 'PUT' && call.path.startsWith('/houses/')).body.status).toBe('FIRE');
  expect(state.calls.some(call => /^\/houses\/\d+$/.test(call.path))).toBe(false);
});

test('edit deep link without a list record gives a recovery action', async ({ page }) => {
  const state = await mockBackend(page);
  await signIn(page, '/app/houses/1/edit');
  await expect(page.getByText('Chọn ngôi nhà cần chỉnh sửa')).toBeVisible();
  expect(state.calls.some(call => call.path === '/houses/1')).toBe(false);
});

test('server error can retry into a truthful empty state', async ({ page }) => {
  const state = await mockBackend(page);
  state.houseError = 500;
  await signIn(page);
  await expect(page.getByRole('alert')).toContainText('Máy chủ gặp lỗi');
  state.houseError = 0; state.empty = true;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Chưa có ngôi nhà nào')).toBeVisible();
});

for (const [name, width, height] of [['desktop', 1440, 1000], ['tablet', 900, 1100], ['mobile', 375, 812]]) {
  test(`responsive ${name}: table stays inside viewport and navigation works`, async ({ page }, testInfo) => {
    await page.setViewportSize({ width, height });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await mockBackend(page);
    await signIn(page);
    await expect(page.getByRole('table')).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    if (name === 'mobile') {
      await page.getByRole('button', { name: 'Mở menu' }).click();
      await expect(page.getByRole('dialog')).toBeVisible();
      await page.getByRole('menuitem', { name: /Hồ sơ cá nhân/ }).click();
      await expect(page.getByRole('heading', { name: 'Hồ sơ của tôi' })).toBeVisible();
      await expect(page.getByRole('dialog')).toHaveCount(0);
    }
    await page.screenshot({ path: testInfo.outputPath(`${name}.png`), fullPage: true });
    expect(errors).toEqual([]);
  });
}
