import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { ideaAPI, voteAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { showLevelUpToast } from '../components/LevelUpToast';
import PostIdeaModal from '../components/PostIdeaModal';
import { FeedSkeletonGrid } from '../components/Skeleton';
import wsService from '../services/wsService';
import './IdeaFeed.css';

/* ── Constants ── */
const CATEGORIES = ['All', 'FinTech', 'HealthTech', 'EdTech', 'SaaS', 'Other'];
const SORT_OPTIONS = [
  { value: 'trending', label: '🔥 Trending' },
  { value: 'newest',   label: '🆕 Newest'   },
];

const LEVEL_COLORS = {
  Newcomer:  '#94a3b8',
  Thinker:   '#06b6d4',
  Innovator: '#7c3aed',
  Visionary: '#f59e0b',
  Legend:    '#f43f5e',
};

const VISIBILITY_META = {
  PUBLIC:      { label: '🌍 Public',      cls: 'badge-cyan'   },
  PRIVATE:     { label: '🔒 Private',     cls: 'badge-red'    },
  INVITE_ONLY: { label: '🔗 Invite Only', cls: 'badge-amber'  },
};

/* ─────────────────────────────────────────────────────────────────
   IdeaCard
───────────────────────────────────────────────────────────────── */
function IdeaCard({ idea, onVote }) {
  const [voting,     setVoting]     = useState(false);
  const [liveCount,  setLiveCount]  = useState(idea.voteCount || 0);
  const navigate = useNavigate();

  /* Subscribe to live vote updates for this specific idea */
  useEffect(() => {
    let sub;
    wsService.subscribe(`/topic/idea/${idea.id}/votes`, (payload) => {
      if (payload?.voteCount !== undefined) setLiveCount(payload.voteCount);
    }).then((s) => { sub = s; });
    return () => wsService.unsubscribe(sub);
  }, [idea.id]);

  /* Sync if parent refreshes the idea */
  useEffect(() => { setLiveCount(idea.voteCount || 0); }, [idea.voteCount]);

  const handleVote = async (e, type) => {
    e.stopPropagation();
    setVoting(true);
    try {
      const { data } = await voteAPI.cast(idea.id, { type });
      showLevelUpToast(data);
      onVote(idea.id);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Vote failed');
    } finally {
      setVoting(false);
    }
  };

  /* Traction score — 0-100 normalised to a visual bar (score is float) */
  const tractionPct = Math.min(100, Math.max(0, Number(idea.tractionScore || 0)));
  const tractionLabel = Number(idea.tractionScore || 0).toFixed(1);

  const visMeta = VISIBILITY_META[idea.visibility] || VISIBILITY_META.PUBLIC;
  const authorInitial = idea.authorName?.charAt(0).toUpperCase() || '?';
  const levelColor  = LEVEL_COLORS[idea.authorLevel] || '#7c3aed';

  return (
    <article
      className="idea-card fade-in"
      onClick={() => navigate(`/ideas/${idea.id}`)}
      role="link"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && navigate(`/ideas/${idea.id}`)}
      aria-label={`Idea: ${idea.title}`}
    >
      {/* ── Header row ── */}
      <div className="idea-card__header">
        <span className="badge badge-purple idea-card__cat">{idea.category}</span>

        <div className="idea-card__badges">
          {idea.isPlagiarised && (
            <span className="badge badge-red" title="Flagged as potential plagiarism">🚫 Flagged</span>
          )}
          {idea.battleStatus === 'WON' && (
            <span className="badge badge-amber">🏆 Winner</span>
          )}
          <span className={`badge ${visMeta.cls} idea-card__vis`}>{visMeta.label}</span>
        </div>
      </div>

      {/* ── Title ── */}
      <h3 className="idea-card__title">{idea.title}</h3>

      {/* ── Pitch ── */}
      <p className="idea-card__pitch">{idea.pitch}</p>

      {/* ── Traction score bar ── */}
      <div className="idea-card__traction-wrap">
        <div className="idea-card__traction-bar-track">
          <div
            className="idea-card__traction-bar-fill"
            style={{ width: `${tractionPct}%` }}
          />
        </div>
        <span className="idea-card__traction-label">⚡ {tractionLabel}</span>
      </div>

      {/* ── Author ── */}
      <div className="idea-card__author">
        <div
          className="idea-card__avatar"
          style={{ background: `linear-gradient(135deg, ${levelColor}, var(--accent-secondary))` }}
        >
          {authorInitial}
        </div>
        <div className="idea-card__author-info">
          <span className="idea-card__author-name">{idea.authorName}</span>
          {idea.authorLevel && (
            <span className="idea-card__author-level" style={{ color: levelColor }}>
              {idea.authorLevel}
            </span>
          )}
        </div>
        <span className="idea-card__date">
          {new Date(idea.createdAt).toLocaleDateString(undefined, { month:'short', day:'numeric' })}
        </span>
      </div>

      {/* ── Footer ── */}
      <div className="idea-card__footer" onClick={(e) => e.stopPropagation()}>
        <div className="idea-card__vote-group">
          <button
            className="idea-card__vote-btn idea-card__vote-btn--up"
            onClick={(e) => handleVote(e, 'UP')}
            disabled={voting}
            title="Upvote"
            aria-label="Upvote"
          >
            👍
          </button>
          <span className={`idea-card__vote-count ${liveCount > 0 ? 'idea-card__vote-count--pos' : ''}`}>
            {liveCount}
          </span>
          <button
            className="idea-card__vote-btn idea-card__vote-btn--down"
            onClick={(e) => handleVote(e, 'DOWN')}
            disabled={voting}
            title="Downvote"
            aria-label="Downvote"
          >
            👎
          </button>
        </div>

        <Link
          to={`/ideas/${idea.id}`}
          className="idea-card__comment-btn"
          onClick={(e) => e.stopPropagation()}
          aria-label={`${idea.commentCount || 0} comments`}
        >
          💬 {idea.commentCount || 0}
        </Link>
      </div>
    </article>
  );
}

