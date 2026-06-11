import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { auth } from '../api';
import { toast } from './Toast';
import { GoogleLogin } from '@react-oauth/google';

export default function AuthPage() {
  const [loading, setLoading] = useState(false);
  const { login, user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (user) navigate('/', { replace: true });
  }, [user, navigate]);

  const handleGoogleSuccess = async (credentialResponse) => {
    setLoading(true);
    try {
      const res = await auth.googleAuth(credentialResponse.credential);
      login(res.token, res.email);
      toast('Welcome to FileVault!');
      navigate('/');
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleError = () => {
    toast('Google Sign-In was cancelled or failed. Please try again.', 'error');
  };

  return (
    <div className="auth-page">
      {/* Animated background orbs */}
      <div className="auth-bg-orb auth-bg-orb-1" />
      <div className="auth-bg-orb auth-bg-orb-2" />
      <div className="auth-bg-orb auth-bg-orb-3" />

      <div className="auth-card">
        <div className="auth-logo-container">
          <div className="auth-logo-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/>
              <polyline points="13 2 13 9 20 9"/>
            </svg>
          </div>
          <h1 className="auth-title">FileVault</h1>
        </div>

        <p className="auth-description">
          Secure cloud storage for your files, photos, and documents. 
          Get started with 1 GB of free storage.
        </p>

        <div className="auth-features-grid">
          <div className="auth-feature">
            <div className="auth-feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <span>Upload & organize files</span>
          </div>
          <div className="auth-feature">
            <div className="auth-feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <span>Create nested folders</span>
          </div>
          <div className="auth-feature">
            <div className="auth-feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <span>Share files instantly</span>
          </div>
          <div className="auth-feature">
            <div className="auth-feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <span>Encrypted & secure</span>
          </div>
        </div>

        <div className="auth-divider">
          <span>Sign in to continue</span>
        </div>

        <div className="auth-google-container">
          {loading ? (
            <div className="auth-loading">
              <div className="auth-spinner" />
              <span>Authenticating...</span>
            </div>
          ) : (
            <GoogleLogin
              onSuccess={handleGoogleSuccess}
              onError={handleGoogleError}
              theme="outline"
              shape="rectangular"
              size="large"
              width="320"
              text="continue_with"
              logo_alignment="left"
            />
          )}
        </div>

        <p className="auth-footer">
          By continuing, you agree to our Terms of Service and Privacy Policy.
        </p>
      </div>
    </div>
  );
}
