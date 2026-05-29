import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';

// Layout
import Navbar        from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';

// Pages
import LoginPage          from './pages/LoginPage';
import RegisterPage       from './pages/RegisterPage';
import IdeaFeedPage       from './pages/IdeaFeedPage';
import IdeaDetailPage     from './pages/IdeaDetailPage';
import CertificatePage    from './pages/CertificatePage';
import BattleArenaPage    from './pages/BattleArenaPage';
import LeaderboardPage    from './pages/LeaderboardPage';
import HallOfFamePage     from './pages/HallOfFamePage';
import ProfilePage        from './pages/ProfilePage';
import AdminPage          from './pages/AdminPage';
import CollabRequestsPage from './pages/CollabRequestsPage';

/** Full-page loader shown while restoring session from localStorage */
function AppLoader() {
  return (
    <div style={{
      minHeight: '100vh', display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', gap: 16,
    }}>
      <div style={{
        fontSize: '2.5rem',
        filter: 'drop-shadow(0 0 20px rgba(124,58,237,.6))',
        animation: 'float 1.5s ease-in-out infinite',
      }}>⚡</div>
      <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>Loading IdeaForge…</p>
    </div>
  );
}

export default function App() {
  const { loading } = useAuth();

  // Restore session from localStorage before rendering any routes
  if (loading) return <AppLoader />;

  return (
    <>
      <Navbar />

      <main>
        <Routes>
          {/* ── Public routes ── */}
          <Route path="/login"    element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Certificate — fully public, no auth needed */}
          <Route path="/ideas/:id/certificate" element={<CertificatePage />} />

          {/* Hall of Fame — public */}
          <Route path="/hall-of-fame" element={<HallOfFamePage />} />

          {/* ── Protected routes ── */}
          <Route path="/" element={
            <ProtectedRoute><IdeaFeedPage /></ProtectedRoute>
          } />

          <Route path="/ideas/:id" element={
            <ProtectedRoute><IdeaDetailPage /></ProtectedRoute>
          } />

          <Route path="/battle" element={
            <ProtectedRoute><BattleArenaPage /></ProtectedRoute>
          } />

          <Route path="/leaderboard" element={
            <ProtectedRoute><LeaderboardPage /></ProtectedRoute>
          } />

          {/* Own profile */}
          <Route path="/profile" element={
            <ProtectedRoute><ProfilePage /></ProtectedRoute>
          } />

          {/* Any user's profile */}
          <Route path="/profile/:userId" element={
            <ProtectedRoute><ProfilePage /></ProtectedRoute>
          } />

          <Route path="/collab-requests" element={
            <ProtectedRoute><CollabRequestsPage /></ProtectedRoute>
          } />

          {/* Admin only */}
          <Route path="/admin" element={
            <ProtectedRoute adminOnly><AdminPage /></ProtectedRoute>
          } />

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
