import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { collabAPI } from '../services/api';
import { showLevelUpToast } from '../components/LevelUpToast';
import { useAuth } from '../context/AuthContext';
import { CollabSkeleton } from '../components/Skeleton';
import './CollabRequests.css';

const STATUS_META = {
  PENDING:  { cls: 'badge-amber',  label: '⏳ Pending'  },
  ACCEPTED: { cls: 'badge-green',  label: '✅ Accepted' },
  REJECTED: { cls: 'badge-red',    label: '❌ Rejected' },
};

/* ── Received request card ── */
function ReceivedCard({ req, onAction }) {
  const [acting, setActing] = useState(null); // 'accept' | 'reject'
  const statusMeta = STATUS_META[req.status] || STATUS_META.PENDING;
  const { updateUser } = useAuth();

  const handleAccept = async () => {
    setActing('accept');
    try {
      const { data } = await collabAPI.accept(req.id);
      showLevelUpToast(data.levelUpEvent, updateUser);
      toast.success(`You accepted ${req.requesterName}'s request! 🤝 +75 XP to both`);
      onAction(req.id, 'ACCEPTED');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to accept');
    } finally { setActing(null); }
  };

  const handleReject = async () => {
    setActing('reject');
    try {
      await collabAPI.reject(req.id);
      toast.success('Request rejected');
      onAction(req.id, 'REJECTED');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to reject');
    } finally { setActing(null); }
  };

  return (
    <div className={`cr-card cr-card--received ${req.status !== 'PENDING' ? 'cr-card--resolved' : ''}`}>
      <div className="cr-card__header">
        <div className="cr-card__avatar">
          {req.requesterName?.charAt(0).toUpperCase() || '?'}
        </div>
        <div className="cr-card__header-info">
          <span className="cr-card__name">{req.requesterName}</span>
          <span className="cr-card__idea-link">
            wants to collaborate on{' '}
            <Link to={`/ideas/${req.ideaId}`} className="cr-idea-link">
              {req.ideaTitle}
            </Link>
          </span>
        </div>
        <span className={`badge ${statusMeta.cls} cr-card__status`}>{statusMeta.label}</span>
      </div>

      {req.message && (
        <div className="cr-card__message">
          <span className="cr-card__message-label">💬 Message</span>
          <p className="cr-card__message-text">"{req.message}"</p>
        </div>
      )}

      <div className="cr-card__footer">
        <span className="cr-card__date">
          Sent{' '}
          {new Date(req.createdAt).toLocaleString(undefined, {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </span>

        {req.status === 'PENDING' && (
          <div className="cr-card__actions">
            <button
              id={`accept-btn-${req.id}`}
              className="btn-success cr-action-btn"
              onClick={handleAccept}
              disabled={!!acting}
            >
              {acting === 'accept'
                ? <><span className="spinner" style={{ width:14, height:14, borderWidth:2 }} /> Accepting…</>
                : '✓ Accept'
              }
            </button>
            <button
              id={`reject-btn-${req.id}`}
              className="btn-danger cr-action-btn"
              onClick={handleReject}
              disabled={!!acting}
            >
              {acting === 'reject'
                ? <><span className="spinner" style={{ width:14, height:14, borderWidth:2 }} /> Rejecting…</>
                : '✕ Reject'
              }
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

/* ── Sent request card ── */
function SentCard({ req }) {
  const statusMeta = STATUS_META[req.status] || STATUS_META.PENDING;
  return (
    <div className={`cr-card cr-card--sent ${req.status !== 'PENDING' ? 'cr-card--resolved' : ''}`}>
      <div className="cr-card__header">
        <div className="cr-card__header-info">
          <span className="cr-card__idea-link">
            Request to collaborate on{' '}
            <Link to={`/ideas/${req.ideaId}`} className="cr-idea-link">
              {req.ideaTitle}
            </Link>
          </span>
          <span className="cr-card__owner-hint">owned by {req.ownerName}</span>
        </div>
        <span className={`badge ${statusMeta.cls} cr-card__status`}>{statusMeta.label}</span>
      </div>

      {req.message && (
        <div className="cr-card__message">
          <span className="cr-card__message-label">Your message</span>
          <p className="cr-card__message-text">"{req.message}"</p>
        </div>
      )}

      <div className="cr-card__footer">
        <span className="cr-card__date">
          Sent{' '}
          {new Date(req.createdAt).toLocaleString(undefined, {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </span>
        {req.status === 'ACCEPTED' && (
          <Link to={`/ideas/${req.ideaId}`} className="btn-secondary cr-action-btn" style={{ textDecoration:'none' }}>
            View Idea →
          </Link>
        )}
      </div>
    </div>
  );
}

/* ================================================================
   CollabRequestsPage
   ================================================================ */
export default function CollabRequestsPage() {
  const [tab,      setTab]      = useState('received');
  const [received, setReceived] = useState([]);
  const [sent,     setSent]     = useState([]);
  const [loading,  setLoading]  = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [recRes, sentRes] = await Promise.all([
        collabAPI.getReceived(),
        collabAPI.getSent(),
      ]);
      setReceived(recRes.data || []);
      setSent(sentRes.data || []);
    } catch {
      toast.error('Failed to load collaboration requests');
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  /* Update status in-place after accept/reject */
  const handleAction = useCallback((id, newStatus) => {
    setReceived((prev) => prev.map((r) => r.id === id ? { ...r, status: newStatus } : r));
  }, []);

  const pendingCount = received.filter((r) => r.status === 'PENDING').length;

  return (
    <div className="cr-page">
      {/* ── Hero ── */}
      <div className="cr-hero">
        <div className="page-wrapper cr-hero__inner">
          <div>
            <h1 className="cr-hero__title">
              🤝 Collaboration <span className="gradient-text">Requests</span>
            </h1>
            <p className="cr-hero__sub">
              Manage incoming collaboration requests and track the ones you've sent.
            </p>
          </div>
          {pendingCount > 0 && (
            <div className="cr-pending-badge">
              <span className="cr-pending-badge__count">{pendingCount}</span>
              <span className="cr-pending-badge__label">pending</span>
            </div>
          )}
        </div>
      </div>

      <div className="page-wrapper cr-body">
        {/* ── Tabs ── */}
        <div className="cr-tabs">
          <button
            id="cr-tab-received"
            className={`cr-tab-btn ${tab === 'received' ? 'cr-tab-btn--active' : ''}`}
            onClick={() => setTab('received')}
          >
            📥 Received
            {pendingCount > 0 && <span className="cr-tab-count">{pendingCount}</span>}
          </button>
          <button
            id="cr-tab-sent"
            className={`cr-tab-btn ${tab === 'sent' ? 'cr-tab-btn--active' : ''}`}
            onClick={() => setTab('sent')}
          >
            📤 Sent
            {sent.length > 0 && <span className="cr-tab-count cr-tab-count--neutral">{sent.length}</span>}
          </button>
        </div>

        {/* ── Content ── */}
        {loading ? (
          <CollabSkeleton />
        ) : (
          <div className="cr-list">
            {tab === 'received' && (
              received.length === 0
                ? <div className="cr-empty"><span>📭</span><p>No collaboration requests received yet.</p></div>
                : received.map((r) => <ReceivedCard key={r.id} req={r} onAction={handleAction} />)
            )}
            {tab === 'sent' && (
              sent.length === 0
                ? <div className="cr-empty"><span>📭</span><p>You haven't sent any collaboration requests yet.</p></div>
                : sent.map((r) => <SentCard key={r.id} req={r} />)
            )}
          </div>
        )}
      </div>
    </div>
  );
}
