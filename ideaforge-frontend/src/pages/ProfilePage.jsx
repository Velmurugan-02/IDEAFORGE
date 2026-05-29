import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { userAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { ProfileSkeleton } from '../components/Skeleton';
import './Profile.css';

/* ── Level config (matches gamification tiers) ── */
const LEVELS = [
  { name: 'Newcomer',  min: 0,    max: 199,  color: '#94a3b8', next: 'Thinker'   },
  { name: 'Thinker',   min: 200,  max: 499,  color: '#06b6d4', next: 'Innovator' },
  { name: 'Innovator', min: 500,  max: 999,  color: '#10b981', next: 'Visionary' },
  { name: 'Visionary', min: 1000, max: 2499, color: '#7c3aed', next: 'Legend'    },
  { name: 'Legend',    min: 2500, max: Infinity, color: '#d4a853', next: null    },
];

function getLevelMeta(level) {
  return LEVELS.find((l) => l.name === level) || LEVELS[0];
}

/** XP fill toward next level; label uses next tier threshold as nextLevelXP. */
function getXpProgress(xp, level) {
  const meta = getLevelMeta(level);
  if (!meta.next) {
    return { pct: 100, current: xp, nextThreshold: meta.min, nextLevel: 'MAX', maxed: true };
  }
  const nextMeta = LEVELS.find((l) => l.name === meta.next);
  const rangeXp = nextMeta.min - meta.min;
  const earnedInRange = Math.max(0, xp - meta.min);
  const pct = Math.min(100, rangeXp <= 0 ? 0 : (earnedInRange / rangeXp) * 100);
  return {
    pct,
    current: xp,
    nextThreshold: nextMeta.min,
    nextLevel: meta.next,
    maxed: false,
  };
}

const ALL_BADGES = [
  { type: 'FIRST_IDEA',       label: 'First Idea',      icon: '💡', desc: 'Posted your first idea'              },
  { type: 'TRENDSETTER',      label: 'Trendsetter',     icon: '🔥', desc: '100+ votes on one idea'              },
  { type: 'SERIAL_PITCHER',   label: 'Serial Pitcher',  icon: '🎯', desc: 'Posted 5 or more ideas'              },
  { type: 'PROBLEM_SOLVER',   label: 'Problem Solver',  icon: '⚔️', desc: 'Won a Battle Arena challenge'        },
  { type: 'GHOST_BUSTER',     label: 'Ghost Buster',    icon: '👻', desc: 'Answered all challenges on an idea'  },
  { type: 'CROWD_FAVOURITE',  label: 'Crowd Favourite', icon: '⭐', desc: 'Idea appeared in Hall of Fame'       },
  { type: 'PROTECTOR',        label: 'Protector',       icon: '🛡️', desc: 'Successfully reported plagiarism'    },
  { type: 'COLLABORATOR',     label: 'Collaborator',    icon: '🤝', desc: 'Accepted or received collab request' },
];

const VISIBILITY_META = {
  PUBLIC:      { cls: 'badge-cyan',  label: '🌍 Public'      },
  PRIVATE:     { cls: 'badge-red',   label: '🔒 Private'     },
  INVITE_ONLY: { cls: 'badge-amber', label: '🔗 Invite Only' },
};

const BATTLE_META = {
  WON:    { cls: 'badge-amber',   label: '🏆 Winner'   },
  ACTIVE: { cls: 'badge-purple',  label: '⚔️ Active'   },
  LOST:   { cls: 'badge-red',     label: '💔 Lost'     },
  NONE:   { cls: 'badge-muted',   label: 'Battle: —'  },
};

function IdeaRow({ idea }) {
  const visMeta = VISIBILITY_META[idea.visibility] || VISIBILITY_META.PUBLIC;
  const battleKey = idea.battleStatus && BATTLE_META[idea.battleStatus] ? idea.battleStatus : 'NONE';
  const battleMeta = BATTLE_META[battleKey];
  return (
    <Link to={`/ideas/${idea.id}`} className="profile-idea-row">
      <div className="profile-idea-row__main">
        <span className="profile-idea-row__title">{idea.title}</span>
        <div className="profile-idea-row__badges">
          <span className={`badge ${battleMeta.cls}`}>{battleMeta.label}</span>
          <span className={`badge ${visMeta.cls}`}>{visMeta.label}</span>
          {idea.isPlagiarised && <span className="badge badge-red">🚫 Flagged</span>}
        </div>
      </div>
      <div className="profile-idea-row__stats">
        <span className="profile-idea-row__stat">👍 {idea.voteCount ?? 0}</span>
        <span className="profile-idea-row__stat">⚡ {Number(idea.tractionScore || 0).toFixed(1)}</span>
      </div>
    </Link>
  );
}

function BadgeTile({ badge, earned }) {
  return (
    <div
      className={`badge-tile ${earned ? 'badge-tile--earned' : 'badge-tile--locked'}`}
      title={earned ? `Earned: ${new Date(earned.earnedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}` : badge.desc}
    >
      <div className="badge-tile__icon">{earned ? badge.icon : '🔒'}</div>
      <div className="badge-tile__label">{badge.label}</div>
      {earned && earned.earnedAt && (
        <div className="badge-tile__earned-date">
          ✓ {new Date(earned.earnedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
        </div>
      )}
      {!earned && <div className="badge-tile__lock-desc">{badge.desc}</div>}
    </div>
  );
}

export default function ProfilePage() {
  const { userId } = useParams();
  const { currentUser } = useAuth();

  const [profile, setProfile] = useState(null);
  const [ideas, setIdeas] = useState([]);
  const [collabs, setCollabs] = useState([]);
  const [badges, setBadges] = useState([]);
  const [loading, setLoading] = useState(true);

  const targetId = userId ? Number(userId) : currentUser?.id;
  const isOwnProfile = targetId === currentUser?.id;

  const load = useCallback(async () => {
    if (!targetId) return;
    setLoading(true);
    try {
      const [profRes, ideasRes, collabsRes, badgesRes] = await Promise.all([
        isOwnProfile ? userAPI.getMe() : userAPI.getById(targetId),
        userAPI.getIdeas(targetId),
        userAPI.getCollaborations(targetId),
        userAPI.getBadges(targetId),
      ]);
      setProfile(profRes.data);
      setIdeas(ideasRes.data || []);
      setCollabs(collabsRes.data || []);
      setBadges(badgesRes.data || []);
    } catch {
      /* silent */
    } finally {
      setLoading(false);
    }
  }, [targetId, isOwnProfile]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return (
    <div className="profile-page">
      <div className="profile-hero">
        <div className="page-wrapper"><ProfileSkeleton /></div>
      </div>
    </div>
  );
  if (!profile) return (
    <div style={{ textAlign:'center', padding:'80px 24px', color:'var(--text-secondary)' }}>
      <p style={{ fontSize:'2.5rem' }}>😕</p>
      <h2>User not found</h2>
    </div>
  );

  const levelMeta = getLevelMeta(profile.level);
  const xpProgress = getXpProgress(profile.xp || 0, profile.level);
  const earnedMap = {};
  badges.forEach((b) => {
    earnedMap[b.type] = b;
  });

  const memberSince = profile.createdAt
    ? new Date(profile.createdAt).toLocaleDateString(undefined, { month: 'long', year: 'numeric' })
    : '—';

  return (
    <div className="profile-page">
      <div className="profile-hero">
        <div className="page-wrapper profile-hero__inner">
          <div
            className="profile-avatar"
            style={{ background: `linear-gradient(135deg, ${levelMeta.color}, var(--accent-secondary))` }}
          >
            {profile.name?.charAt(0).toUpperCase()}
          </div>

          <div className="profile-info">
            <div className="profile-info__top">
              <h1 className="profile-info__name">{profile.name}</h1>
            </div>
            <div className="profile-info__meta">
              <span>👤 Member since {memberSince}</span>
              {profile.streakDays > 0 && (
                <span className="profile-streak">🔥 {profile.streakDays} day streak</span>
              )}
              <span className="profile-idea-count">💡 {ideas.length} idea{ideas.length !== 1 ? 's' : ''}</span>
            </div>

            <div className="profile-xp-section">
              <div className="profile-xp-bar-track">
                <div
                  className="profile-xp-bar-fill"
                  style={{
                    width: `${xpProgress.pct}%`,
                    background: `linear-gradient(90deg, ${levelMeta.color}aa, ${levelMeta.color})`,
                  }}
                />
              </div>
              <span className="profile-xp-label">
                {xpProgress.current} / {xpProgress.nextThreshold} XP
                {xpProgress.nextLevel !== 'MAX' && <> to <strong>{xpProgress.nextLevel}</strong></>}
                {xpProgress.nextLevel === 'MAX' && <> — Maximum Level Reached! 🏆</>}
              </span>
            </div>
          </div>

          <div
            className="profile-level-badge-lg"
            style={{
              borderColor: levelMeta.color,
              color: levelMeta.color,
              boxShadow: `0 0 28px ${levelMeta.color}55`,
            }}
          >
            <div className="profile-level-badge-lg__icon">
              {profile.level === 'Newcomer'  && '🌱'}
              {profile.level === 'Thinker'   && '💭'}
              {profile.level === 'Innovator' && '⚡'}
              {profile.level === 'Visionary' && '🔮'}
              {profile.level === 'Legend'    && '👑'}
            </div>
            <div className="profile-level-badge-lg__name">{profile.level || 'Newcomer'}</div>
            <div className="profile-level-badge-lg__xp">{profile.xp || 0} XP</div>
          </div>
        </div>
      </div>

      <div className="page-wrapper profile-body">
        <section className="profile-section" aria-labelledby="badges-heading">
          <h2 id="badges-heading" className="profile-section__title">🏅 Badge wall</h2>
          <p className="profile-section__hint">Hover an earned badge to see the date.</p>
          <div className="badge-wall">
            {ALL_BADGES.map((b) => (
              <BadgeTile key={b.type} badge={b} earned={earnedMap[b.type]} />
            ))}
          </div>
        </section>

        <section className="profile-section" aria-labelledby="ideas-heading">
          <h2 id="ideas-heading" className="profile-section__title">
            💡 {isOwnProfile ? 'My ideas' : `${profile.name}'s ideas`}
          </h2>
          <div className="profile-ideas-list">
            {ideas.length === 0 ? (
              <div className="profile-empty">
                <p>No ideas posted yet.</p>
                {isOwnProfile && (
                  <Link to="/" className="btn-primary" style={{ textDecoration: 'none', marginTop: 12 }}>
                    Post your first idea →
                  </Link>
                )}
              </div>
            ) : (
              ideas.map((idea) => <IdeaRow key={idea.id} idea={idea} />)
            )}
          </div>
        </section>

        <section className="profile-section" aria-labelledby="collabs-heading">
          <h2 id="collabs-heading" className="profile-section__title">
            🤝 {isOwnProfile ? 'My collaborations' : `Ideas ${profile.name} collaborates on`}
          </h2>
          <div className="profile-ideas-list">
            {collabs.length === 0 ? (
              <div className="profile-empty">
                <p>No collaborations yet.</p>
              </div>
            ) : (
              collabs.map((idea) => <IdeaRow key={idea.id} idea={idea} />)
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
