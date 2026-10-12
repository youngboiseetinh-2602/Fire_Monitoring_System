import { useEffect, useRef, useState } from 'react';
import { authenticate } from './api';

function Arrow() {
  return <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 12h15m-6-7 7 7-7 7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" /></svg>;
}

function AuthDialog({ mode, onClose, onSwitch, onLogin }) {
  const dialog = useRef(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [fields, setFields] = useState({});
  const [success, setSuccess] = useState(false);
  const register = mode === 'register';

  useEffect(() => {
    const element = dialog.current;
    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    element.showModal();
    element.querySelector('input')?.focus();
    document.body.style.overflow = 'hidden';
    return () => {
      element.close();
      document.body.style.overflow = previousOverflow;
      previousFocus?.focus();
    };
  }, []);

  async function submit(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    if (register && values.password !== values.confirmPassword) {
      setFields({ confirmPassword: 'Passwords do not match.' });
      return;
    }
    delete values.confirmPassword;
    if (register) {
      values.role = 'MEMBER';
      if (!values.phone) delete values.phone;
    }
    setPending(true);
    setError('');
    setFields({});
    try {
      const result = await authenticate(mode, values);
      if (register) setSuccess(true);
      else onLogin({ username: values.username, token: result });
    } catch (failure) {
      setError(failure.message);
      setFields(failure.fields || {});
    } finally {
      setPending(false);
    }
  }

  function field(name, label, options = {}) {
    return <div className={options.half ? 'col-sm-6' : 'col-12'}>
      <label className="form-label" htmlFor={name}>{label}</label>
      <input id={name} name={name} className={`form-control${fields[name] ? ' is-invalid' : ''}`}
        type={options.type || 'text'} required={!options.optional} maxLength={options.maxLength}
        minLength={options.minLength} pattern={options.pattern} autoComplete={options.autoComplete}
        autoFocus={name === (register ? 'fullName' : 'username')}
        aria-invalid={!!fields[name]} aria-describedby={fields[name] ? `${name}-error` : undefined} />
      {fields[name] && <div id={`${name}-error`} className="invalid-feedback">{fields[name]}</div>}
    </div>;
  }

  return <dialog ref={dialog} className="auth-dialog" aria-labelledby="auth-title"
    onCancel={event => { event.preventDefault(); if (!pending) onClose(); }}
    onClick={event => { if (event.target === dialog.current && !pending) {
      const rect = dialog.current.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) onClose();
    } }}>
    <button className="btn-close dialog-close" aria-label="Close" onClick={onClose} disabled={pending} />
    <img className="dialog-flame" src="/flame.svg" alt="" />
    <h2 id="auth-title">{success ? 'You’re all set.' : register ? 'Create your account' : 'Welcome back.'}</h2>
    <p className="auth-description">{success ? 'Your account has been created. Log in to get started.' : register ? 'Take the first step toward a safer community.' : 'Log in to your Fire Monitoring System account.'}</p>
    {success ? <button className="btn btn-fire w-100" onClick={() => onSwitch('login')}>Continue to login <Arrow /></button> : <>
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <form onSubmit={submit}>
        <fieldset disabled={pending}>
          <div className="row g-3">
            {register && field('fullName', 'Full name', { maxLength: 100, autoComplete: 'name' })}
            {field('username', 'Username', { maxLength: 50, autoComplete: 'username' })}
            {register && field('phone', 'Phone (optional)', { type: 'tel', pattern: '0[0-9]{9}', maxLength: 10, autoComplete: 'tel', half: true, optional: true })}
            {register && field('cccd', 'Citizen ID (CCCD)', { maxLength: 20, half: true })}
            {field('password', 'Password', { type: 'password', minLength: register ? 8 : undefined, maxLength: register ? 100 : undefined, autoComplete: register ? 'new-password' : 'current-password' })}
            {register && field('confirmPassword', 'Confirm password', { type: 'password', minLength: 8, maxLength: 100, autoComplete: 'new-password' })}
          </div>
          {register && <p className="password-hint">Use 8–100 characters for your password.</p>}
          <button className="btn btn-fire w-100 mt-4" type="submit">
            {pending ? <><span className="spinner-border spinner-border-sm" aria-hidden="true" /> Please wait…</> : <>{register ? 'Create account' : 'Login'} <Arrow /></>}
          </button>
        </fieldset>
      </form>
      <p className="auth-switch">{register ? 'Already have an account?' : 'New to Fire Monitoring?'}{' '}
        <button className="btn btn-link" disabled={pending} onClick={() => onSwitch(register ? 'login' : 'register')}>{register ? 'Login' : 'Register'}</button>
      </p>
    </>}
  </dialog>;
}

export default function App() {
  const [mode, setMode] = useState(null);
  // Keep the access token in memory; refreshing the page requires a new login.
  const [session, setSession] = useState(null);
  return <div className="landing">
    <a className="skip-link" href="#main">Skip to content</a>
    <header className="site-header container-fluid d-flex align-items-center justify-content-between">
      <a className="brand d-flex align-items-center" href="/" aria-label="Fire Monitoring System home">
        <img src="/flame.svg" width="45" height="60" alt="" />
        <span>Fire Monitoring System</span>
      </a>
      <nav className="d-flex align-items-center header-actions" aria-label="Account">
        {session ? <button className="btn btn-outline-dark" onClick={() => setSession(null)}>Log out</button> : <>
          <button className="btn btn-header-login" onClick={() => setMode('login')}>Login</button>
          <button className="btn btn-fire" onClick={() => setMode('register')}>Register</button>
        </>}
      </nav>
    </header>
    <main id="main" className="hero container-fluid">
      <div className="hero-copy">
        <h1>Fire Monitoring<br />System</h1>
        <p>Monitor forest fires in real-time, get early alerts,<br className="desktop-break" /> and help protect our forests and communities.</p>
        {session ? <div className="session-card" role="status"><strong>Welcome, {session.username}.</strong><span>You have successfully logged in.</span></div> :
          <div className="hero-actions d-flex flex-wrap">
            <button className="btn btn-fire" onClick={() => setMode('login')}>Login <Arrow /></button>
            <button className="btn btn-outline-dark" onClick={() => setMode('register')}>Register <Arrow /></button>
          </div>}
      </div>
    </main>
    {mode && <AuthDialog key={mode} mode={mode} onClose={() => setMode(null)} onSwitch={setMode}
      onLogin={value => { setSession(value); setMode(null); }} />}
  </div>;
}
