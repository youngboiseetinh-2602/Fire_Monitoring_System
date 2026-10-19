export function cleanFilters(filters, allowed) {
  return Object.fromEntries(allowed.flatMap(key => {
    const value = typeof filters[key] === 'string' ? filters[key].trim() : '';
    return value ? [[key, value]] : [];
  }));
}
