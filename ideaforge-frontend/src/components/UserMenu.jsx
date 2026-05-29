import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import './UserMenu.css';

const LEVEL_COLORS = {
  Newcomer:  '#94a3b8',
  Thinker:   '#06b6d4',
  Innovator: '#10b981',
  Visionary: '#7c3aed',
  Legend:    '#d4a853',
};

const LEVEL_ORDER = ['Newcomer', 'Thinker', 'Innovator', 'Visionary', 'Legend'];

function xpProgressForMenu(xp, level) {
  const idx = LEVEL_ORDER.indexOf(level);
  if (idx < 0) return 0;
  if (level === 'Legend') return 100;
  const curMin = [0, 200, 500, 1000, 2500][idx];
  const nextMin = [200, 500, 1000, 2500, Infinity][idx];
  if (nextMin === Infinity) return 100;
  return Math.min(100, Math.max(0, Math.round(((xp - curMin) / (nextMin - curMin)) * 100)));
}

/**
 * Avatar button + dropdown: Profile, My Ideas, Collab Requests, Admin (optional), Logout.
 */
export default function UserMenu({ user, isAdmin, onLogout }) {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef(null);
  const levelColor = LEVEL_COLORS[user?.level] || '#7c3aed';
  const xpPct = xpProgressForMenu(user?.xp ?? 0, user?.level);

  const close = () => setOpen(false);

  useEffect(() => {
    const onDoc = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  const handleLogout = () => {
    close();
    onLogout();
  };

  return (
    <div className="user-menu" ref={wrapRef}>
      <button
        type="button"
        className="user-menu__trigger"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        aria-haspopup="true"
        title="Account menu"
      >
        <span
          className="user-menu__avatar"
          style={{ background: `linear-gradient(135deg, ${levelColor}, var(--accent-secondary))` }}
        >
          {user?.name?.charAt(0).toUpperCase() || '?'}
        </span>
        <span className="user-menu__chev" aria-hidden>▾</span>
      </button>

      {open && (
        <div className="user-menu__dropdown fade-in" role="menu">
          <div className="user-menu__head">
            <p className="user-menu__name">{user?.name}</p>
            <p className="user-menu__meta">
              <span style={{ color: levelColor }}>{user?.level}</span>
              <span className="user-menu__dot">·</span>
              <span>{user?.xp ?? 0} XP</span>
            </p>
            <div className="user-menu__xpbar">
              <div className="user-menu__xpfill" style={{ width: `${xpPct}%`, background: levelColor }} />
            </div>
          </div>

          <Link to="/profile" className="user-menu__item" role="menuitem" onClick={close}>
            👤 Profile
          </Link>
          <Link to="/profile" className="user-menu__item" role="menuitem" onClick={close}>
            💡 My ideas
          </Link>
          <Link to="/collab-requests" className="user-menu__item" role="menuitem" onClick={close}>
            🤝 Collab requests
          </Link>
          {isAdmin && (
            <Link to="/admin" className="user-menu__item user-menu__item--admin" role="menuitem" onClick={close}>
              🛡️ Admin panel
            </Link>
          )}

          <div className="user-menu__sep" />

          <button type="button" className="user-menu__item user-menu__item--danger" role="menuitem" onClick={handleLogout}>
            Logout
          </button>
        </div>
      )}
    </div>
  );
}
