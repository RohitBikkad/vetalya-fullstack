/** Shared transport for the existing Spring Boot API. No React, DOM or storage dependency. */
export function createApiClient({ baseUrl, platform, tokenStore, fetchImpl = globalThis.fetch, production = false }) {
  if (!['web', 'android', 'ios'].includes(platform)) throw new Error('Unsupported client platform');
  if (!tokenStore || !fetchImpl) throw new Error('Token store and fetch implementation are required');
  const base = String(baseUrl || '').replace(/\/+$/, '');
  if (base !== '/api') {
    let url;
    try { url = new URL(base); } catch { throw new Error('API base URL must be an absolute URL ending in /api'); }
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash || url.pathname !== '/api')
      throw new Error('API base URL must be an http(s) origin followed by /api');
    if (production && url.protocol !== 'https:') throw new Error('Production API requires HTTPS');
  } else if (platform !== 'web') throw new Error('Native apps require an absolute API URL');
  const publicAuthPaths = new Set(['/auth/login', '/auth/register', '/auth/google', '/auth/apple', '/auth/refresh', '/auth/exchange', '/auth/logout', '/auth/login-otp', '/auth/otp/send', '/auth/otp/verify', '/auth/demo-login']);
  let refreshPromise;
  let sessionVersion = 0;
  let storageUpdates = Promise.resolve();
  function updateStorage(action) {
    const next = storageUpdates.then(action, action);
    storageUpdates = next.catch(() => {});
    return next;
  }
  async function send(path, { body, headers, ...options } = {}) {
    if (typeof path !== 'string' || !path.startsWith('/') || path.startsWith('//') || path.includes('..') || path.includes('\\'))
      throw new Error('Use an API-relative path such as /owner/pets');
    const accessToken = publicAuthPaths.has(path.split('?')[0]) ? undefined : await tokenStore.getAccessToken();
    const response = await fetchImpl(base + path, {
      ...options,
      credentials: platform === 'web' ? 'include' : 'omit',
      headers: { 'Content-Type': 'application/json', ...headers, ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}) },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });
    const text = await response.text();
    let data;
    try { data = text ? JSON.parse(text) : null; } catch { throw new Error(`Invalid API response (${response.status})`); }
    if (!response.ok) {
      const error = new Error(data?.detail || data?.message || `API request failed (${response.status})`);
      error.status = response.status;
      throw error;
    }
    return data;
  }
  async function acceptTokens(data, version) {
    const accessToken = data?.accessToken || data?.token;
    if (!accessToken || (platform !== 'web' && !data?.refreshToken)) throw new Error('Incomplete authentication response');
    await updateStorage(async () => {
      if (version !== sessionVersion) throw new Error('Session changed while authentication was pending');
      await tokenStore.saveTokens({ accessToken, ...(platform !== 'web' ? { refreshToken: data.refreshToken } : {}) });
    });
    return data;
  }
  async function authenticate(path, data) {
    const version = ++sessionVersion;
    const result = await send(path, { method: 'POST', body: { ...data, clientType: platform } });
    return acceptTokens(result, version);
  }
  function refreshSession() {
    if (!refreshPromise) {
      const version = sessionVersion;
      refreshPromise = (async () => {
        try {
          const refreshToken = platform === 'web' ? undefined : await tokenStore.getRefreshToken();
          if (platform !== 'web' && !refreshToken) throw new Error('Sign in is required');
          const result = await send('/auth/refresh', { method: 'POST', ...(refreshToken ? { body: { refreshToken } } : {}) });
          return await acceptTokens(result, version);
        } catch (error) {
          if (error.status === 401 || error.status === 403) await updateStorage(() => version === sessionVersion ? tokenStore.clear() : undefined);
          throw error;
        } finally { refreshPromise = undefined; }
      })();
    }
    return refreshPromise;
  }
  async function request(path, options) {
    try { return await send(path, options); }
    catch (error) {
      if (error.status !== 401 || path.startsWith('/auth/')) throw error;
      await refreshSession();
      return send(path, options);
    }
  }
  async function logout() {
    ++sessionVersion;
    const refreshToken = platform === 'web' ? undefined : await tokenStore.getRefreshToken();
    const clearing = updateStorage(() => tokenStore.clear());
    try {
      return await send('/auth/logout', { method: 'POST', ...(refreshToken ? { body: { refreshToken } } : {}) });
    } finally { await clearing; }
  }

  return {
    request, refreshSession, logout,
    login: data => authenticate('/auth/login', data),
    register: data => authenticate('/auth/register', { ...data, role: 'ROLE_PET_OWNER' }),
    loginWithOtp: data => authenticate('/auth/login-otp', data),
    google: data => authenticate('/auth/google', { ...data, role: 'ROLE_PET_OWNER' }),
    apple: data => authenticate('/auth/apple', { ...data, role: 'ROLE_PET_OWNER' }),
    exchangeCode: data => authenticate('/auth/exchange', data),
  };
}
