export const required = label => ({ required: true, whitespace: true, message: `Vui lòng nhập ${label.toLowerCase()}.` });
export const maxLength = max => ({ max, message: `Tối đa ${max} ký tự.` });
export const phoneRule = { pattern: /^0[0-9]{9}$/, message: 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.' };
export const cccdRule = { pattern: /^[0-9]{12}$/, message: 'CCCD phải gồm đúng 12 chữ số.' };
export function applyFieldErrors(form, error) {
  form.setFields(Object.entries(error.fields || {}).map(([name, message]) => ({ name, errors: [message] })));
}
