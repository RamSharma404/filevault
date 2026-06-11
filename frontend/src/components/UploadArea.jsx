import { useState, useRef } from 'react';
import { files } from '../api';
import { toast } from './Toast';

function formatSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

export default function UploadArea({ onUploaded, currentFolderId }) {
  const [dragOver, setDragOver] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [loaded, setLoaded] = useState(0);
  const [total, setTotal] = useState(0);
  const [fileName, setFileName] = useState('');
  const inputRef = useRef();
  const abortRef = useRef({});

  const handleUpload = async (file) => {
    if (!file) return;
    if (file.size === 0) {
      toast('Cannot upload empty file', 'error');
      return;
    }
    setUploading(true);
    setProgress(0);
    setLoaded(0);
    setTotal(file.size);
    setFileName(file.name);
    try {
      const result = await files.upload(file, currentFolderId, (pct, ld, tl) => {
        setProgress(pct);
        setLoaded(ld);
        setTotal(tl);
      }, abortRef.current);
      toast(`Uploaded ${result.originalFilename}`);
      onUploaded?.(result);
    } catch (err) {
      if (err.message === '__CANCELLED__') {
        toast('Upload cancelled', 'info');
      } else {
        toast(err.message, 'error');
      }
    } finally {
      setUploading(false);
      setProgress(0);
      setLoaded(0);
      setTotal(0);
      setFileName('');
      abortRef.current = {};
    }
  };

  const handleCancel = (e) => {
    e.stopPropagation();
    abortRef.current.abort?.();
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    handleUpload(file);
  };

  const handleChange = (e) => {
    const file = e.target.files[0];
    handleUpload(file);
    e.target.value = '';
  };

  return (
    <div
      className={`upload-area ${dragOver ? 'drag-over' : ''} ${uploading ? 'uploading' : ''}`}
      onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
      onClick={() => !uploading && inputRef.current?.click()}
    >
      <input
        ref={inputRef}
        type="file"
        hidden
        onChange={handleChange}
      />
      {uploading ? (
        <div className="upload-progress-card">
          <div className="upload-progress-header">
            <div className="upload-progress-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="17 8 12 3 7 8"/>
                <line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
            </div>
            <div className="upload-progress-info">
              <p className="upload-progress-filename">{fileName}</p>
              <p className="upload-progress-stats">{formatSize(loaded)} of {formatSize(total)} • {progress}%</p>
            </div>
            <button className="upload-cancel-btn" onClick={handleCancel} title="Cancel upload">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          <div className="upload-progress-bar">
            <div className="upload-progress-bar-fill" style={{ width: `${progress}%` }} />
          </div>
        </div>
      ) : (
        <div className="upload-prompt">
          <div className="upload-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </div>
          <p className="upload-title"><strong>Click to upload</strong> or drag and drop</p>
          <p className="upload-hint">PDF, Images, Videos, Documents (max 1GB per file, 1GB total quota)</p>
        </div>
      )}
    </div>
  );
}
