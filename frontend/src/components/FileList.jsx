import FileCard from './FileCard';
import FolderCard from './FolderCard';

export default function FileList({ files, folders: folderList, onDeleted, onFolderOpen, onFolderDeleted }) {
  const isEmpty = (!files || files.length === 0) && (!folderList || folderList.length === 0);

  if (isEmpty) {
    return (
      <div className="empty-state">
        <div className="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="12" y1="18" x2="12" y2="12"/>
            <line x1="9" y1="15" x2="15" y2="15"/>
          </svg>
        </div>
        <h3>No files yet</h3>
        <p>Upload your first file or create a folder to get started</p>
      </div>
    );
  }

  return (
    <div className="content-grid">
      {folderList && folderList.length > 0 && (
        <div className="file-grid">
          {folderList.map((f) => (
            <FolderCard key={`folder-${f.id}`} folder={f} onOpen={onFolderOpen} onDeleted={onFolderDeleted} />
          ))}
        </div>
      )}
      {files && files.length > 0 && (
        <div className="file-grid">
          {files.map((f) => (
            <FileCard key={`file-${f.id}`} file={f} onDeleted={onDeleted} />
          ))}
        </div>
      )}
    </div>
  );
}
