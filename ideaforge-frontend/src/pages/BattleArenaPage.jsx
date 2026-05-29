import React, { useEffect, useState, useRef, useCallback } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { battleAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { showLevelUpToast } from '../components/LevelUpToast';
import wsService from '../services/wsService';
import { BattleSkeleton } from '../components/Skeleton';
import './BattleArena.css';

/* ── Confetti particle ── */
const CONFETTI_COLORS = ['#7c3aed','#06b6d4','#f59e0b','#f43f5e','#10b981','#a78bfa','#67e8f9'];
function randomBetween(a, b) { return a + Math.random() * (b - a); }

function Confetti({ active }) {
  const canvasRef = useRef();

  useEffect(() => {
    if (!active) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx    = canvas.getContext('2d');
    canvas.width  = window.innerWidth;
    canvas.height = window.innerHeight;

    const particles = Array.from({ length: 180 }, () => ({
      x:    randomBetween(0, canvas.width),
      y:    randomBetween(-120, -20),
      vx:   randomBetween(-3, 3),
      vy:   randomBetween(2, 7),
      size: randomBetween(6, 14),
      color: CONFETTI_COLORS[Math.floor(Math.random() * CONFETTI_COLORS.length)],
      angle: Math.random() * Math.PI * 2,
      spin:  randomBetween(-0.15, 0.15),
    }));

    let raf;
    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      particles.forEach((p) => {
        p.x     += p.vx;
        p.y     += p.vy;
        p.angle += p.spin;
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.angle);
        ctx.fillStyle = p.color;
        ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size / 2);
        ctx.restore();
      });
      raf = requestAnimationFrame(draw);
    };
    draw();
    const timer = setTimeout(() => cancelAnimationFrame(raf), 5000);
    return () => { cancelAnimationFrame(raf); clearTimeout(timer); };
  }, [active]);

  if (!active) return null;
  return <canvas ref={canvasRef} className="confetti-canvas" />;
}

/* ── Countdown timer ── */
function Countdown({ seconds }) {
  const [remaining, setRemaining] = useState(seconds);
  useEffect(() => {
    setRemaining(seconds);
    if (seconds <= 0) return;
    const id = setInterval(() => setRemaining((r) => Math.max(0, r - 1)), 1000);
    return () => clearInterval(id);
  }, [seconds]);

  const hh = String(Math.floor(remaining / 3600)).padStart(2, '0');
  const mm = String(Math.floor((remaining % 3600) / 60)).padStart(2, '0');
  const ss = String(remaining % 60).padStart(2, '0');
  const isUrgent = remaining < 3600; // < 1 hour

  return (
    <div className={`battle-countdown ${isUrgent ? 'battle-countdown--urgent' : ''}`}>
      <span className="battle-countdown__icon">⏱</span>
      <span className="battle-countdown__time">{hh}:{mm}:{ss}</span>
      <span className="battle-countdown__label">remaining</span>
    </div>
  );
}

/* ── Split vote bar ── */
function VoteBar({ pctA, pctB }) {
  return (
    <div className="vote-split-bar">
      <div className="vote-split-bar__side vote-split-bar__side--a" style={{ width: `${pctA}%` }}>
        {pctA >= 15 && <span className="vote-split-bar__pct">{Math.round(pctA)}%</span>}
      </div>
      <div className="vote-split-bar__divider" />
      <div className="vote-split-bar__side vote-split-bar__side--b" style={{ width: `${pctB}%` }}>
        {pctB >= 15 && <span className="vote-split-bar__pct">{Math.round(pctB)}%</span>}
      </div>
    </div>
  );
}