/* ─────────────────────────────────────────────────────────────────
   IdeaFeedPage
───────────────────────────────────────────────────────────────── */
export default function IdeaFeedPage() {
  const [ideas,       setIdeas]       = useState([]);
  const [loading,     setLoading]     = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [category,    setCategory]    = useState('All');
  const [sort,        setSort]        = useState('trending');
  const [page,        setPage]        = useState(0);
  const [hasMore,     setHasMore]     = useState(true);
  const [showModal,   setShowModal]   = useState(false);
  const { updateUser }                = useAuth();

  /* ── Fetch ideas ── */
  const fetchIdeas = useCallback(async (cat, srt, pg = 0, append = false) => {
    if (append) setLoadingMore(true);
    else        setLoading(true);
    try {
      const params = { page: pg, size: 12, sort: srt };
      if (cat && cat !== 'All') params.category = cat;
      const { data } = await ideaAPI.getAll(params);
      const content  = Array.isArray(data) ? data : (data.content ?? []);
      const last     = data.last ?? content.length < 12;
      setIdeas((prev) => append ? [...prev, ...content] : content);
      setHasMore(!last);
    } catch {
      toast.error('Failed to load ideas');
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, []);

  /* Re-fetch whenever filter/sort changes */
  useEffect(() => {
    setPage(0);
    fetchIdeas(category, sort, 0, false);
  }, [category, sort, fetchIdeas]);

  const loadMore = () => {
    const next = page + 1;
    setPage(next);
    fetchIdeas(category, sort, next, true);
  };

  /* Refresh a single card after vote */
  const refreshCard = useCallback(async (id) => {
    try {
      const { data } = await ideaAPI.getById(id);
      setIdeas((prev) => prev.map((i) => (i.id === id ? data : i)));
    } catch { /* silent */ }
  }, []);

  /* Prepend freshly posted idea */
  const handlePosted = useCallback((newIdea) => {
    setIdeas((prev) => [newIdea, ...prev]);
  }, []);

  return (
    <div className="feed-page">
      {/* ── Hero ── */}
      <div className="feed-hero">
        <div className="page-wrapper feed-hero__inner">
          <div className="feed-hero__text">
            <h1 className="feed-hero__title">
              💡 Idea <span className="gradient-text">Feed</span>
            </h1>
            <p className="feed-hero__sub">
              Discover, vote, and battle the world's next big startup ideas
            </p>
          </div>
          <button
            id="post-idea-btn"
            className="btn-primary feed-hero__cta pulse-glow"
            onClick={() => setShowModal(true)}
          >
            + Post Idea
          </button>
        </div>
      </div>

      <div className="page-wrapper feed-body">
        {/* ── Controls row ── */}
        <div className="feed-controls">
          {/* Category filter */}
          <div className="feed-filters" role="group" aria-label="Filter by category">
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                id={`filter-${cat.toLowerCase()}`}
                className={`feed-filter-btn ${category === cat ? 'feed-filter-btn--active' : ''}`}
                onClick={() => setCategory(cat)}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* Sort toggle */}
          <div className="feed-sort" role="group" aria-label="Sort order">
            {SORT_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                id={`sort-${opt.value}`}
                className={`feed-sort-btn ${sort === opt.value ? 'feed-sort-btn--active' : ''}`}
                onClick={() => setSort(opt.value)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* ── Grid ── */}
        {loading ? (
          <FeedSkeletonGrid count={8} />
        ) : ideas.length === 0 ? (
          <div className="feed-empty">
            <div className="feed-empty__icon">🌌</div>
            <h3 className="feed-empty__title">No ideas yet — be the first to post!</h3>
            <p className="feed-empty__sub">Share your startup vision and earn XP from the community.</p>
            <button className="btn-primary" onClick={() => setShowModal(true)}>
              + Post the first idea
            </button>
          </div>
        ) : (
          <>
            <div className="idea-grid">
              {ideas.map((idea) => (
                <IdeaCard key={idea.id} idea={idea} onVote={refreshCard} />
              ))}
            </div>

            {hasMore && (
              <div className="feed-load-more">
                <button
                  id="load-more-btn"
                  className="btn-secondary"
                  onClick={loadMore}
                  disabled={loadingMore}
                >
                  {loadingMore
                    ? <><span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> Loading…</>
                    : 'Load more ideas'
                  }
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* ── Post Idea Modal ── */}
      {showModal && (
        <PostIdeaModal
          onClose={() => setShowModal(false)}
          onPosted={handlePosted}
        />
      )}
    </div>
  );
}
