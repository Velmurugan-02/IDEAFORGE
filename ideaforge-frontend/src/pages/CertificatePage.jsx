import React, { useEffect, useState, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ideaAPI } from '../services/api';
import './Certificate.css';

/* ── Format: "13 May 2026, 14:25 IST" ── */
function formatIST(isoString) {
  if (!isoString) return '—';
  const date = new Date(isoString);
  // IST = UTC + 5:30
  const istOffset = 5.5 * 60 * 60 * 1000;
  const istDate   = new Date(date.getTime() + istOffset);
  const months    = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const day   = String(istDate.getUTCDate()).padStart(2, '0');
  const month = months[istDate.getUTCMonth()];
  const year  = istDate.getUTCFullYear();
  const hh    = String(istDate.getUTCHours()).padStart(2, '0');
  const mm    = String(istDate.getUTCMinutes()).padStart(2, '0');
  return `${day} ${month} ${year}, ${hh}:${mm} IST`;
}

export default function CertificatePage() {
  const { id }       = useParams();
  const [cert, setCert]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(false);
  const printRef              = useRef();

  useEffect(() => {
    ideaAPI.getCertificate(id)
      .then(({ data }) => setCert(data))
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [id]);

  const handlePrint = () => window.print();

  if (loading) return (
    <div className="cert-page">
      <div className="page-loader"><div className="spinner" /></div>
    </div>
  );

  if (error || !cert) return (
    <div className="cert-page cert-page--error">
      <div className="cert-error-box">
        <div style={{ fontSize: '3rem', marginBottom: 16 }}>😕</div>
        <h2>Certificate Not Found</h2>
        <p>This idea may not exist, or its certificate hasn't been generated yet.</p>
        <Link to="/" className="btn-secondary" style={{ marginTop: 20, display: 'inline-block', textDecoration: 'none' }}>
          ← Back to Feed
        </Link>
      </div>
    </div>
  );

  return (
    <div className="cert-page">
      {/* Print button — hidden in print mode */}
      <div className="cert-toolbar no-print">
        <Link to={`/ideas/${id}`} className="cert-back-btn">← Back to Idea</Link>
        <button id="download-pdf-btn" className="btn-primary cert-print-btn" onClick={handlePrint}>
          🖨️ Download as PDF
        </button>
      </div>

      {/* Certificate card */}
      <div className="cert-wrapper" ref={printRef} id="certificate-card">
        {/* Outer border frame */}
        <div className="cert-outer-frame">
          <div className="cert-inner-frame">

            {/* Header */}
            <div className="cert-header">
              <div className="cert-logo">⚡</div>
              <h1 className="cert-brand">IdeaForge</h1>
              <p className="cert-brand-sub">Startup Idea Protection & Validation Platform</p>
            </div>

            {/* Divider */}
            <div className="cert-divider">
              <div className="cert-divider__line" />
              <div className="cert-divider__seal">🏅</div>
              <div className="cert-divider__line" />
            </div>

            {/* Certificate heading */}
            <h2 className="cert-heading">Certificate of Original Idea Ownership</h2>
            <p className="cert-subheading">This is to certify that the following idea was originally submitted by its owner</p>

            {/* Idea title */}
            <div className="cert-idea-block">
              <p className="cert-idea-label">ORIGINAL IDEA</p>
              <h3 className="cert-idea-title">"{cert.ideaTitle}"</h3>
              {cert.ideaPitch && (
                <p className="cert-idea-pitch">{cert.ideaPitch}</p>
              )}
            </div>

            {/* Details grid */}
            <div className="cert-details">
              <div className="cert-detail">
                <span className="cert-detail__label">Originally posted by</span>
                <span className="cert-detail__value">{cert.ownerName}</span>
              </div>
              <div className="cert-detail">
                <span className="cert-detail__label">Date and Time</span>
                <span className="cert-detail__value">{formatIST(cert.issuedAt)}</span>
              </div>
              <div className="cert-detail cert-detail--code">
                <span className="cert-detail__label">Certificate Code</span>
                <code className="cert-code">{cert.certificateCode}</code>
              </div>
            </div>

            {/* Footer declaration */}
            <div className="cert-declaration">
              <div className="cert-declaration__line" />
              <p className="cert-declaration__text">
                This certificate serves as proof of <strong>first submission</strong> on the IdeaForge platform.
                The timestamp and certificate code are immutable and cryptographically verifiable.
              </p>
              <div className="cert-declaration__line" />
            </div>

            {/* Seal row */}
            <div className="cert-seal-row">
              <div className="cert-seal">
                <div className="cert-seal__circle">
                  <span className="cert-seal__inner">⚡</span>
                </div>
                <p className="cert-seal__label">IdeaForge<br/>Official Seal</p>
              </div>
              <div className="cert-seal-spacer" />
              <div className="cert-seal">
                <div className="cert-seal__circle cert-seal__circle--gold">
                  <span className="cert-seal__inner">🏅</span>
                </div>
                <p className="cert-seal__label">Verified<br/>Original</p>
              </div>
            </div>

            {/* Footer */}
            <p className="cert-footer-note">
              ideaforge.app • Protecting innovators since 2024 • {cert.certificateCode}
            </p>

          </div>
        </div>
      </div>
    </div>
  );
}
