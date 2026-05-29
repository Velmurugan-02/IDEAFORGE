import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { ideaAPI, commentAPI, voteAPI, collabAPI, reportAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { showLevelUpToast } from '../components/LevelUpToast';
import wsService from '../services/wsService';
import './IdeaDetail.css';

/* ── Constants ── */
const LEVEL_COLORS = {
  Newcomer: '#94a3b8', Thinker: '#06b6d4',
  Innovator: '#7c3aed', Visionary: '#f59e0b', Legend: '#f43f5e',
};

const COMMENT_TAGS = ['Support', 'Question', 'Challenge'];

const TAG_META = {
  SUPPORT:     { cls: 'badge-green',  icon: '✅', label: 'Support'   },
  QUESTION:    { cls: 'badge-cyan',   icon: '❓', label: 'Question'  },
  CHALLENGE:   { cls: 'badge-amber',  icon: '⚠️', label: 'Challenge' },
  SUGGESTION:  { cls: 'badge-purple', icon: '💡', label: 'Suggestion'},
  APPRECIATION:{ cls: 'badge-green',  icon: '🙌', label: 'Thanks'   },
};

const VISIBILITY_META = {
  PUBLIC:      { label: '🌍 Public',      cls: 'badge-cyan'  },
  PRIVATE:     { label: '🔒 Private',     cls: 'badge-red'   },
  INVITE_ONLY: { label: '🔗 Invite Only', cls: 'badge-amber' },
};

const COMMENT_FILTER_TABS = [
  { key: 'ALL',       label: 'All'       },
  { key: 'SUPPORT',   label: '✅ Support' },
  { key: 'QUESTION',  label: '❓ Question'},
  { key: 'CHALLENGE', label: '⚠️ Challenge'},
];

/* ================================================================
   CollabModal
   ================================================================ */
function CollabModal({ ideaId, onClose }) {
  const [msg, setMsg]         = useState('');
  const [sending, setSending] = useState(false);

  const handleSend = async (e) => {
    e.preventDefault();
    setSending(true);
    try {
      await collabAPI.create(ideaId, { message: msg });
      toast.success('Collaboration request sent! 🤝');
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to send request');
    } finally { setSending(false); }
  };

  return (
    <div className="detail-modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="detail-modal-box fade-in">
        <div className="detail-modal-header">
          <h3>🤝 Request to Collaborate</h3>
          <button className="detail-modal-close" onClick={onClose}>✕</button>
        </div>
        <p className="detail-modal-desc">
          Explain why you'd like to collaborate. If accepted, you'll become a co-owner of this idea.
        </p>
        <form onSubmit={handleSend}>
          <textarea
            className="form-input"
            rows={4}
            placeholder="Hi! I'd love to collaborate because…"
            value={msg}
            onChange={(e) => setMsg(e.target.value)}
            required
            autoFocus
          />
          <div className="detail-modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={sending || !msg.trim()}>
              {sending ? 'Sending…' : 'Send Request 🤝'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

/* ================================================================
   ReportModal
   ================================================================ */
function ReportModal({ ideaId, onClose }) {
  const [form, setForm]       = useState({ originalIdeaId: '', reason: '' });
  const [sending, setSending] = useState(false);

  const handleSend = async (e) => {
    e.preventDefault();
    setSending(true);
    try {
      await reportAPI.submit(ideaId, {
        originalIdeaId: Number(form.originalIdeaId),
        reason: form.reason,
      });
      toast.success('Report submitted — our team will review it shortly 🛡️');
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit report');
    } finally { setSending(false); }
  };

  return (
    <div className="detail-modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="detail-modal-box fade-in">
        <div className="detail-modal-header">
          <h3>🚨 Report as Plagiarism</h3>
          <button className="detail-modal-close" onClick={onClose}>✕</button>
        </div>
        <p className="detail-modal-desc">
          If you believe this idea was copied from an existing one, provide the original idea's ID and reason.
          False reports may result in account action.
        </p>
        <form onSubmit={handleSend} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div className="form-group">
            <label className="form-label" htmlFor="original-idea-id">Original Idea ID *</label>
            <input
              id="original-idea-id"
              className="form-input"
              type="number"
              min={1}
              placeholder="Enter the ID of the original idea"
              value={form.originalIdeaId}
              onChange={(e) => setForm({ ...form, originalIdeaId: e.target.value })}
              required
            />
            <span style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
              You can find an idea's ID in its URL: /ideas/<strong>42</strong>
            </span>
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="report-reason">Reason *</label>
            <textarea
              id="report-reason"
              className="form-input"
              rows={3}
              placeholder="Describe how this idea copies the original…"
              value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })}
              required
            />
          </div>
          <div className="detail-modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-danger" disabled={sending || !form.originalIdeaId || !form.reason.trim()}>
              {sending ? 'Submitting…' : '🚨 Submit Report'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

/* ================================================================
   VotePanel (right panel top)
   ================================================================ */
function VotePanel({ idea, currentVote, onVote }) {
  const [voting, setVoting]     = useState(false);
  const [liveCount, setLiveCount] = useState(idea.voteCount || 0);

  useEffect(() => { setLiveCount(idea.voteCount || 0); }, [idea.voteCount]);

  const handleVote = async (type) => {
    if (voting) return;
    // Optimistic update
    const prevCount  = liveCount;
    const prevVote   = currentVote;
    const delta      = type === 'UP' ? 1 : -1;
    const wasThisType = currentVote === type;
    setLiveCount((c) => wasThisType ? c - delta : c + delta);

    setVoting(true);
    try {
      await onVote(type);
    } catch {
      setLiveCount(prevCount);  // rollback
    } finally {
      setVoting(false);
    }
  };

  return (
    <div className="vote-panel">
      <button
        id="vote-up-btn"
        className={`vote-btn vote-btn--up ${currentVote === 'UP' ? 'vote-btn--active-up' : ''}`}
        onClick={() => handleVote('UP')}
        disabled={voting}
        aria-label="Upvote"
      >
        <span className="vote-btn__icon">👍</span>
        <span className="vote-btn__label">Upvote</span>
      </button>

      <div className="vote-count">
        <span className="vote-count__num">{liveCount}</span>
        <span className="vote-count__label">votes</span>
      </div>

      <button
        id="vote-down-btn"
        className={`vote-btn vote-btn--down ${currentVote === 'DOWN' ? 'vote-btn--active-down' : ''}`}
        onClick={() => handleVote('DOWN')}
        disabled={voting}
        aria-label="Downvote"
      >
        <span className="vote-btn__icon">👎</span>
        <span className="vote-btn__label">Downvote</span>
      </button>
    </div>
  );
}

/* ================================================================
   CommentItem
   ================================================================ */
function CommentItem({ comment, isOwner, onAnswer }) {
  const [answering, setAnswering] = useState(false);
  const meta = TAG_META[comment.tag] || TAG_META.QUESTION;

  const handleAnswer = async () => {
    setAnswering(true);
    try { await onAnswer(comment.id); }
    finally { setAnswering(false); }
  };

  return (
    <div className={`comment-item ${comment.answered ? 'comment-item--answered' : ''} ${comment.tag === 'CHALLENGE' && !comment.answered ? 'comment-item--challenge' : ''}`}>
      <div className="comment-item__header">
        <div className="comment-item__avatar">
          {comment.userName?.charAt(0).toUpperCase() || '?'}
        </div>
        <div className="comment-item__author">
          <span className="comment-item__user">{comment.userName}</span>
          <span className="comment-item__date">
            {new Date(comment.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
          </span>
        </div>
        <div className="comment-item__tags">
          <span className={`badge ${meta.cls}`}>{meta.icon} {meta.label}</span>
          {comment.answered && <span className="badge badge-green">✓ Answered</span>}
        </div>
      </div>

      <p className="comment-item__content">{comment.content}</p>

      {comment.tag === 'CHALLENGE' && !comment.answered && (
        <div className="comment-item__challenge-banner">
          ⚠️ Awaiting response from idea owner
        </div>
      )}

      {isOwner && comment.tag === 'CHALLENGE' && !comment.answered && (
        <button
          className="btn-secondary comment-item__answer-btn"
          onClick={handleAnswer}
          disabled={answering}
        >
          {answering ? 'Marking…' : '✓ Mark as Answered'}
        </button>
      )}
    </div>
  );
}

/* ================================================================
   CommentsPanel (right panel bottom)
   ================================================================ */
function CommentsPanel({ ideaId, isOwner }) {
  const [comments, setComments]       = useState([]);
  const [loading, setLoading]         = useState(true);
  const [filterTag, setFilterTag]     = useState('ALL');
  const [form, setForm]               = useState({ content: '', tag: 'QUESTION' });
  const [posting, setPosting]         = useState(false);
  const { updateUser }                = useAuth();
  const bottomRef                     = useRef(null);

  const fetchComments = useCallback(async () => {
    try {
      const { data } = await commentAPI.getAll(ideaId, filterTag !== 'ALL' ? filterTag : null);
      setComments(data);
    } catch { /* silent */ }
    finally { setLoading(false); }
  }, [ideaId, filterTag]);

  useEffect(() => { fetchComments(); }, [fetchComments]);

  const handlePost = async (e) => {
    e.preventDefault();
    setPosting(true);
    try {
      const { data } = await commentAPI.post(ideaId, { ...form, tag: form.tag.toUpperCase() });
      setComments((prev) => [...prev, data]);
      showLevelUpToast(data.levelUpEvent, updateUser);
      setForm({ content: '', tag: 'QUESTION' });
      toast.success('Comment posted! 💬');
      // Scroll new comment into view
      setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 100);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to post comment');
    } finally { setPosting(false); }
  };

  const handleAnswer = async (commentId) => {
    try {
      const { data } = await commentAPI.answer(commentId);
      setComments((prev) => prev.map((c) => c.id === commentId ? { ...c, answered: data.answered } : c));
      showLevelUpToast(data.levelUpEvent, updateUser);
      toast.success('Marked as answered ✓ +100 XP');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed');
    }
  };

  const filtered = filterTag === 'ALL' ? comments : comments.filter((c) => c.tag === filterTag);

  return (
    <div className="comments-panel">
      <h3 className="comments-panel__title">
        💬 Comments
        <span className="comments-panel__count">{comments.length}</span>
      </h3>

      {/* Tag filter tabs */}
      <div className="comments-filter">
        {COMMENT_FILTER_TABS.map((t) => (
          <button
            key={t.key}
            className={`comments-filter-btn ${filterTag === t.key ? 'comments-filter-btn--active' : ''}`}
            onClick={() => setFilterTag(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Post form */}
      <form className="comment-form" onSubmit={handlePost}>
        <textarea
          id="comment-input"
          className="form-input"
          rows={3}
          placeholder="Share your thoughts, ask a question, or challenge this idea…"
          value={form.content}
          onChange={(e) => setForm({ ...form, content: e.target.value })}
          required
        />
        <div className="comment-form__row">
          <div className="comment-tag-group">
            {COMMENT_TAGS.map((tag) => (
              <label
                key={tag}
                className={`comment-tag-btn ${form.tag.toUpperCase() === tag.toUpperCase() ? 'comment-tag-btn--active' : ''}`}
              >
                <input
                  type="radio"
                  name="comment-tag"
                  value={tag}
                  checked={form.tag.toUpperCase() === tag.toUpperCase()}
                  onChange={() => setForm({ ...form, tag: tag.toUpperCase() })}
                  style={{ display: 'none' }}
                />
                {TAG_META[tag.toUpperCase()]?.icon} {tag}
              </label>
            ))}
          </div>
          <button
            type="submit"
            id="post-comment-btn"
            className="btn-primary"
            disabled={posting || !form.content.trim()}
          >
            {posting
              ? <><span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} /></>
              : 'Post'
            }
          </button>
        </div>
      </form>

      {/* Comment list */}
      <div className="comment-list">
        {loading ? (
          <div className="page-loader" style={{ minHeight: 120 }}><div className="spinner" /></div>
        ) : filtered.length === 0 ? (
          <div className="comment-empty">
            <p>No {filterTag !== 'ALL' ? filterTag.toLowerCase() + ' ' : ''}comments yet.</p>
          </div>
        ) : (
          filtered.map((c) => (
            <CommentItem
              key={c.id}
              comment={c}
              isOwner={isOwner}
              onAnswer={handleAnswer}
            />
          ))
        )}
        <div ref={bottomRef} />
      </div>
    </div>
  );
}

/* ================================================================
   IdeaDetailPage — main
   ================================================================ */
export default function IdeaDetailPage() {
  const { id }    = useParams();
  const navigate  = useNavigate();
  const { currentUser, updateUser } = useAuth();

  const [idea,        setIdea]        = useState(null);
  const [cert,        setCert]        = useState(null);
  const [currentVote, setCurrentVote] = useState(null); // 'UP' | 'DOWN' | null
  const [loading,     setLoading]     = useState(true);
  const [showCollab,  setShowCollab]  = useState(false);
  const [showReport,  setShowReport]  = useState(false);
  const [genInvite,   setGenInvite]   = useState(false);
  const [inviteLink,  setInviteLink]  = useState(null);

  /* ── Load idea + cert + user's own vote ── */
  useEffect(() => {
    let wsSub;

    const load = async () => {
      try {
        const [ideaRes, voteRes] = await Promise.all([
          ideaAPI.getById(id),
          currentUser ? voteAPI.getUserVote(id).catch(() => ({ data: null })) : Promise.resolve({ data: null }),
        ]);
        setIdea(ideaRes.data);
        setCurrentVote(voteRes.data); // 'UP' | 'DOWN' | null

        // Load certificate silently (ignore 404)
        ideaAPI.getCertificate(id)
          .then(({ data }) => setCert(data))
          .catch(() => {});
      } catch {
        toast.error('Idea not found');
      } finally {
        setLoading(false);
      }
    };

    load();

    // WebSocket — live vote count
    wsService.subscribe(`/topic/idea/${id}/votes`, (payload) => {
      if (payload?.voteCount !== undefined) {
        setIdea((prev) => prev ? { ...prev, voteCount: payload.voteCount } : prev);
      }
    }).then((s) => { wsSub = s; });

    return () => wsService.unsubscribe(wsSub);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  /* ── Vote handler ── */
  const handleVote = useCallback(async (type) => {
    try {
      const { data } = await voteAPI.cast(id, { type });
      showLevelUpToast(data, updateUser);
      // Toggle currentVote state
      setCurrentVote((prev) => prev === type ? null : type);
      // Refresh idea for accurate server count
      const { data: fresh } = await ideaAPI.getById(id);
      setIdea(fresh);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Vote failed');
      throw err; // propagate to VotePanel rollback
    }
  }, [id, updateUser]);

  /* ── Generate invite link ── */
  const handleGenerateInvite = async () => {
    setGenInvite(true);
    try {
      const { data } = await ideaAPI.createInvite(id);
      const link = `${window.location.origin}/ideas/${id}?inviteToken=${data.inviteToken}`;
      setInviteLink(link);
      await navigator.clipboard.writeText(link);
      toast.success('Invite link copied to clipboard! 🔗');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to generate invite link');
    } finally {
      setGenInvite(false);
    }
  };

  if (loading) return <div className="page-loader"><div className="spinner" /></div>;
  if (!idea)   return (
    <div style={{ textAlign: 'center', padding: '80px 24px', color: 'var(--text-secondary)' }}>
      <p style={{ fontSize: '2rem', marginBottom: 12 }}>😕</p>
      <h2>Idea not found</h2>
      <button className="btn-secondary" style={{ marginTop: 16 }} onClick={() => navigate('/')}>← Back to Feed</button>
    </div>
  );

  const isOwner      = currentUser?.id === idea.userId;
  const isCollab     = idea.collaborators?.length > 0;
  const visMeta      = VISIBILITY_META[idea.visibility] || VISIBILITY_META.PUBLIC;
  const levelColor   = LEVEL_COLORS[idea.authorLevel] || '#7c3aed';
  const authorInitial = idea.authorName?.charAt(0).toUpperCase() || '?';

  return (
    <div className="idea-detail-page">
      {/* ── Back nav ── */}
      <div className="detail-topbar page-wrapper">
        <button className="detail-back-btn" onClick={() => navigate(-1)}>← Back</button>
        <div className="detail-topbar-badges">
          <span className="badge badge-purple">{idea.category}</span>
          <span className={`badge ${visMeta.cls}`}>{visMeta.label}</span>
          {idea.battleStatus === 'WON' && <span className="badge badge-amber">🏆 Battle Winner</span>}
        </div>
      </div>

      {/* ── Two-panel layout ── */}
      <div className="detail-layout page-wrapper">

        {/* ══════════════════════════════════════
            LEFT PANEL — Content
        ══════════════════════════════════════ */}
        <div className="detail-left fade-in">

          {/* Plagiarism warning banner */}
          {idea.isPlagiarised && (
            <div className="detail-plagiarism-banner">
              🚫 <strong>Flagged for plagiarism.</strong> This idea has been identified as potentially copied and is under review.
            </div>
          )}

          {/* Title */}
          <h1 className="detail-title">{idea.title}</h1>

          {/* Pitch */}
          <p className="detail-pitch">{idea.pitch}</p>

          {/* Author card */}
          <div className="detail-author-card">
            <Link to={`/profile/${idea.userId}`} className="detail-author-link">
              <div
                className="detail-author-avatar"
                style={{ background: `linear-gradient(135deg, ${levelColor}, var(--accent-secondary))` }}
              >
                {authorInitial}
              </div>
              <div className="detail-author-info">
                <span className="detail-author-name">{idea.authorName}</span>
                {idea.authorLevel && (
                  <span className="detail-author-level" style={{ color: levelColor }}>
                    {idea.authorLevel}
                  </span>
                )}
              </div>
            </Link>
            <span className="detail-posted-date">
              Posted {new Date(idea.createdAt).toLocaleDateString(undefined, { month: 'long', day: 'numeric', year: 'numeric' })}
            </span>
          </div>

          {/* Collaborators */}
          {idea.collaborators?.length > 0 && (
            <div className="detail-collabs">
              🤝 <span className="detail-collabs__label">Co-created with</span>
              {idea.collaborators.map((name, i) => (
                <span key={i} className="detail-collab-chip">{name}</span>
              ))}
            </div>
          )}

          {/* ── Idea content sections ── */}
          <div className="detail-sections">
            {idea.problem && (
              <div className="detail-section">
                <h3 className="detail-section-label">🎯 Problem it solves</h3>
                <p className="detail-section-text">{idea.problem}</p>
              </div>
            )}
            {idea.targetAudience && (
              <div className="detail-section">
                <h3 className="detail-section-label">👥 Target Audience</h3>
                <p className="detail-section-text">{idea.targetAudience}</p>
              </div>
            )}
          </div>

          {/* ── Ownership proof ── */}
          <div className="detail-cert-panel">
            <div className="detail-cert-panel__icon">🛡️</div>
            <div className="detail-cert-panel__body">
              <p className="detail-cert-panel__title">Intellectual Property Protection</p>
              <p className="detail-cert-panel__text">
                Posted on <strong>{new Date(idea.createdAt).toLocaleDateString()}</strong> by <strong>{idea.authorName}</strong>
                {cert && (
                  <> — Certificate <code className="detail-cert-code">#{cert.certificateCode}</code></>
                )}
              </p>
            </div>
            <Link
              to={`/ideas/${id}/certificate`}
              className="detail-cert-link"
              target="_blank"
              rel="noopener noreferrer"
            >
              View Certificate →
            </Link>
          </div>
        </div>

        {/* ══════════════════════════════════════
            RIGHT PANEL — Actions & Comments
        ══════════════════════════════════════ */}
        <div className="detail-right">

          {/* Vote panel */}
          <div className="detail-right-card">
            <VotePanel idea={idea} currentVote={currentVote} onVote={handleVote} />

            {/* Traction score */}
            <div className="detail-traction">
              <span className="detail-traction__label">Traction Score</span>
              <div className="xp-bar-track" style={{ flex: 1 }}>
                <div
                  className="xp-bar-fill"
                  style={{ width: `${Math.min(100, Number(idea.tractionScore || 0))}%` }}
                />
              </div>
              <span className="detail-traction__val">⚡ {Number(idea.tractionScore || 0).toFixed(1)}</span>
            </div>
          </div>

          {/* Actions card */}
          <div className="detail-right-card">
            <h4 className="detail-actions-title">Actions</h4>
            <div className="detail-actions-list">
              {/* Non-owner actions */}
              {!isOwner && (
                <>
                  <button
                    id="collab-request-btn"
                    className="btn-primary detail-action-btn"
                    onClick={() => setShowCollab(true)}
                  >
                    🤝 Request to Collaborate
                  </button>
                  <button
                    id="report-btn"
                    className="btn-danger detail-action-btn"
                    onClick={() => setShowReport(true)}
                  >
                    🚨 Report as Copy
                  </button>
                </>
              )}

              {/* Owner-only actions */}
              {isOwner && idea.visibility === 'INVITE_ONLY' && (
                <button
                  id="invite-link-btn"
                  className="btn-secondary detail-action-btn"
                  onClick={handleGenerateInvite}
                  disabled={genInvite}
                >
                  {genInvite ? 'Generating…' : '🔗 Generate Invite Link'}
                </button>
              )}
              {inviteLink && (
                <div className="detail-invite-box">
                  <p className="detail-invite-box__label">Invite Link (copied!)</p>
                  <code className="detail-invite-box__link">{inviteLink}</code>
                </div>
              )}

              {/* Owner or collaborator */}
              {(isOwner || idea.collaborators?.some(c => c === currentUser?.name)) && (
                <Link
                  id="edit-idea-btn"
                  to={`/ideas/${id}/edit`}
                  className="btn-secondary detail-action-btn"
                  style={{ textDecoration: 'none', textAlign: 'center' }}
                >
                  ✏️ Edit Idea
                </Link>
              )}

              {/* Collab requests link for owner */}
              {isOwner && (
                <Link
                  to="/collab-requests"
                  className="detail-collab-requests-link"
                >
                  Manage collab requests →
                </Link>
              )}
            </div>
          </div>

          {/* Comments panel */}
          <div className="detail-right-card detail-right-card--comments">
            <CommentsPanel ideaId={id} isOwner={isOwner} />
          </div>
        </div>
      </div>

      {/* ── Modals ── */}
      {showCollab  && <CollabModal ideaId={id} onClose={() => setShowCollab(false)} />}
      {showReport  && <ReportModal ideaId={id} onClose={() => setShowReport(false)} />}
    </div>
  );
}
