import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { ideaAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { showLevelUpToast } from './LevelUpToast';
import './PostIdeaModal.css';

const CATEGORIES = ['FinTech', 'HealthTech', 'EdTech', 'SaaS', 'Other'];

const VISIBILITY_OPTIONS = [
  {
    value: 'PUBLIC',
    label: '🌍 Public',
    desc: 'Anyone on IdeaForge can see and vote on your idea.',
  },
  {
    value: 'PRIVATE',
    label: '🔒 Private',
    desc: 'Only you can see this idea. Good for drafts.',
  },
  {
    value: 'INVITE_ONLY',
    label: '🔗 Invite Only',
    desc: 'Share via a secret link. Others need the link to view.',
  },
];

const TERMS_TEXT =
  'I confirm this is my original idea. I understand that copying another user\'s ' +
  'idea violates IdeaForge\'s community guidelines and may result in account suspension.';

const EMPTY_FORM = {
  title: '',
  pitch: '',
  problem: '',
  targetAudience: '',
  category: 'SaaS',
  visibility: 'PUBLIC',
  agreedToTerms: false,
};

/* ================================================================
   PostIdeaModal
   Props:
     onClose()          — close the modal
     onPosted(idea)     — called with the new idea after successful post
   ================================================================ */
export default function PostIdeaModal({ onClose, onPosted }) {
  const [step, setStep]           = useState(1);          // 1 | 2 | 3
  const [form, setForm]           = useState(EMPTY_FORM);
  const [dupResult, setDupResult] = useState(null);       // DuplicateCheckResponse | null
  const [checking, setChecking]   = useState(false);
  const [posting, setPosting]     = useState(false);
  const [dupWarned, setDupWarned] = useState(false);      // user saw the warning
  const { updateUser }            = useAuth();

  // Close on Escape key
  useEffect(() => {
    const handle = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handle);
    return () => window.removeEventListener('keydown', handle);
  }, [onClose]);

  // Lock body scroll when open
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = ''; };
  }, []);

  const patch = (field, value) => setForm((prev) => ({ ...prev, [field]: value }));

  /* ── Step 1 → Step 2: run duplicate check ── */
  const handleNext = useCallback(async () => {
    setChecking(true);
    setDupResult(null);
    try {
      const { data } = await ideaAPI.checkDuplicate({ title: form.title, pitch: form.pitch });
      setDupResult(data);
      if (!data.similarityFound) {
        // Unique — skip the warning and jump straight to Step 3
        setTimeout(() => setStep(3), 900); // brief "✅ unique" display
      } else {
        setDupWarned(true);
        setStep(2);
      }
    } catch {
      toast.error('Duplicate check failed. Continuing anyway.');
      setStep(3);
    } finally {
      setChecking(false);
    }
  }, [form.title, form.pitch]);

  /* Transition: unique found → show green flash on step 2 before advancing */
  useEffect(() => {
    if (dupResult && !dupResult.similarityFound) {
      setStep(2); // briefly show green screen
    }
  }, [dupResult]);

  /* ── Step 3: submit idea ── */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setPosting(true);
    try {
      const { data } = await ideaAPI.create({ ...form, agreedToTerms: true });
      showLevelUpToast(data.levelUpEvent, updateUser);
      onPosted(data);

      if (dupWarned) {
        toast.success(
          'Idea posted! Remember to consider collaborating if a similar idea exists 🤝',
          { duration: 6000 }
        );
      } else {
        toast.success('Idea posted! 💡 +50 XP earned');
      }
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to post idea');
    } finally {
      setPosting(false);
    }
  };

  const titleLen = form.title.length;
  const pitchLen = form.pitch.length;

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal-panel fade-in">

        {/* ── Header ── */}
        <div className="modal-header">
          <div className="modal-header__left">
            <span className="modal-logo">⚡</span>
            <div>
              <h2 className="modal-title">Post a New Idea</h2>
              <div className="modal-steps">
                {[1, 2, 3].map((n) => (
                  <div key={n} className={`modal-step ${step >= n ? 'modal-step--done' : ''} ${step === n ? 'modal-step--active' : ''}`}>
                    <div className="modal-step__dot">{step > n ? '✓' : n}</div>
                    <span className="modal-step__label">
                      {n === 1 ? 'Details' : n === 2 ? 'Check' : 'Confirm'}
                    </span>
                  </div>
                ))}
                <div className="modal-step-line" />
              </div>
            </div>
          </div>
          <button className="modal-close" onClick={onClose} aria-label="Close">✕</button>
        </div>

        {/* ══════════════════════════════════════
            STEP 1 — Idea Details
        ══════════════════════════════════════ */}
        {step === 1 && (
          <form
            className="modal-body"
            onSubmit={(e) => { e.preventDefault(); handleNext(); }}
          >
            <div className="modal-form-grid">
              {/* Title */}
              <div className="form-group modal-col-full">
                <label className="form-label" htmlFor="idea-title">
                  Title <span className="modal-required">*</span>
                </label>
                <input
                  id="idea-title"
                  className="form-input"
                  type="text"
                  maxLength={200}
                  placeholder="Your brilliant startup idea…"
                  value={form.title}
                  onChange={(e) => patch('title', e.target.value)}
                  required
                  autoFocus
                />
                <span className={`modal-char-count ${titleLen > 180 ? 'modal-char-count--warn' : ''}`}>
                  {titleLen}/200
                </span>
              </div>

              {/* Category */}
              <div className="form-group">
                <label className="form-label" htmlFor="idea-category">Category</label>
                <select
                  id="idea-category"
                  className="form-input"
                  value={form.category}
                  onChange={(e) => patch('category', e.target.value)}
                >
                  {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
                </select>
              </div>

              {/* Pitch */}
              <div className="form-group modal-col-full">
                <label className="form-label" htmlFor="idea-pitch">
                  One-liner pitch <span className="modal-required">*</span>
                </label>
                <input
                  id="idea-pitch"
                  className="form-input"
                  type="text"
                  maxLength={300}
                  placeholder="Describe your idea in one compelling sentence"
                  value={form.pitch}
                  onChange={(e) => patch('pitch', e.target.value)}
                  required
                />
                <span className={`modal-char-count ${pitchLen > 270 ? 'modal-char-count--warn' : ''}`}>
                  {pitchLen}/300
                </span>
              </div>

              {/* Problem */}
              <div className="form-group modal-col-full">
                <label className="form-label" htmlFor="idea-problem">
                  Problem it solves <span className="modal-required">*</span>
                </label>
                <textarea
                  id="idea-problem"
                  className="form-input"
                  rows={3}
                  placeholder="What pain point does this address? Be specific."
                  value={form.problem}
                  onChange={(e) => patch('problem', e.target.value)}
                  required
                />
              </div>

              {/* Target audience */}
              <div className="form-group modal-col-full">
                <label className="form-label" htmlFor="idea-audience">
                  Target audience <span className="modal-required">*</span>
                </label>
                <input
                  id="idea-audience"
                  className="form-input"
                  placeholder="Who is this for? e.g. 'Small business owners in Southeast Asia'"
                  value={form.targetAudience}
                  onChange={(e) => patch('targetAudience', e.target.value)}
                  required
                />
              </div>

              {/* Visibility */}
              <div className="form-group modal-col-full">
                <label className="form-label">Visibility</label>
                <div className="visibility-grid">
                  {VISIBILITY_OPTIONS.map((opt) => (
                    <label
                      key={opt.value}
                      className={`visibility-card ${form.visibility === opt.value ? 'visibility-card--active' : ''}`}
                    >
                      <input
                        type="radio"
                        name="visibility"
                        value={opt.value}
                        checked={form.visibility === opt.value}
                        onChange={() => patch('visibility', opt.value)}
                        style={{ display: 'none' }}
                      />
                      <span className="visibility-card__label">{opt.label}</span>
                      <span className="visibility-card__desc">{opt.desc}</span>
                    </label>
                  ))}
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
              <button
                type="submit"
                className="btn-primary"
                disabled={checking || !form.title.trim() || !form.pitch.trim() || !form.problem.trim() || !form.targetAudience.trim()}
              >
                {checking
                  ? <><span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> Checking…</>
                  : 'Next — Check for Duplicates →'
                }
              </button>
            </div>
          </form>
        )}

        {/* ══════════════════════════════════════
            STEP 2 — Duplicate Check Result
        ══════════════════════════════════════ */}
        {step === 2 && (
          <div className="modal-body modal-check-body">

            {/* Still checking */}
            {checking && (
              <div className="modal-check-loading">
                <div className="modal-check-spinner">
                  <div className="spinner" style={{ width: 48, height: 48, borderWidth: 4 }} />
                </div>
                <p className="modal-check-label">Checking for similar ideas…</p>
                <p className="modal-check-sub">Scanning {'{'}thousands of ideas{'}'} for similarity</p>
              </div>
            )}

            {/* No duplicate — green flash before auto-advancing */}
            {!checking && dupResult && !dupResult.similarityFound && (
              <div className="modal-check-result modal-check-result--unique fade-in">
                <div className="modal-check-icon">✅</div>
                <h3 className="modal-check-title">Your idea appears to be unique!</h3>
                <p className="modal-check-desc">Great job — no similar ideas were found. Advancing to the final step…</p>
                <div className="modal-check-progress">
                  <div className="modal-check-progress-fill" />
                </div>
              </div>
            )}

            {/* Duplicate found — yellow warning */}
            {!checking && dupResult && dupResult.similarityFound && (
              <div className="modal-check-result modal-check-result--warning fade-in">
                <div className="modal-check-icon">⚠️</div>
                <h3 className="modal-check-title">A similar idea was found</h3>

                <div className="modal-dup-box">
                  <div className="modal-dup-box__header">
                    <span className="modal-dup-box__title">"{dupResult.similarIdeaTitle}"</span>
                    <span className="modal-dup-similarity">
                      {dupResult.similarityScore}% similarity
                    </span>
                  </div>
                  <div className="modal-dup-bar-track">
                    <div
                      className="modal-dup-bar-fill"
                      style={{ width: `${dupResult.similarityScore}%` }}
                    />
                  </div>
                  <p className="modal-dup-desc">
                    Your idea has <strong>{dupResult.similarityScore}%</strong> similarity with an existing idea.
                    You can still post — but consider collaborating with the original poster instead.
                  </p>
                  <Link
                    to={`/ideas/${dupResult.similarIdeaId}`}
                    className="modal-dup-link"
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={onClose}
                  >
                    View Original Idea →
                  </Link>
                </div>

                <div className="modal-footer">
                  <button className="btn-secondary" onClick={() => setStep(1)}>← Edit My Idea</button>
                  <button
                    className="btn-primary"
                    onClick={() => setStep(3)}
                  >
                    Continue Anyway →
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ══════════════════════════════════════
            STEP 3 — Terms & Submit
        ══════════════════════════════════════ */}
        {step === 3 && (
          <form className="modal-body" onSubmit={handleSubmit}>
            {dupWarned && (
              <div className="modal-collab-note">
                💡 <strong>Reminder:</strong> A similar idea exists. Consider reaching out to the original poster to collaborate instead of competing.
              </div>
            )}

            <div className="modal-summary">
              <h3 className="modal-summary__title">{form.title}</h3>
              <p className="modal-summary__pitch">{form.pitch}</p>
              <div className="modal-summary__meta">
                <span className="badge badge-purple">{form.category}</span>
                <span className="badge badge-cyan">{form.visibility}</span>
              </div>
            </div>

            <div className="modal-terms-box">
              <h4 className="modal-terms-heading">📋 Community Terms</h4>
              <p className="modal-terms-text">{TERMS_TEXT}</p>
            </div>

            <label className="modal-terms-check">
              <input
                id="terms-checkbox"
                type="checkbox"
                checked={form.agreedToTerms}
                onChange={(e) => patch('agreedToTerms', e.target.checked)}
                className="modal-terms-input"
                required
              />
              <span className="modal-terms-label">
                I agree to these terms and understand IdeaForge's IP protection policy.
              </span>
            </label>

            <div className="modal-footer">
              <button type="button" className="btn-secondary" onClick={() => setStep(dupWarned ? 2 : 1)}>
                ← Back
              </button>
              <button
                type="submit"
                className="btn-primary pulse-glow"
                disabled={posting || !form.agreedToTerms}
              >
                {posting
                  ? <><span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> Posting…</>
                  : '⚡ Post Idea'
                }
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
