import { useState } from 'react';
import { folders } from '../api';
import { toast } from './Toast';

export default function FolderCard({ folder, onOpen, onDeleted }) {
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async (e) => {
    e.stopPropagation();
    if (!window.confirm(`Delete folder "${folder.name}" and all its contents?`)) return;
    setDeleting(true);
    try {
      await folders.delete(folder.id);
      toast('Folder deleted');
      onDeleted?.(folder.id);
    } catch (err) {
      toast(err.message, 'error');
      setDeleting(false);
    }
  };

  return (
    <div className="folder-card" onClick={() => onOpen(folder.id)} onKeyDown={(e) => e.key === 'Enter' && onOpen(folder.id)} tabIndex={0} role="button">
      <div className="folder-icon">{'\uD83D\uDCC1'}</div>
      <div className="folder-name" title={folder.name}>{folder.name}</div>
      <div className="folder-actions">
        <button className="btn btn-icon btn-icon-danger" onClick={handleDelete} disabled={deleting} title="Delete folder">
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