/* ── Idea card in battle ── */
function BattleIdeaCard({ idea, side, voted, hasVoted, onVote, isWinner }) {
  return (
    <div className={`battle-idea-card battle-idea-card--${side} ${isWinner ? 'battle-idea-card--winner' : ''} ${voted ? 'battle-idea-card--voted' : ''}`}>
      {isWinner && <div className="battle-winner-badge">🏆 Winner!</div>}
      {voted && !isWinner && <span className="badge badge-cyan battle-my-vote-badge">✓ Your Vote</span>}

      <span className={`battle-side-label battle-side-label--${side}`}>
        {side === 'a' ? '⚔️ Idea A' : '⚔️ Idea B'}
      </span>
      <h3 className="battle-idea-title">
        <Link to={`/ideas/${idea.id}`} className="battle-idea-link">{idea.title}</Link>
      </h3>
      <p className="battle-idea-pitch">{idea.pitch}</p>
      <p className="battle-idea-owner">by {idea.authorName}</p>

      <button
        className={`btn-primary battle-vote-btn ${side === 'b' ? 'battle-vote-btn--b' : ''}`}
        onClick={() => onVote(idea.id)}
        disabled={hasVoted}
        id={`vote-${side}-btn`}
      >
        {hasVoted ? (voted ? '✓ Voted!' : 'Voting closed') : `Vote for ${side === 'a' ? 'A' : 'B'} 👍`}
      </button>
    </div>
  );
}

