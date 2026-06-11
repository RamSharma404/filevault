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

function getFileIcon(contentType) {
  if (!contentType) return '\uD83D\uDCC4';
  if (contentType.startsWith('image/')) return '\uD83D\uDDBC\uFE0F';
  if (contentType.startsWith('video/')) return '\uD83C\uDFA5';
  if (contentType.includes('pdf')) return '\uD83D\uDCD1';
  if (contentType.includes('zip') || contentType.includes('compress')) return '\uD83D\uDCE6';
  if (contentType.includes('text')) return '\uD83D\uDCDD';
  if (contentType.includes('word')) return '\uD83D\uDCC3';
  return '\uD83D\uDCC4';
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

  return (
    <div className="file-card">
      <div className="file-icon">{getFileIcon(file.contentType)}</div>
      <div className="file-info">
        <p className="file-name" title={file.originalFilename}>{file.originalFilename}</p>
        <div className="file-meta">
          <span>{formatSize(file.size)}</span>
          <span className="meta-sep">&middot;</span>
          <span>{formatDate(file.uploadedAt)}</span>
        </div>
      </div>
      <div className="file-actions">
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
  );
}
