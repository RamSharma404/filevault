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
    const msg = data?.error || data?.message || `Request failed with status ${res.status}`;
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
  upload: (file, folderId, onProgress, abortRef) => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      if (abortRef) abortRef.abort = () => xhr.abort();

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable && onProgress) {
          onProgress(Math.round((e.loaded / e.total) * 100), e.loaded, e.total);
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve(JSON.parse(xhr.responseText));
        } else {
          try {
            const err = JSON.parse(xhr.responseText);
            reject(new Error(err.error || err.message || `Upload failed with status ${xhr.status}`));
          } catch {
            reject(new Error(`Upload failed with status ${xhr.status}`));
          }
        }
      });

      xhr.addEventListener('error', () => reject(new Error('Network error during upload')));
      xhr.addEventListener('abort', () => reject(new Error('__CANCELLED__')));

      const form = new FormData();
      form.append('file', file);
      if (folderId) form.append('folderId', folderId);

      xhr.open('POST', `${API_BASE}/files/upload`);
      const token = getToken();
      if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      xhr.send(form);
    });
  },
  list: (folderId) => {
    const params = folderId ? `?folderId=${folderId}` : '';
    return request(`/files${params}`);
  },
  download: (id) => request(`/files/${id}/download`),
  delete: (id) => request(`/files/${id}`, { method: 'DELETE' }),
  storageInfo: () => request('/files/storage-info'),
};

export const folders = {
  create: (name, parentId) =>
    request('/folders', {
      method: 'POST',
      body: JSON.stringify({ name, parentId: parentId || null }),
    }),
  list: (parentId) => {
    const params = parentId ? `?parentId=${parentId}` : '';
    return request(`/folders${params}`);
  },
  breadcrumbs: (id) => request(`/folders/${id}/breadcrumbs`),
  delete: (id) => request(`/folders/${id}`, { method: 'DELETE' }),
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
