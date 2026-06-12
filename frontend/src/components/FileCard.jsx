import { useState } from 'react';
import { files } from '../api';
import { toast } from './Toast';

function formatSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatDate(dateStr) {
  const d = new Date(dateStr);
  return d.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function FileIcon({ contentType, thumbnailUrl, altText }) {
  if (thumbnailUrl) {
    return (
      <div className="file-type-icon" style={{ padding: 0, overflow: 'hidden' }}>
        <img src={thumbnailUrl} alt={altText} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
      </div>
    );
  }

  if (contentType?.startsWith('image/')) {
    return (
      <div className="file-type-icon file-type-image">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
          <circle cx="8.5" cy="8.5" r="1.5"/>
          <polyline points="21 15 16 10 5 21"/>
        </svg>
      </div>
    );
  }
  if (contentType?.startsWith('video/')) {
    return (
      <div className="file-type-icon file-type-video">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <polygon points="23 7 16 12 23 17 23 7"/>
          <rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
        </svg>
      </div>
    );
  }
  if (contentType?.includes('pdf')) {
    return (
      <div className="file-type-icon file-type-pdf">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
          <polyline points="10 9 9 9 8 9"/>
        </svg>
      </div>
    );
  }
  if (contentType?.includes('zip') || contentType?.includes('compress')) {
    return (
      <div className="file-type-icon file-type-zip">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
          <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
          <line x1="12" y1="22.08" x2="12" y2="12"/>
        </svg>
      </div>
    );
  }
  if (contentType?.includes('word') || contentType?.includes('document')) {
    return (
      <div className="file-type-icon file-type-doc">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
      </div>
    );
  }
  if (contentType?.includes('text')) {
    return (
      <div className="file-type-icon file-type-text">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
      </div>
    );
  }
  return (
    <div className="file-type-icon file-type-default">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/>
        <polyline points="13 2 13 9 20 9"/>
      </svg>
    </div>
  );
}

export default function FileCard({ file, onDeleted }) {
  const [deleting, setDeleting] = useState(false);

  const handleDownload = async (e) => {
    e.stopPropagation();
    try {
      const res = await files.download(file.id);
      const a = document.createElement('a');
      a.href = res.url;
      a.download = file.originalFilename;
      a.rel = 'noopener noreferrer';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    } catch (err) {
      toast(err.message, 'error');
    }
  };

  const handleDelete = async (e) => {
    e.stopPropagation();
    setDeleting(true);
    try {
      await files.delete(file.id);
      toast('File deleted');
      onDeleted?.(file.id);
    } catch (err) {
      toast(err.message, 'error');
      setDeleting(false);
    }
  };

  const [showShareModal, setShowShareModal] = useState(false);
  const [shareLink, setShareLink] = useState('');
  const [isDownloadLink, setIsDownloadLink] = useState(false);
  const [loadingShare, setLoadingShare] = useState(false);

  const [showVersionsModal, setShowVersionsModal] = useState(false);
  const [fileVersions, setFileVersions] = useState([]);
  const [loadingVersions, setLoadingVersions] = useState(false);

  const loadVersions = async () => {
    setLoadingVersions(true);
    try {
      const res = await files.getVersions(file.id);
      setFileVersions(res || []);
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setLoadingVersions(false);
    }
  };

  const handleVersionsClick = (e) => {
    e.stopPropagation();
    setShowVersionsModal(true);
    loadVersions();
  };

  const handleRestoreVersion = async (versionId) => {
    try {
      await files.restoreVersion(file.id, versionId);
      toast('Version restored successfully');
      setShowVersionsModal(false);
      // Let the parent know there's been an update
      onDeleted?.(file.id); // Slight hack to trigger a refresh
    } catch (err) {
      toast(err.message, 'error');
    }
  };

  const handleDownloadVersion = async (versionId) => {
    try {
      const res = await files.downloadVersion(file.id, versionId);
      const a = document.createElement('a');
      a.href = res.url;
      a.download = file.originalFilename;
      a.rel = 'noopener noreferrer';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    } catch (err) {
      toast(err.message, 'error');
    }
  };

  const handleShareClick = async (e) => {
    e.stopPropagation();
    setShowShareModal(true);
    setShareLink('');
    await generateShareLink(false);
  };

  const generateShareLink = async (downloadFlag) => {
    setLoadingShare(true);
    setIsDownloadLink(downloadFlag);
    try {
      const res = await files.share(file.id, downloadFlag);
      setShareLink(res.url);
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setLoadingShare(false);
    }
  };

  const copyShareLink = async () => {
    if (!shareLink) return;
    try {
      await navigator.clipboard.writeText(shareLink);
      toast('Share link copied to clipboard!');
      setShowShareModal(false);
    } catch (err) {
      toast('Failed to copy link', 'error');
    }
  };

  return (
    <>
      <div className="file-card">
        <FileIcon contentType={file.contentType} thumbnailUrl={file.thumbnailUrl} altText={file.originalFilename} />
        <div className="file-info">
          <p className="file-name" title={file.originalFilename}>{file.originalFilename}</p>
          <div className="file-meta">
            <span>{formatSize(file.size)}</span>
            <span className="meta-sep">&middot;</span>
            <span>{formatDate(file.uploadedAt)}</span>
          </div>
        </div>
        <div className="file-actions">
          <button className="btn btn-icon" onClick={handleVersionsClick} title="Version History">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 8v4l3 3"/>
              <circle cx="12" cy="12" r="10"/>
            </svg>
          </button>
          <button className="btn btn-icon" onClick={handleShareClick} title="Share">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="18" cy="5" r="3"/>
              <circle cx="6" cy="12" r="3"/>
              <circle cx="18" cy="19" r="3"/>
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
            </svg>
          </button>
          <button className="btn btn-icon" onClick={handleDownload} title="Download">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
          </button>
          <button className="btn btn-icon btn-icon-danger" onClick={handleDelete} disabled={deleting} title="Delete">
            {deleting ? (
              <span className="spinner-xs" />
            ) : (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            )}
          </button>
        </div>
      </div>

      {showShareModal && (
        <div className="dialog-overlay" onClick={() => setShowShareModal(false)}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <div className="dialog-header">
              <h3>Share "{file.originalFilename}"</h3>
            </div>
            <div className="dialog-body">
              <p style={{ marginBottom: '16px' }}>Anyone with this link can access the file for 1 hour.</p>
              
              <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
                <button 
                  className={`btn btn-sm ${!isDownloadLink ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => generateShareLink(false)}
                >
                  View Only
                </button>
                <button 
                  className={`btn btn-sm ${isDownloadLink ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => generateShareLink(true)}
                >
                  Direct Download
                </button>
              </div>

              <input 
                type="text" 
                value={loadingShare ? 'Generating link...' : shareLink} 
                readOnly 
                onClick={(e) => e.target.select()}
              />
            </div>
            <div className="dialog-footer">
              <button className="btn btn-secondary" onClick={() => setShowShareModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={copyShareLink} disabled={loadingShare || !shareLink}>
                Copy Link
              </button>
            </div>
          </div>
        </div>
      )}

      {showVersionsModal && (
        <div className="dialog-overlay" onClick={() => setShowVersionsModal(false)}>
          <div className="dialog dialog-lg" onClick={(e) => e.stopPropagation()}>
            <div className="dialog-header">
              <h3>Version History for "{file.originalFilename}"</h3>
            </div>
            <div className="dialog-body" style={{ maxHeight: '400px', overflowY: 'auto' }}>
              {loadingVersions ? (
                <div className="loading-state">
                  <div className="spinner" />
                  <p>Loading versions...</p>
                </div>
              ) : fileVersions.length === 0 ? (
                <div className="empty-state">
                  <p>No previous versions found for this file.</p>
                </div>
              ) : (
                <div className="trash-list">
                  {fileVersions.map((v, index) => (
                    <div key={v.id} className="trash-item">
                      <div className="trash-item-info">
                        <span className="trash-item-name">Version {fileVersions.length - index}</span>
                        <div className="trash-item-meta">
                          <span>{formatSize(v.size)}</span>
                          <span className="meta-sep">&middot;</span>
                          <span>Uploaded {formatDate(v.uploadedAt)}</span>
                        </div>
                      </div>
                      <div className="trash-item-actions">
                        <button className="btn btn-sm btn-secondary" onClick={() => handleDownloadVersion(v.id)}>
                          Download
                        </button>
                        <button className="btn btn-sm btn-primary" onClick={() => handleRestoreVersion(v.id)}>
                          Restore
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="dialog-footer">
              <button className="btn btn-secondary" onClick={() => setShowVersionsModal(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
