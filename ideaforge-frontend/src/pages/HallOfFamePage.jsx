import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { hallOfFameAPI } from '../services/api';
import { HallOfFameSkeleton } from '../components/Skeleton';
import './HallOfFame.css';

function weekKey(e) {
  return e.weekStart || e.week_start || '';
}

/* ── Group entries by week start (ISO date string) ── */
function groupByWeek(entries) {
  const map = new Map();
  entries.forEach((e) => {
    const key = weekKey(e);
    if (!key) return;
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(e);
  });
  // Sort each group by rank
  map.forEach((arr) => arr.sort((a, b) => a.rank - b.rank));
  // Return newest week first
  return [...map.entries()].sort((a, b) => b[0].localeCompare(a[0]));
}

function formatWeek(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString(undefined, { day: 'numeric', month: 'long', year: 'numeric' });
}

const RANK_META = {
  1: { medal: '🥇', cls: 'hof-card--gold' },
  2: { medal: '🥈', cls: 'hof-card--silver' },
  3: { medal: '🥉', cls: 'hof-card--bronze' },
};

function HofCard({ entry }) {
  const meta = RANK_META[entry.rank] || {};
  const ideaId = entry.ideaId ?? entry.idea_id;
  const title = entry.ideaTitle ?? entry.idea_title;
  const owner = entry.ownerName ?? entry.owner_name;
  const score = entry.snapshotScore ?? entry.snapshot_score;
  return (
    <Link
      to={`/ideas/${ideaId}`}
      className={`hof-card ${meta.cls || ''}`}
      aria-label={`Rank ${entry.rank}: ${title}`}
    >
      <div className="hof-card__rank">
        <span className="hof-card__medal">{meta.medal || `#${entry.rank}`}</span>
      </div>
      <div className="hof-card__body">
        <h3 className="hof-card__title">{title}</h3>
        <div className="hof-card__footer">
          <span className="hof-card__owner">by {owner}</span>
          <span className="hof-card__score">⚡ {Number(score || 0).toFixed(1)}</span>
        </div>
      </div>
    </Link>
  );
}

export default function HallOfFamePage() {
  const [weeks,   setWeeks]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [empty,   setEmpty]   = useState(false);

  useEffect(() => {
    hallOfFameAPI.getAll()
      .then(({ data }) => {
        if (!data || data.length === 0) { setEmpty(true); return; }
        setWeeks(groupByWeek(data));
      })
      .catch(() => setEmpty(true))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="hof-page">
      {/* ── Hero ── */}
      <div className="hof-hero">
        <div className="page-wrapper hof-hero__inner">
          <div>
            <h1 className="hof-hero__title">
              🏛️ Hall of <span className="gradient-text">Fame</span>
            </h1>
            <p className="hof-hero__sub">
              The best ideas of each week, immortalised on IdeaForge.
            </p>
          </div>
          <Link to="/leaderboard" className="btn-secondary" style={{ textDecoration:'none', flexShrink:0 }}>
            🏆 Live Leaderboard
          </Link>
        </div>
      </div>

      <div className="page-wrapper hof-body">
        {loading && <HallOfFameSkeleton />}

        {!loading && empty && (
          <div className="hof-empty">
            <div className="hof-empty__icon">🏛️</div>
            <h3>No entries yet</h3>
            <p>The Hall of Fame is updated every week. Post an idea to compete!</p>
          </div>
        )}

        {!loading && weeks.map(([wk, entries]) => (
          <section key={wk} className="hof-week fade-in">
            <div className="hof-week__header">
              <div className="hof-week__line" />
              <h2 className="hof-week__title">
                <span className="hof-week__icon">📅</span>
                Week of {formatWeek(wk)}
              </h2>
              <div className="hof-week__line" />
            </div>

            <div className="hof-grid">
              {entries.map((entry) => (
                <HofCard key={entry.id ?? `${wk}-${entry.rank}-${entry.ideaId ?? entry.idea_id}`} entry={entry} />
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}
