import { useState, useEffect } from 'react';
import { trash } from '../api';
import { toast } from './Toast';

function formatSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function daysRemaining(deletedAt) {
  if (!deletedAt) return null;
  const deleted = new Date(deletedAt);
  const expiresAt = new Date(deleted.getTime() + 30 * 24 * 60 * 60 * 1000);
  const now = new Date();
  const diff = Math.ceil((expiresAt - now) / (1000 * 60 * 60 * 24));
  return Math.max(0, diff);
}

function formatDeletedDate(dateStr) {
  const d = new Date(dateStr);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

export default function TrashView({ onClose }) {
  const [trashedFiles, setTrashedFiles] = useState([]);
  const [trashedFolders, setTrashedFolders] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadTrash = async () => {
    setLoading(true);
    try {
      const [files, folders] = await Promise.all([
        trash.listFiles(),
        trash.listFolders(),
      ]);
      setTrashedFiles(files || []);
      setTrashedFolders(folders || []);
    } catch (err) {
      toast('Failed to load trash', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadTrash(); }, []);

  const handleRestoreFile = async (id) => {
    try {
      await trash.restoreFile(id);
      setTrashedFiles((prev) => prev.filter((f) => f.id !== id));
      toast('File restored');
    } catch (err) { toast(err.message, 'error'); }
  };

  const handleRestoreFolder = async (id) => {
    try {
      await trash.restoreFolder(id);
      setTrashedFolders((prev) => prev.filter((f) => f.id !== id));
      toast('Folder and contents restored');
    } catch (err) { toast(err.message, 'error'); }
  };

  const handlePermanentDeleteFile = async (id) => {
    try {
      await trash.permanentlyDeleteFile(id);
      setTrashedFiles((prev) => prev.filter((f) => f.id !== id));
      toast('File permanently deleted');
    } catch (err) { toast(err.message, 'error'); }
  };

  const handlePermanentDeleteFolder = async (id) => {
    try {
      await trash.permanentlyDeleteFolder(id);
      setTrashedFolders((prev) => prev.filter((f) => f.id !== id));
      toast('Folder permanently deleted');
    } catch (err) { toast(err.message, 'error'); }
  };

  const totalItems = trashedFiles.length + trashedFolders.length;

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="trash-modal" onClick={(e) => e.stopPropagation()}>
        <div className="trash-header">
          <div>
            <h3>🗑️ Trash</h3>
            <p className="trash-subtitle">Items are automatically deleted after 30 days</p>
          </div>
          <button className="btn btn-icon" onClick={onClose} title="Close">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <div className="trash-body">
          {loading ? (
            <div className="loading-state">
              <div className="spinner" />
              <p>Loading trash...</p>
            </div>
          ) : totalItems === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
              </div>
              <h3>Trash is empty</h3>
              <p>Deleted files and folders will appear here</p>
            </div>
          ) : (
            <div className="trash-list">
              {trashedFolders.map((folder) => {
                const days = daysRemaining(folder.deletedAt);
                return (
                  <div key={`folder-${folder.id}`} className="trash-item">
                    <div className="trash-item-icon">📁</div>
                    <div className="trash-item-info">
                      <span className="trash-item-name">{folder.name}</span>
                      <div className="trash-item-meta">
                        <span>Folder</span>
                        <span className="meta-sep">&middot;</span>
                        <span>Deleted {formatDeletedDate(folder.deletedAt)}</span>
                        <span className="meta-sep">&middot;</span>
                        <span className={`trash-countdown ${days <= 5 ? 'urgent' : ''}`}>
                          {days === 0 ? 'Expiring today' : `Deletes in ${days} day${days !== 1 ? 's' : ''}`}
                        </span>
                      </div>
                    </div>
                    <div className="trash-item-actions">
                      <button className="btn btn-sm btn-secondary" onClick={() => handleRestoreFolder(folder.id)}>
                        Restore
                      </button>
                      <button className="btn btn-sm btn-danger" onClick={() => handlePermanentDeleteFolder(folder.id)}>
                        Delete
                      </button>
                    </div>
                  </div>
                );
              })}
              {trashedFiles.map((file) => {
                const days = daysRemaining(file.deletedAt);
                return (
                  <div key={`file-${file.id}`} className="trash-item">
                    <div className="trash-item-icon">📄</div>
                    <div className="trash-item-info">
                      <span className="trash-item-name">{file.originalFilename}</span>
                      <div className="trash-item-meta">
                        <span>{formatSize(file.size)}</span>
                        <span className="meta-sep">&middot;</span>
                        <span>Deleted {formatDeletedDate(file.deletedAt)}</span>
                        <span className="meta-sep">&middot;</span>
                        <span className={`trash-countdown ${days <= 5 ? 'urgent' : ''}`}>
                          {days === 0 ? 'Expiring today' : `Deletes in ${days} day${days !== 1 ? 's' : ''}`}
                        </span>
                      </div>
                    </div>
                    <div className="trash-item-actions">
                      <button className="btn btn-sm btn-secondary" onClick={() => handleRestoreFile(file.id)}>
                        Restore
                      </button>
                      <button className="btn btn-sm btn-danger" onClick={() => handlePermanentDeleteFile(file.id)}>
                        Delete
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
