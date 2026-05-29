import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * ProtectedRoute — redirects unauthenticated users to /login.
 * Preserves the intended path in location state so we can redirect back after login.
 *
 * @param {boolean} adminOnly — when true, also requires ROLE_ADMIN.
 */
export default function ProtectedRoute({ children, adminOnly = false }) {
  const { isAuthenticated, currentUser, loading } = useAuth();
  const location = useLocation();

  // While session is being restored, render nothing (App shows a full-page spinner)
  if (loading) return null;

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (adminOnly && currentUser?.role !== 'ROLE_ADMIN') {
    return <Navigate to="/" replace />;
  }

  return children;
}
