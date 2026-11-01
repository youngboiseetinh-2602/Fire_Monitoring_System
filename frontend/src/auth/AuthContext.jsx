import { createContext, useContext, useEffect, useState } from 'react';
import { authApi } from '../api/authApi';
import { userApi } from '../api/userApi';
import { setAccessToken, setSessionExpiredHandler } from '../api/axiosClient';

const AuthContext = createContext(null);
const tokenKey = 'fire-monitoring.access-token';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [expired, setExpired] = useState(false);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    const unsubscribe = setSessionExpiredHandler(() => {
      sessionStorage.removeItem(tokenKey);
      setUser(null);
      setExpired(true);
    });
    const token = sessionStorage.getItem(tokenKey);
    if (!token) {
      setInitializing(false);
      return unsubscribe;
    }
    setAccessToken(token);
    userApi.getMe()
      .then(profile => setUser({ ...profile, accessToken: token }))
      .catch(() => {
        sessionStorage.removeItem(tokenKey);
        setAccessToken(null);
      })
      .finally(() => setInitializing(false));
    return unsubscribe;
  }, []);

  async function login(values) {
    const token = await authApi.login(values);
    const profile = await userApi.getMe({ headers: { Authorization: `Bearer ${token}` }, auth: false });
    setAccessToken(token);
    sessionStorage.setItem(tokenKey, token);
    setUser({ ...profile, accessToken: token });
    setExpired(false);
  }

  function logout() {
    setAccessToken(null);
    sessionStorage.removeItem(tokenKey);
    setUser(null);
    setExpired(false);
  }

  return <AuthContext.Provider value={{ user, login, logout, expired, initializing, updateUser: profile => setUser(current => current ? { ...profile, accessToken: current.accessToken } : null) }}>
    {children}
  </AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
