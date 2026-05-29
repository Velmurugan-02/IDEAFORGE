import React, { useEffect } from 'react';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';

/**
 * Shows an XP / level-up toast notification when an API returns a LevelUpEvent.
 *
 * Usage:
 *   import { showLevelUpToast } from '../components/LevelUpToast';
 *   showLevelUpToast(response.data.levelUpEvent, updateUser);
 */
export function showLevelUpToast(event, updateUser) {
  if (!event || event.xpGained === 0) return;

  // Optimistically update the navbar XP counter
  if (updateUser && event.totalXP !== undefined) {
    updateUser({ xp: event.totalXP, level: event.newLevel });
  }

  if (event.leveledUp) {
    toast.custom(() => (
      <div className="level-up-toast">
        <div className="level-up-toast__icon">🎉</div>
        <div className="level-up-toast__body">
          <p className="level-up-toast__title">Level Up!</p>
          <p className="level-up-toast__sub">
            You are now a <strong>{event.newLevel}</strong>
          </p>
          <p className="level-up-toast__xp">+{event.xpGained} XP</p>
        </div>
      </div>
    ), { duration: 5000 });
  } else {
    toast.success(`+${event.xpGained} XP earned!`, {
      icon: '⚡',
      style: {
        background: '#1e1e2e',
        color: '#a78bfa',
        border: '1px solid rgba(124,58,237,.3)',
      },
    });
  }
}

/* Inline styles for the custom level-up toast */
const style = document.createElement('style');
style.textContent = `
.level-up-toast {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(124,58,237,.3) 0%, rgba(6,182,212,.2) 100%);
  border: 1px solid rgba(124,58,237,.5);
  border-radius: 14px;
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0,0,0,.4), 0 0 24px rgba(124,58,237,.3);
  animation: fadeIn .3s ease;
  min-width: 240px;
}
.level-up-toast__icon { font-size: 2rem; }
.level-up-toast__body { display: flex; flex-direction: column; gap: 2px; }
.level-up-toast__title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #e2e8f0;
}
.level-up-toast__sub { font-size: 13px; color: #a78bfa; }
.level-up-toast__xp {
  font-size: 13px;
  font-weight: 700;
  color: #67e8f9;
}
`;
if (!document.head.querySelector('#level-up-styles')) {
  style.id = 'level-up-styles';
  document.head.appendChild(style);
}
