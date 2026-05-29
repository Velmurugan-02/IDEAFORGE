import React, { useEffect, useState, useCallback } from 'react';
import toast from 'react-hot-toast';
import { Link } from 'react-router-dom';
import { adminAPI, ideaAPI, battleAPI } from '../services/api';
import { AdminReportSkeleton } from '../components/Skeleton';
import './AdminPage.css';

function formatPosted(iso) {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return '—';
  }
}

function winnerLabel(battle) {
  if (!battle?.winnerId) return 'Draw';
  if (battle.ideaA?.id === battle.winnerId) return battle.ideaA.title;
  if (battle.ideaB?.id === battle.winnerId) return battle.ideaB.title;
  return '—';
}

async function fetchPublicIdeasForSelect() {
  const all = [];
  for (let page = 0; page < 8; page += 1) {
    try {
      const { data } = await ideaAPI.getAll({ page, size: 40, sort: 'trending' });
      const content = Array.isArray(data) ? data : (data.content ?? []);
      const last = data.last ?? true;
      content.forEach((idea) => {
        if (idea.visibility === 'PUBLIC' && !idea.isPlagiarised) {
          all.push(idea);
        }
      });
      if (last || content.length === 0) break;
    } catch {
      break;
    }
  }
  const byId = new Map();
  all.forEach((i) => byId.set(i.id, i));
  return [...byId.values()].sort((a, b) => (a.title || '').localeCompare(b.title || ''));
}

