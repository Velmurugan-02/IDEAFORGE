import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { userAPI } from '../services/api';
import wsService from '../services/wsService';

/**
 * AuthContext — single source of truth for the authenticated user.
 *
 * Shape of currentUser:
 *   { id, name, email, role, xp, level, streakDays, token }
 */
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading]         = useState(true); // true until session is restored
  const navigate = useNavigate();

  /* ----------------------------------------------------------------
     Restore session on app load — reads token from localStorage,
     then fetches /api/users/me to get fresh profile data.
  ---------------------------------------------------------------- */
  useEffect(() => {
    const restoreSession = async () => {
      const token = localStorage.getItem('token');
      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const { data } = await userAPI.getMe();
        const user = { ...data, token };
        setCurrentUser(user);
        localStorage.setItem('user', JSON.stringify(user));
        // Connect WebSocket for authenticated user
        wsService.connect();
      } catch {
        // Token invalid / expired — clean up
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setCurrentUser(null);
      } finally {
        setLoading(false);
      }
    };

    restoreSession();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /* ----------------------------------------------------------------
     login — called after successful /auth/login or /auth/register
  ---------------------------------------------------------------- */
  const login = useCallback((token, userData) => {
    const user = { ...userData, token };
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    setCurrentUser(user);
    wsService.connect();
  }, []);

  /* ----------------------------------------------------------------
     logout — clears everything and redirects
  ---------------------------------------------------------------- */
  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setCurrentUser(null);
    wsService.disconnect();
    navigate('/login');
  }, [navigate]);

  /* ----------------------------------------------------------------
     refreshProfile — called after XP/level changes to keep state fresh
  ---------------------------------------------------------------- */
  const refreshProfile = useCallback(async () => {
    try {
      const { data } = await userAPI.getMe();
      setCurrentUser((prev) => prev ? { ...prev, ...data } : null);
    } catch { /* silent */ }
  }, []);

  /* ----------------------------------------------------------------
     updateUser — optimistic in-memory patch (e.g. after XP toast)
  ---------------------------------------------------------------- */
  const updateUser = useCallback((patch) => {
    setCurrentUser((prev) => {
      if (!prev) return null;
      const updated = { ...prev, ...patch };
      localStorage.setItem('user', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const value = {
    currentUser,
    loading,
    login,
    logout,
    refreshProfile,
    updateUser,
    isAuthenticated: !!currentUser,
    isAdmin: currentUser?.role === 'ROLE_ADMIN',
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/** Hook — throws if used outside AuthProvider */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>');
  return ctx;
}

export default AuthContext;
