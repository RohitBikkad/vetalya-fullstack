/**
 * Vetalya API Client Service
 * Interacts with Spring Boot backend at http://localhost:8080 (or through Vite /api proxy)
 */

const getApiBaseUrl = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (envUrl && envUrl.startsWith('http')) {
    return envUrl;
  }
  if (typeof window !== 'undefined' && window.location.hostname.includes('onrender.com')) {
    return 'https://vetalaya-v2-1.onrender.com/api';
  }
  return envUrl || 'https://vetalaya-v2-1.onrender.com/api';
};

const API_BASE_URL = getApiBaseUrl();

/**
 * Universal request wrapper handling JWT injection and JSON parsing
 */
export async function request(endpoint, options = {}) {
  const token = localStorage.getItem('vetalya_jwt_token');

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const config = {
    credentials: 'include',
    ...options,
    headers,
  };

  const url = `${API_BASE_URL}${endpoint}`;
  const response = await fetch(url, config);

  if (response.status === 401) {
    // If token is invalid or expired, clear local storage
    if (endpoint !== '/auth/login' && endpoint !== '/auth/google') {
      localStorage.removeItem('vetalya_jwt_token');
      localStorage.removeItem('vetalya_auth_user');
    }
  }

  const data = await response.json().catch(() => null);

  if (!response.ok || !data) {
    const errorMessage =
      data?.message ||
      (data?.details ? Object.values(data.details).join(', ') : null) ||
      (!data ? `Failed to connect to backend at ${API_BASE_URL} (Status ${response.status}).` : 'Request failed');
    throw new Error(errorMessage);
  }

  return data;
}

export async function logoutApi() {
  try {
    await request('/auth/logout', { method: 'POST' });
  } catch (e) {
    console.warn('Backend logout failed or not available:', e);
  }
}

export async function exchangeCodeApi(code) {
  return request('/auth/exchange', {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}

export async function loginApi(email, password, rememberMe = true) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password, rememberMe }),
  });
}

export async function registerApi(data, emailArg, passwordArg, roleArg) {
  let body;
  if (typeof data === 'object' && data !== null) {
    body = data;
  } else {
    body = {
      fullName: data,
      email: emailArg,
      password: passwordArg,
      role: roleArg || 'ROLE_PET_OWNER',
    };
  }
  return request('/auth/register', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function googleAuthApi(idToken, role = 'PET_PARENT') {
  return request('/auth/google', {
    method: 'POST',
    body: JSON.stringify({ idToken, role }),
  });
}

export async function demoLoginApi(role = 'PET_PARENT') {
  return request('/auth/demo-login', {
    method: 'POST',
    body: JSON.stringify({ role }),
  });
}

export async function getCurrentUserApi() {
  return request('/auth/me', {
    method: 'GET',
  });
}
