import { useState, useEffect, useCallback } from 'react';
import { files, folders } from '../api';
import Header from './Header';
import UploadArea from './UploadArea';
import FileList from './FileList';
import { toast } from './Toast';

const FILTERS = [
  { label: 'All', value: 'all' },
  { label: 'Images', value: 'image' },
  { label: 'Videos', value: 'video' },
  { label: 'PDFs', value: 'pdf' },
  { label: 'Docs', value: 'doc' },
];

function matchesFilter(contentType, filter) {
  if (filter === 'all') return true;
  if (filter === 'image') return contentType?.startsWith('image/');
  if (filter === 'video') return contentType?.startsWith('video/');
  if (filter === 'pdf') return contentType?.includes('pdf');
  if (filter === 'doc') return contentType?.includes('word') || contentType?.includes('text');
  return true;
}

export default function Dashboard() {
  const [fileList, setFileList] = useState([]);
  const [folderList, setFolderList] = useState([]);
  const [breadcrumbs, setBreadcrumbs] = useState([]);
  const [currentFolderId, setCurrentFolderId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  const [showNewFolder, setShowNewFolder] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');
  const [creatingFolder, setCreatingFolder] = useState(false);
  const [storageRefresh, setStorageRefresh] = useState(0);

  const loadContents = useCallback(async (folderId) => {
    setLoading(true);
    try {
      const [fileData, folderData] = await Promise.all([
        files.list(folderId),
        folders.list(folderId),
      ]);
      setFileList(fileData || []);
      setFolderList(folderData || []);

      if (folderId) {
        const bc = await folders.breadcrumbs(folderId);
        setBreadcrumbs(bc || []);
      } else {
        setBreadcrumbs([]);
      }
    } catch (err) {
      toast('Failed to load files', 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadContents(currentFolderId);
  }, [currentFolderId, loadContents]);

  const navigateToFolder = (folderId) => {
    setCurrentFolderId(folderId);
    setFilter('all');
  };

  const handleUploaded = (file) => {
    setFileList((prev) => [file, ...prev]);
    setStorageRefresh((v) => v + 1);
  };

  const handleFileDeleted = (id) => {
    setFileList((prev) => prev.filter((f) => f.id !== id));
    setStorageRefresh((v) => v + 1);
  };

  const handleFolderDeleted = (id) => {
    setFolderList((prev) => prev.filter((f) => f.id !== id));
    setStorageRefresh((v) => v + 1);
  };

  const handleCreateFolder = async (e) => {
    e.preventDefault();
    if (!newFolderName.trim()) return;
    setCreatingFolder(true);
    try {
      const folder = await folders.create(newFolderName.trim(), currentFolderId);
      setFolderList((prev) => [folder, ...prev]);
      toast(`Folder "${folder.name}" created`);
      setShowNewFolder(false);
      setNewFolderName('');
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setCreatingFolder(false);
    }
  };

  const filteredFiles = fileList.filter((f) => matchesFilter(f.contentType, filter));
  const totalItems = filteredFiles.length + folderList.length;

  return (
    <div className="dashboard">
      <Header storageRefresh={storageRefresh} />
      <main className="dashboard-main">
        <div className="container">
          <section className="upload-section">
            <UploadArea onUploaded={handleUploaded} currentFolderId={currentFolderId} />
          </section>

          <section className="files-section">
            <div className="toolbar">
              <div className="toolbar-left">
                <div className="breadcrumbs">
                  <button className="breadcrumb-item" onClick={() => navigateToFolder(null)}>Home</button>
                  {breadcrumbs.map((bc) => (
                    <span key={bc.id} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <span className="breadcrumb-sep">/</span>
                      <button
                        className={`breadcrumb-item ${bc.id === currentFolderId ? 'active' : ''}`}
                        onClick={() => navigateToFolder(bc.id)}
                      >
                        {bc.name}
                      </button>
                    </span>
                  ))}
                </div>
              </div>
              <div className="toolbar-right">
                <button className="btn btn-secondary btn-sm" onClick={() => setShowNewFolder(true)}>
                  + New Folder
                </button>
                <span className="file-count">{totalItems} item{totalItems !== 1 ? 's' : ''}</span>
              </div>
            </div>

            <div className="filter-chips">
              {FILTERS.map((f) => (
                <button
                  key={f.value}
                  className={`filter-chip ${filter === f.value ? 'active' : ''}`}
                  onClick={() => setFilter(f.value)}
                >
                  {f.label}
                </button>
              ))}
            </div>

            <div style={{ marginTop: '16px' }}>
              {loading ? (
                <div className="loading-state">
                  <div className="spinner" />
                  <p>Loading...</p>
                </div>
              ) : (
                <FileList
                  files={filteredFiles}
                  folders={filter === 'all' ? folderList : []}
                  onDeleted={handleFileDeleted}
                  onFolderOpen={navigateToFolder}
                  onFolderDeleted={handleFolderDeleted}
                />
              )}
            </div>
          </section>
        </div>
      </main>

      {showNewFolder && (
        <div className="dialog-overlay" onClick={() => setShowNewFolder(false)}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <div className="dialog-header">
              <h3>Create New Folder</h3>
            </div>
            <form onSubmit={handleCreateFolder}>
              <div className="dialog-body">
                <p>Enter a name for your new folder</p>
                <input
                  type="text"
                  placeholder="Folder name"
                  value={newFolderName}
                  onChange={(e) => setNewFolderName(e.target.value)}
                  autoFocus
                  required
                />
              </div>
              <div className="dialog-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowNewFolder(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={creatingFolder || !newFolderName.trim()}>
                  {creatingFolder ? <span className="spinner-xs" /> : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