/* ── Winner overlay ── */
function WinnerOverlay({ winnerTitle, winnerId, onClose }) {
  return (
    <div className="winner-overlay fade-in">
      <div className="winner-overlay__box">
        <div className="winner-overlay__icon">🏆</div>
        <h2 className="winner-overlay__title">Battle Complete!</h2>
        {winnerTitle
          ? <p className="winner-overlay__winner">"{winnerTitle}" wins this battle!</p>
          : <p className="winner-overlay__winner">It's a draw — both ideas fought bravely!</p>
        }
        <div className="winner-overlay__actions">
          {winnerId && (
            <Link to={`/ideas/${winnerId}`} className="btn-primary" style={{ textDecoration:'none' }}>
              View Winner →
            </Link>
          )}
          <button className="btn-secondary" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}

/* ================================================================
   BattleArenaPage
   ================================================================ */
export default function BattleArenaPage() {
  const [battles, setBattles]     = useState([]);
  const [loading, setLoading]     = useState(true);
  const [liveData, setLiveData]   = useState({}); // battleId → WS payload
  const [voted, setVoted]         = useState({}); // battleId → ideaId voted for
  const [winner, setWinner]       = useState(null); // { winnerTitle, winnerId }
  const [confetti, setConfetti]   = useState(false);
  const { updateUser }            = useAuth();
  const subsRef                   = useRef([]);

  /* ── Load active battles ── */
  useEffect(() => {
    battleAPI.getActive()
      .then(({ data }) => setBattles(data))
      .catch(() => toast.error('Failed to load battles'))
      .finally(() => setLoading(false));
    return () => subsRef.current.forEach((s) => wsService.unsubscribe(s));
  }, []);

  /* ── Subscribe to each battle's WS topic ── */
  useEffect(() => {
    if (battles.length === 0) return;
    subsRef.current.forEach((s) => wsService.unsubscribe(s));
    subsRef.current = [];

    battles.forEach((b) => {
      wsService.subscribe(`/topic/battle/${b.id}`, (payload) => {
        setLiveData((prev) => ({ ...prev, [b.id]: payload }));
        if (payload.completed) {
          setWinner({ winnerTitle: payload.winnerTitle, winnerId: payload.winnerId });
          setConfetti(true);
          setTimeout(() => setConfetti(false), 5500);
        }
      }).then((sub) => { subsRef.current.push(sub); });
    });
  }, [battles]);

  /* ── Vote ── */
  const handleVote = useCallback(async (battleId, ideaId) => {
    try {
      const { data } = await battleAPI.vote(battleId, { ideaId });
      showLevelUpToast(data, updateUser);
      setVoted((prev) => ({ ...prev, [battleId]: ideaId }));
      toast.success('Vote cast! ⚔️ +5 XP');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Vote failed');
    }
  }, [updateUser]);

  if (loading) {
    return (
      <div className="battle-page">
        <div className="battle-hero">
          <div className="page-wrapper battle-hero__inner">
            <h1 className="battle-hero__title">⚔️ Battle <span className="gradient-text">Arena</span></h1>
          </div>
        </div>
        <div className="page-wrapper battle-body">
          <BattleSkeleton />
        </div>
      </div>
    );
  }

  return (
    <div className="battle-page">
      <Confetti active={confetti} />

      {/* ── Header ── */}
      <div className="battle-hero">
        <div className="page-wrapper battle-hero__inner">
          <div>
            <h1 className="battle-hero__title">
              ⚔️ Battle <span className="gradient-text">Arena</span>
            </h1>
            <p className="battle-hero__sub">
              Vote for the better idea. The winner earns the 🏆 Battle Winner badge and <strong>+500 XP</strong>.
            </p>
          </div>
          <Link to="/leaderboard" className="btn-secondary" style={{ textDecoration:'none', flexShrink:0 }}>
            🏆 Leaderboard
          </Link>
        </div>
      </div>

      <div className="page-wrapper battle-body">

        {/* ── No battles ── */}
        {battles.length === 0 ? (
          <div className="battle-empty battle-empty--illustrated">
            <div className="battle-empty__art" aria-hidden>
              <div className="battle-empty__arena-ring" />
              <div className="battle-empty__swords">⚔️</div>
            </div>
            <h3 className="battle-empty__title">No active battle</h3>
            <p className="battle-empty__sub">The arena is quiet. When an admin starts the next matchup, vote here to crown a winner.</p>
            <Link to="/" className="btn-secondary" style={{ textDecoration:'none', marginTop:16 }}>
              ← Browse ideas
            </Link>
          </div>
        ) : battles.map((battle) => {
          const live    = liveData[battle.id];
          const pctA    = live?.percentA  ?? battle.percentA;
          const pctB    = live?.percentB  ?? battle.percentB;
          const votesA  = live?.votesA    ?? battle.votesA;
          const votesB  = live?.votesB    ?? battle.votesB;
          const total   = live?.totalVotes ?? (battle.votesA + battle.votesB);
          const userVotedIdeaId = voted[battle.id];
          const hasVoted = !!userVotedIdeaId;

          return (
            <div key={battle.id} className="battle-card fade-in">
              {/* Meta row */}
              <div className="battle-card__meta">
                <span className="badge badge-amber">⚔️ Active Battle</span>
                <Countdown seconds={battle.timeRemainingSeconds} />
                <span className="battle-total-votes">{total} votes cast</span>
              </div>

              {/* VS layout */}
              <div className="battle-vs-layout">
                <BattleIdeaCard
                  idea={battle.ideaA}
                  side="a"
                  voted={userVotedIdeaId === battle.ideaA.id}
                  hasVoted={hasVoted}
                  onVote={(ideaId) => handleVote(battle.id, ideaId)}
                  isWinner={battle.winnerId === battle.ideaA.id}
                />

                <div className="battle-vs-center">
                  <div className="battle-vs-label">VS</div>
                  <div className="battle-vs-scores">
                    <span className="battle-vs-score battle-vs-score--a">{votesA}</span>
                    <span className="battle-vs-score-sep">:</span>
                    <span className="battle-vs-score battle-vs-score--b">{votesB}</span>
                  </div>
                </div>

                <BattleIdeaCard
                  idea={battle.ideaB}
                  side="b"
                  voted={userVotedIdeaId === battle.ideaB.id}
                  hasVoted={hasVoted}
                  onVote={(ideaId) => handleVote(battle.id, ideaId)}
                  isWinner={battle.winnerId === battle.ideaB.id}
                />
              </div>

              {/* Split vote bar */}
              <VoteBar pctA={pctA} pctB={pctB} />

              {/* Pct labels below bar */}
              <div className="battle-pct-row">
                <span className="battle-pct-label battle-pct-label--a">{Math.round(pctA)}% — {battle.ideaA.title}</span>
                <span className="battle-pct-label battle-pct-label--b">{battle.ideaB.title} — {Math.round(pctB)}%</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* ── Winner overlay ── */}
      {winner && (
        <WinnerOverlay
          winnerTitle={winner.winnerTitle}
          winnerId={winner.winnerId}
          onClose={() => setWinner(null)}
        />
      )}
    </div>
  );
}
