import { useState, useRef } from 'react';
import { files } from '../api';
import { toast } from './Toast';

export default function UploadArea({ onUploaded }) {
  const [dragOver, setDragOver] = useState(false);
  const [uploading, setUploading] = useState(false);
  const inputRef = useRef();

  const handleUpload = async (file) => {
    if (!file) return;
    if (file.size === 0) {
      toast('Cannot upload empty file', 'error');
      return;
    }
    setUploading(true);
    try {
      const result = await files.upload(file);
      toast(`Uploaded ${result.originalFilename}`);
      onUploaded?.(result);
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setUploading(false);
    }
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
        <div className="upload-status">
          <div className="spinner" />
          <p>Uploading...</p>
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
