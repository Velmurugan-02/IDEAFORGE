import React, { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { ideaAPI } from '../services/api';
import wsService from '../services/wsService';
import { LeaderboardSkeleton } from '../components/Skeleton';
import './Leaderboard.css';

/* ── Rank change indicator ── */
const RANK_CHANGE = {
  UP:   { icon: '▲', cls: 'rank-change--up',   label: 'Moved up'   },
  DOWN: { icon: '▼', cls: 'rank-change--down',  label: 'Moved down' },
  SAME: { icon: '—', cls: 'rank-change--same',  label: 'No change'  },
  NEW:  { icon: '✨', cls: 'rank-change--new',   label: 'New entry'  },
};

/* ── Compute rank change vs previous snapshot ── */
function computeChanges(newList, prevMap) {
  return newList.map((idea, idx) => {
    const rank    = idx + 1;
    const prevRank = prevMap[idea.id];
    let change = 'NEW';
    if (prevRank !== undefined) {
      if (rank < prevRank)      change = 'UP';
      else if (rank > prevRank) change = 'DOWN';
      else                      change = 'SAME';
    }
    return { ...idea, rank, change };
  });
}

/* ── Medal for top 3 ── */
function RankCell({ rank }) {
  if (rank === 1) return <span className="rank-medal rank-medal--gold">🥇</span>;
  if (rank === 2) return <span className="rank-medal rank-medal--silver">🥈</span>;
  if (rank === 3) return <span className="rank-medal rank-medal--bronze">🥉</span>;
  return <span className="rank-num">#{rank}</span>;
}

/* ── Traction mini-bar ── */
function TractionBar({ score }) {
  const pct = Math.min(100, Math.max(0, Number(score || 0)));
  return (
    <div className="traction-bar-wrap">
      <div className="traction-bar-track">
        <div className="traction-bar-fill" style={{ width: `${pct}%` }} />
      </div>
      <span className="traction-score-val">⚡ {Number(score || 0).toFixed(1)}</span>
    </div>
  );
}

/* ================================================================
   LeaderboardPage
   ================================================================ */
export default function LeaderboardPage() {
  const [rows,    setRows]    = useState([]);    // { idea + rank + change }
  const [loading, setLoading] = useState(true);
  const [flash,   setFlash]   = useState({});    // ideaId → 'up' | 'down'
  const [wsLive,  setWsLive]  = useState(false); // whether WS is connected
  const prevRanksRef          = useRef({});       // ideaId → rank

  /* ── Initial fetch ── */
  useEffect(() => {
    ideaAPI.getAll({ sort: 'trending', size: 20, page: 0 })
      .then(({ data }) => {
        const list = Array.isArray(data) ? data : (data.content ?? []);
        const withRanks = list.map((idea, i) => ({ ...idea, rank: i + 1, change: 'SAME' }));
        setRows(withRanks);
        const rankMap = {};
        withRanks.forEach((r) => { rankMap[r.id] = r.rank; });
        prevRanksRef.current = rankMap;
      })
      .catch(() => toast.error('Failed to load leaderboard'))
      .finally(() => setLoading(false));
  }, []);

  /* ── WS subscription ── */
  useEffect(() => {
    let sub;
    wsService.subscribe('/topic/leaderboard', (payload) => {
      const newList = Array.isArray(payload) ? payload : payload?.ideas ?? [];
      if (newList.length === 0) return;

      setWsLive(true);
      const updated = computeChanges(newList, prevRanksRef.current);

      // Compute which ideas changed rank for flash animation
      const flashMap = {};
      updated.forEach((r) => {
        if      (r.change === 'UP')   flashMap[r.id] = 'up';
        else if (r.change === 'DOWN') flashMap[r.id] = 'down';
      });

      setRows(updated);
      setFlash(flashMap);

      // Update prevRanks
      const newRankMap = {};
      updated.forEach((r) => { newRankMap[r.id] = r.rank; });
      prevRanksRef.current = newRankMap;

      // Clear flash after 1 second
      setTimeout(() => setFlash({}), 1000);
    }).then((s) => { sub = s; });
    return () => wsService.unsubscribe(sub);
  }, []);

  if (loading) {
    return (
      <div className="lb-page">
        <div className="lb-hero">
          <div className="page-wrapper lb-hero__inner">
            <h1 className="lb-hero__title">🏆 Live <span className="gradient-text">Leaderboard</span></h1>
          </div>
        </div>
        <div className="page-wrapper lb-body">
          <LeaderboardSkeleton rows={10} />
        </div>
      </div>
    );
  }

  return (
    <div className="lb-page">
      {/* ── Hero ── */}
      <div className="lb-hero">
        <div className="page-wrapper lb-hero__inner">
          <div>
            <h1 className="lb-hero__title">
              🏆 Live <span className="gradient-text">Leaderboard</span>
            </h1>
            <p className="lb-hero__sub">
              Ideas ranked by real-time traction score — updated every minute
              {wsLive && <span className="lb-live-badge">● LIVE</span>}
            </p>
          </div>
          <Link to="/battle" className="btn-primary" style={{ textDecoration:'none', flexShrink:0 }}>
            ⚔️ Battle Arena
          </Link>
        </div>
      </div>

      <div className="page-wrapper lb-body">
        {/* ── Top 3 podium ── */}
        {rows.length >= 3 && (
          <div className="lb-podium">
            {/* 2nd place */}
            <div className="lb-podium-item lb-podium-item--2">
              <div className="lb-podium-medal">🥈</div>
              <Link to={`/ideas/${rows[1].id}`} className="lb-podium-title">{rows[1].title}</Link>
              <p className="lb-podium-owner">{rows[1].authorName}</p>
              <p className="lb-podium-score">⚡ {Number(rows[1].tractionScore || 0).toFixed(1)}</p>
              <div className="lb-podium-block lb-podium-block--2" />
            </div>
            {/* 1st place */}
            <div className="lb-podium-item lb-podium-item--1">
              <div className="lb-podium-medal lb-podium-medal--1">🥇</div>
              <Link to={`/ideas/${rows[0].id}`} className="lb-podium-title lb-podium-title--1">{rows[0].title}</Link>
              <p className="lb-podium-owner">{rows[0].authorName}</p>
              <p className="lb-podium-score lb-podium-score--1">⚡ {Number(rows[0].tractionScore || 0).toFixed(1)}</p>
              <div className="lb-podium-block lb-podium-block--1" />
            </div>
            {/* 3rd place */}
            <div className="lb-podium-item lb-podium-item--3">
              <div className="lb-podium-medal">🥉</div>
              <Link to={`/ideas/${rows[2].id}`} className="lb-podium-title">{rows[2].title}</Link>
              <p className="lb-podium-owner">{rows[2].authorName}</p>
              <p className="lb-podium-score">⚡ {Number(rows[2].tractionScore || 0).toFixed(1)}</p>
              <div className="lb-podium-block lb-podium-block--3" />
            </div>
          </div>
        )}

        {/* ── Table ── */}
        <div className="lb-table-wrapper">
          <table className="lb-table" id="leaderboard-table">
            <thead>
              <tr>
                <th className="lb-th lb-th--rank">Rank</th>
                <th className="lb-th lb-th--title">Idea</th>
                <th className="lb-th lb-th--owner">Owner</th>
                <th className="lb-th lb-th--traction">Traction</th>
                <th className="lb-th lb-th--votes">Votes</th>
                <th className="lb-th lb-th--change">Change</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((idea) => {
                const flashType = flash[idea.id];
                const changeMeta = RANK_CHANGE[idea.change] || RANK_CHANGE.SAME;
                return (
                  <tr
                    key={idea.id}
                    className={`lb-row ${flashType === 'up' ? 'lb-row--flash-up' : ''} ${flashType === 'down' ? 'lb-row--flash-down' : ''}`}
                  >
                    {/* Rank */}
                    <td className="lb-td lb-td--rank">
                      <RankCell rank={idea.rank} />
                    </td>

                    {/* Title */}
                    <td className="lb-td lb-td--title">
                      <Link to={`/ideas/${idea.id}`} className="lb-title-link">
                        {idea.title}
                      </Link>
                      {idea.isPlagiarised && (
                        <span className="badge badge-red" style={{ fontSize: 10, marginLeft: 6 }}>🚫</span>
                      )}
                      {idea.battleStatus === 'WON' && (
                        <span className="badge badge-amber" style={{ fontSize: 10, marginLeft: 6 }}>🏆</span>
                      )}
                      <p className="lb-title-cat">{idea.category}</p>
                    </td>

                    {/* Owner */}
                    <td className="lb-td lb-td--owner">
                      <Link to={`/profile/${idea.userId}`} className="lb-owner-link">
                        {idea.authorName}
                      </Link>
                    </td>

                    {/* Traction */}
                    <td className="lb-td lb-td--traction">
                      <TractionBar score={idea.tractionScore} />
                    </td>

                    {/* Votes */}
                    <td className="lb-td lb-td--votes">
                      <span className="lb-vote-count">{idea.voteCount ?? 0}</span>
                    </td>

                    {/* Change */}
                    <td className="lb-td lb-td--change">
                      <span
                        className={`rank-change ${changeMeta.cls}`}
                        title={changeMeta.label}
                        aria-label={changeMeta.label}
                      >
                        {changeMeta.icon}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {rows.length === 0 && (
            <div style={{ textAlign:'center', padding:'60px 24px', color:'var(--text-muted)' }}>
              <p>No ideas yet — post the first one!</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
