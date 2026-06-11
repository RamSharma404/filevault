import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { files } from '../api';

function formatSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

export default function Header({ storageRefresh }) {
  const { user, logout } = useAuth();
  const [storage, setStorage] = useState({ used: 0, total: 1073741824 });

  useEffect(() => {
    files.storageInfo().then(setStorage).catch(() => {});
  }, [storageRefresh]);

  const pct = Math.min(100, Math.round((storage.used / storage.total) * 100));
  const barClass = pct > 90 ? 'danger' : pct > 70 ? 'warning' : '';

  return (
    <header className="header">
      <div className="header-left">
        <div className="logo-icon-sm">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
        </div>
        <h2 className="header-title">FileVault</h2>
      </div>
      <div className="header-right">
        <div className="storage-bar-container">
          <div className="storage-bar">
            <div className={`storage-bar-fill ${barClass}`} style={{ width: `${pct}%` }} />
          </div>
          <span className="storage-text">{formatSize(storage.used)} / {formatSize(storage.total)}</span>
        </div>
        <div className="user-badge">
          <div className="user-avatar">{user?.email?.charAt(0).toUpperCase()}</div>
          <span className="user-email">{user?.email}</span>
        </div>
        <button className="btn btn-ghost" onClick={logout} title="Sign out">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        </button>
      </div>
    </header>
  );
}
