import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { auth } from '../api';
import { toast } from './Toast';
import { GoogleLogin } from '@react-oauth/google';

export default function AuthPage() {
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleGoogleSuccess = async (credentialResponse) => {
    setLoading(true);
    try {
      const res = await auth.googleAuth(credentialResponse.credential);
      login(res.token, res.email);
      toast('Authenticated successfully!');
      navigate('/');
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleError = () => {
    toast('Google Sign-In failed', 'error');
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-logo">
            <div className="logo-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
            </div>
            <h1>FileVault</h1>
            <p className="auth-subtitle">Secure cloud storage • 1GB free per user</p>
            <div className="auth-features">
              <span>☁️ Cloud Storage</span>
              <span>📁 Folders</span>
              <span>🔒 Encrypted</span>
            </div>
          </div>
        </div>

        <div className="auth-form" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: '2rem' }}>
          {loading ? (
            <div className="btn-loading" style={{ color: 'var(--text)' }}>
              <span className="spinner-sm" /> Authenticating...
            </div>
          ) : (
            <GoogleLogin
              onSuccess={handleGoogleSuccess}
              onError={handleGoogleError}
              theme="filled_black"
              shape="pill"
              size="large"
              text="continue_with"
            />
          )}
          <p style={{ marginTop: '1.5rem', fontSize: '0.85rem', color: 'var(--text-muted)', textAlign: 'center' }}>
            By continuing, you agree to our Terms of Service and Privacy Policy.
          </p>
        </div>
      </div>
    </div>
  );
}