export default function AdminPage() {
  const [reports, setReports] = useState([]);
  const [stats, setStats] = useState(null);
  const [publicIdeas, setPublicIdeas] = useState([]);
  const [activeBattles, setActiveBattles] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [battleSubmitting, setBattleSubmitting] = useState(false);
  const [ideaAId, setIdeaAId] = useState('');
  const [ideaBId, setIdeaBId] = useState('');

  const loadBattles = useCallback(async () => {
    try {
      const [act, hist] = await Promise.all([battleAPI.getActive(), battleAPI.getHistory()]);
      setActiveBattles(act.data || []);
      setHistory(hist.data || []);
    } catch {
      toast.error('Failed to load battles');
    }
  }, []);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [repRes, statsRes, ideas] = await Promise.all([
        adminAPI.getReports(),
        adminAPI.getStats(),
        fetchPublicIdeasForSelect(),
      ]);
      setReports(repRes.data || []);
      setStats(statsRes.data);
      setPublicIdeas(ideas);
      await loadBattles();
    } catch {
      toast.error('Failed to load admin data');
    } finally {
      setLoading(false);
    }
  }, [loadBattles]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const removeReport = (id) => {
    setReports((prev) => prev.filter((row) => row.report?.id !== id));
  };

  const handleConfirm = async (id) => {
    try {
      await adminAPI.confirmReport(id);
      toast.success('Plagiarism confirmed — idea flagged.');
      removeReport(id);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Confirm failed');
    }
  };

  const handleDismiss = async (id) => {
    try {
      await adminAPI.dismissReport(id);
      toast.success('Report dismissed.');
      removeReport(id);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Dismiss failed');
    }
  };

  const handleCreateBattle = async (e) => {
    e.preventDefault();
    const a = Number(ideaAId);
    const b = Number(ideaBId);
    if (!a || !b || a === b) {
      toast.error('Choose two different public ideas.');
      return;
    }
    setBattleSubmitting(true);
    try {
      await battleAPI.create({ ideaAId: a, ideaBId: b });
      toast.success('Battle created!');
      setIdeaAId('');
      setIdeaBId('');
      await loadBattles();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not create battle');
    } finally {
      setBattleSubmitting(false);
    }
  };

  return (
    <div className="admin-page">
      <div className="admin-hero page-wrapper">
        <h1 className="admin-hero__title">
          🛡️ Admin <span className="gradient-text">Console</span>
        </h1>
        <p className="admin-hero__sub">Plagiarism moderation, battle control, and platform metrics.</p>
      </div>

      {/* ── Stats ── */}
      <section className="page-wrapper admin-section">
        <h2 className="admin-section__title">📊 Platform stats</h2>
        <div className="admin-stats-grid">
          {loading && !stats ? (
            [1, 2, 3, 4].map((k) => <div key={k} className="admin-stat-card glass-card admin-stat-card--skeleton" />)
          ) : (
            <>
              <div className="admin-stat-card glass-card">
                <span className="admin-stat-card__label">Total users</span>
                <span className="admin-stat-card__value">{stats?.totalUsers ?? 0}</span>
              </div>
              <div className="admin-stat-card glass-card">
                <span className="admin-stat-card__label">Total ideas</span>
                <span className="admin-stat-card__value">{stats?.totalIdeas ?? 0}</span>
              </div>
              <div className="admin-stat-card glass-card">
                <span className="admin-stat-card__label">Total votes</span>
                <span className="admin-stat-card__value">{stats?.totalVotes ?? 0}</span>
              </div>
              <div className="admin-stat-card glass-card">
                <span className="admin-stat-card__label">Battles fought</span>
                <span className="admin-stat-card__value">{stats?.totalBattlesFought ?? 0}</span>
              </div>
            </>
          )}
        </div>
      </section>

      {/* ── Reports ── */}
      <section className="page-wrapper admin-section">
        <h2 className="admin-section__title">🚨 Plagiarism reports (pending)</h2>
        {loading ? (
          <>
            <AdminReportSkeleton />
            <AdminReportSkeleton />
          </>
        ) : reports.length === 0 ? (
          <div className="admin-empty glass-card">
            <span className="admin-empty__icon">✅</span>
            <p>No pending reports.</p>
          </div>
        ) : (
          <div className="admin-reports">
            {reports.map((row) => {
              const rid = row.report?.id;
              const reported = row.reportedIdea;
              const original = row.originalIdea;
              const reporter = row.report?.reporterName || '—';
              return (
                <div key={rid} className="admin-report-card glass-card">
                  <div className="admin-report-grid">
                    <div className="admin-report-col admin-report-col--bad">
                      <span className="admin-report-label">Reported idea</span>
                      <p className="admin-report-title">{reported?.title || '—'}</p>
                      <p className="admin-report-date">Posted {formatPosted(reported?.createdAt)}</p>
                    </div>
                    <div className="admin-report-col admin-report-col--ok">
                      <span className="admin-report-label">Original idea</span>
                      <p className="admin-report-title">{original?.title || '—'}</p>
                      <p className="admin-report-date">Posted {formatPosted(original?.createdAt)}</p>
                    </div>
                  </div>
                  <div className="admin-report-meta">
                    <p>
                      <strong>Reporter:</strong> {reporter}
                    </p>
                    <p>
                      <strong>Reason:</strong> {row.report?.reason || '—'}
                    </p>
                  </div>
                  <div className="admin-report-actions">
                    <button type="button" className="btn-danger" onClick={() => handleConfirm(rid)}>
                      Confirm plagiarism
                    </button>
                    <button type="button" className="btn-secondary admin-btn-muted" onClick={() => handleDismiss(rid)}>
                      Dismiss
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* ── Battles ── */}
      <section className="page-wrapper admin-section admin-section--last">
        <h2 className="admin-section__title">⚔️ Battle management</h2>

        {activeBattles.length > 0 ? (
          <div className="admin-active-battle glass-card">
            <span className="badge badge-amber">Active battle</span>
            <div className="admin-active-battle__ideas">
              <div>
                <p className="admin-active-label">Idea A</p>
                <p className="admin-active-title">{activeBattles[0].ideaA?.title}</p>
                <Link to={`/ideas/${activeBattles[0].ideaA?.id}`} className="admin-mini-link">
                  Open →
                </Link>
              </div>
              <div className="admin-vs">VS</div>
              <div>
                <p className="admin-active-label">Idea B</p>
                <p className="admin-active-title">{activeBattles[0].ideaB?.title}</p>
                <Link to={`/ideas/${activeBattles[0].ideaB?.id}`} className="admin-mini-link">
                  Open →
                </Link>
              </div>
            </div>
            <p className="admin-active-hint">End one battle (wait for timer or resolve) before creating another.</p>
          </div>
        ) : (
          <div className="admin-no-active glass-card">
            <p>No active battle — create one below.</p>
          </div>
        )}

        <form className="admin-battle-form glass-card" onSubmit={handleCreateBattle}>
          <h3 className="admin-battle-form__title">Create new battle</h3>
          <p className="admin-battle-form__hint">Pick two different public ideas. Requires no active battle.</p>
          <div className="admin-battle-form__row">
            <label className="admin-battle-form__label" htmlFor="battle-idea-a">
              Idea A
            </label>
            <select
              id="battle-idea-a"
              className="form-input admin-select"
              value={ideaAId}
              onChange={(e) => setIdeaAId(e.target.value)}
              required
            >
              <option value="">Select…</option>
              {publicIdeas.map((i) => (
                <option key={i.id} value={i.id}>
                  {i.title}
                </option>
              ))}
            </select>
          </div>
          <div className="admin-battle-form__row">
            <label className="admin-battle-form__label" htmlFor="battle-idea-b">
              Idea B
            </label>
            <select
              id="battle-idea-b"
              className="form-input admin-select"
              value={ideaBId}
              onChange={(e) => setIdeaBId(e.target.value)}
              required
            >
              <option value="">Select…</option>
              {publicIdeas.map((i) => (
                <option key={i.id} value={i.id}>
                  {i.title}
                </option>
              ))}
            </select>
          </div>
          <button type="submit" className="btn-primary" disabled={battleSubmitting || activeBattles.length > 0}>
            {battleSubmitting ? 'Creating…' : 'Start battle'}
          </button>
        </form>

        <h3 className="admin-subheading">Past battles</h3>
        {history.length === 0 ? (
          <p className="admin-muted">No completed battles yet.</p>
        ) : (
          <div className="admin-history-table glass-card">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Idea A</th>
                  <th>Idea B</th>
                  <th>Score</th>
                  <th>Winner</th>
                </tr>
              </thead>
              <tbody>
                {history.map((b) => (
                  <tr key={b.id}>
                    <td>{b.id}</td>
                    <td>{b.ideaA?.title}</td>
                    <td>{b.ideaB?.title}</td>
                    <td>
                      {b.votesA} : {b.votesB}
                    </td>
                    <td className="admin-winner-cell">{winnerLabel(b)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
