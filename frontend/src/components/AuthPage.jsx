import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { auth } from '../api';
import { toast } from './Toast';

export default function AuthPage() {
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleRequestOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await auth.requestOtp(email);
      setStep(2);
      toast('Verification code sent to your email');
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await auth.verifyOtp(email, otp);
      login(res.token, res.email);
      toast('Authenticated successfully!');
      navigate('/');
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setLoading(false);
    }
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

        {step === 1 ? (
          <form onSubmit={handleRequestOtp} className="auth-form">
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
              {loading ? (
                <span className="btn-loading"><span className="spinner-sm" /> Sending code...</span>
              ) : (
                'Continue with Email'
              )}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp} className="auth-form">
            <div className="form-group">
              <label htmlFor="otp">Verification Code</label>
              <input
                id="otp"
                type="text"
                placeholder="Enter 6-digit code"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                required
                maxLength={6}
                autoFocus
                style={{ letterSpacing: '4px', textAlign: 'center', fontSize: '1.2rem', fontWeight: 'bold' }}
              />
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px', textAlign: 'center' }}>
                Code sent to {email} <button type="button" onClick={() => setStep(1)} className="btn-ghost" style={{ padding: 0, color: 'var(--primary)' }}>Change</button>
              </p>
            </div>
            <button type="submit" className="btn btn-primary btn-block" disabled={loading || otp.length < 6}>
              {loading ? (
                <span className="btn-loading"><span className="spinner-sm" /> Verifying...</span>
              ) : (
                'Verify & Login'
              )}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
