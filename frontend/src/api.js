const baseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

export async function authenticate(mode, values) {
  let response;
  try {
    response = await fetch(`${baseUrl}/public/${mode}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(values),
      signal: AbortSignal.timeout(15000),
    });
  } catch {
    throw new Error('Unable to reach the server. Please try again shortly.');
  }
  const text = await response.text();
  if (!response.ok) {
    let problem;
    try { problem = JSON.parse(text); } catch { /* Non-JSON proxy or security error. */ }
    const error = new Error(
      response.status === 401 ? 'Incorrect username or password.'
        : response.status === 409 ? 'This account information is already in use.'
          : response.status >= 500 ? 'The server is unavailable. Please try again shortly.'
            : problem?.detail || 'We could not complete your request. Please try again.',
    );
    error.fields = problem?.validationErrors || {};
    throw error;
  }
  return text;
}
