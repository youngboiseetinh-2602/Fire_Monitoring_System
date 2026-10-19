import { createContext, useContext, useEffect, useState } from 'react';
import { authApi } from '../api/authApi';
import { userApi } from '../api/userApi';
import { setAccessToken, setSessionExpiredHandler } from '../api/axiosClient';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [expired, setExpired] = useState(false);

  useEffect(() => setSessionExpiredHandler(() => {
    setUser(null);
    setExpired(true);
  }), []);

  async function login(values) {
    const token = await authApi.login(values);
    const profile = await userApi.getMe({ headers: { Authorization: `Bearer ${token}` }, auth: false });
    setAccessToken(token);
    setUser(profile);
    setExpired(false);
  }

  function logout() {
    setAccessToken(null);
    setUser(null);
    setExpired(false);
  }

  return <AuthContext.Provider value={{ user, login, logout, expired, updateUser: profile => setUser(current => current ? profile : null) }}>
    {children}
  </AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
