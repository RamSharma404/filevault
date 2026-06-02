const API_BASE = '';

function getToken() {
  return localStorage.getItem('token');
}

async function request(endpoint, options = {}) {
  const token = getToken();
  const headers = { ...options.headers };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const res = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
  const data = res.headers.get('content-type')?.includes('application/json')
    ? await res.json()
    : null;

  if (!res.ok) {
    const msg = data?.error || `Request failed with status ${res.status}`;
    throw new Error(msg);
  }
  return data;
}

export const auth = {
  register: (email, password) =>
    request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),
  login: (email, password) =>
    request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),
};

export const files = {
  upload: (file, onProgress) => {
    const form = new FormData();
    form.append('file', file);
    return request('/files/upload', {
      method: 'POST',
      body: form,
    });
  },
  list: () => request('/files'),
  download: (id) => request(`/files/${id}/download`),
  delete: (id) =>
    request(`/files/${id}`, { method: 'DELETE' }),
};

export function setToken(token) {
  localStorage.setItem('token', token);
}

export function clearToken() {
  localStorage.removeItem('token');
}

export function isAuthenticated() {
  return !!getToken();
}
