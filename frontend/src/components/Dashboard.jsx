import { useState, useEffect, useCallback } from 'react';
import { files } from '../api';
import Header from './Header';
import UploadArea from './UploadArea';
import FileList from './FileList';
import { toast } from './Toast';

export default function Dashboard() {
  const [fileList, setFileList] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadFiles = useCallback(async () => {
    try {
      const data = await files.list();
      setFileList(data || []);
    } catch (err) {
      toast('Failed to load files', 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadFiles();
  }, [loadFiles]);

  const handleUploaded = (file) => {
    setFileList((prev) => [file, ...prev]);
  };

  const handleDeleted = (id) => {
    setFileList((prev) => prev.filter((f) => f.id !== id));
  };

  return (
    <div className="dashboard">
      <Header />
      <main className="dashboard-main">
        <div className="container">
          <section className="upload-section">
            <h2 className="section-title">Upload Files</h2>
            <UploadArea onUploaded={handleUploaded} />
          </section>

          <section className="files-section">
            <div className="section-header">
              <h2 className="section-title">Your Files</h2>
              <span className="file-count">{fileList.length} file{fileList.length !== 1 ? 's' : ''}</span>
            </div>
            {loading ? (
              <div className="loading-state">
                <div className="spinner" />
                <p>Loading your files...</p>
              </div>
            ) : (
              <FileList files={fileList} onDeleted={handleDeleted} />
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
