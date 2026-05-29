import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import wsService from '../services/wsService';
import UserMenu from './UserMenu';
import './Navbar.css';

/* ── NotificationBell ── */
function NotificationBell({ userId }) {
  const [notifications, setNotifications] = useState([]);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef(null);
  const subRef = useRef(null);
  const openRef = useRef(false);

  useEffect(() => {
    openRef.current = open;
  }, [open]);

  useEffect(() => {
    if (!open) return;
    setUnread(0);
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  }, [open]);

  useEffect(() => {
    if (!userId) return;
    wsService.subscribe(`/topic/user/${userId}/notifications`, (payload) => {
      const alreadyViewing = openRef.current;
      const note = {
        id: payload.id ?? `n-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
        type: payload.type || 'INFO',
        message: payload.message || 'New notification',
        time: new Date(),
        read: alreadyViewing,
      };
      setNotifications((prev) => [note, ...prev].slice(0, 20));
      if (!alreadyViewing) setUnread((n) => n + 1);
    }).then((sub) => {
      subRef.current = sub;
    });
    return () => wsService.unsubscribe(subRef.current);
  }, [userId]);

  useEffect(() => {
    const handler = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const toggleDropdown = useCallback(() => setOpen((o) => !o), []);

  const typeIcon = (type) => {
    if (type === 'COLLAB_REQUEST') return '🤝';
    if (type === 'PLAGIARISM_REPORT') return '🚨';
    if (type === 'COLLAB_ACCEPTED') return '✅';
    return '🔔';
  };

  return (
    <div className="nb-wrap" ref={dropdownRef}>
      <button
        id="notification-bell-btn"
        type="button"
        className="nb-bell"
        onClick={toggleDropdown}
        aria-expanded={open}
        aria-label={unread ? `${unread} unread notifications` : 'Notifications'}
        title="Notifications"
      >
        <span className="nb-bell-emoji" aria-hidden>🔔</span>
        {unread > 0 && <span className="nb-badge">{unread > 9 ? '9+' : unread}</span>}
      </button>

      {open && (
        <div className="nb-dropdown fade-in" role="menu">
          <div className="nb-dropdown__header">
            <span className="nb-dropdown__title">Notifications</span>
            <button type="button" className="nb-clear-btn" onClick={() => setNotifications([])}>
              Clear all
            </button>
          </div>
          <div className="nb-dropdown__list">
            {notifications.length === 0 ? (
              <div className="nb-empty">
                <span>🔕</span>
                <p>No notifications yet</p>
              </div>
            ) : (
              notifications.map((n) => (
                <div key={n.id} className={`nb-item ${n.read ? 'nb-item--read' : 'nb-item--unread'}`}>
                  <span className="nb-item__icon">{typeIcon(n.type)}</span>
                  <div className="nb-item__body">
                    <p className="nb-item__msg">{n.message}</p>
                    <span className="nb-item__time">
                      {n.time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

const MAIN_LINKS = [
  { to: '/', label: 'Feed' },
  { to: '/leaderboard', label: 'Leaderboard' },
  { to: '/battle', label: 'Battle' },
  { to: '/hall-of-fame', label: 'Hall of Fame' },
];

export default function Navbar() {
  const { currentUser, logout, isAdmin } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-logo" onClick={() => setMenuOpen(false)}>
          <span className="navbar-logo-text">
            Idea<span className="gradient-text">Forge</span>
            <span className="navbar-logo-bulb" aria-hidden>💡</span>
          </span>
        </Link>

        <ul className="navbar-links">
          {MAIN_LINKS.map(({ to, label }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={to === '/'}
                className={({ isActive }) => `navbar-link ${isActive ? 'navbar-link--active' : ''}`}
              >
                {label}
              </NavLink>
            </li>
          ))}
        </ul>

        <div className="navbar-actions">
          {currentUser ? (
            <>
              <NotificationBell userId={currentUser.id} />
              <UserMenu user={currentUser} isAdmin={isAdmin} onLogout={logout} />
            </>
          ) : (
            <NavLink to="/login" className="btn-secondary navbar-login">
              Log in
            </NavLink>
          )}
        </div>

        <button
          type="button"
          className={`navbar-hamburger ${menuOpen ? 'open' : ''}`}
          onClick={() => setMenuOpen((o) => !o)}
          aria-label="Toggle menu"
        >
          <span /><span /><span />
        </button>
      </div>

      {menuOpen && (
        <div className="navbar-drawer">
          {MAIN_LINKS.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className="navbar-drawer-link"
              onClick={() => setMenuOpen(false)}
            >
              {label}
            </NavLink>
          ))}
          {currentUser ? (
            <>
              <div className="navbar-drawer-sep" />
              <NavLink to="/profile" className="navbar-drawer-link" onClick={() => setMenuOpen(false)}>
                Profile
              </NavLink>
              <NavLink to="/profile" className="navbar-drawer-link" onClick={() => setMenuOpen(false)}>
                My ideas
              </NavLink>
              <NavLink to="/collab-requests" className="navbar-drawer-link" onClick={() => setMenuOpen(false)}>
                Collab requests
              </NavLink>
              {isAdmin && (
                <NavLink to="/admin" className="navbar-drawer-link" onClick={() => setMenuOpen(false)}>
                  Admin panel
                </NavLink>
              )}
              <button type="button" className="navbar-drawer-logout" onClick={() => { setMenuOpen(false); logout(); }}>
                Logout
              </button>
            </>
          ) : (
            <NavLink to="/login" className="navbar-drawer-link" onClick={() => setMenuOpen(false)}>
              Log in
            </NavLink>
          )}
        </div>
      )}
    </nav>
  );
}
