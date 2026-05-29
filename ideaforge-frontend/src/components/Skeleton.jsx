import React from 'react';
import './Skeleton.css';

/** Shimmer block — use className for width/height variants */
export function SkeletonBlock({ className = '', style }) {
  return <div className={`skeleton-block ${className}`.trim()} style={style} aria-hidden />;
}

export function IdeaCardSkeleton() {
  return (
    <div className="skeleton-card glass-card">
      <SkeletonBlock className="skeleton-card__line skeleton-card__line--short" />
      <SkeletonBlock className="skeleton-card__line skeleton-card__line--title" />
      <SkeletonBlock className="skeleton-card__line" />
      <SkeletonBlock className="skeleton-card__line" />
      <div className="skeleton-card__footer">
        <SkeletonBlock className="skeleton-card__pill" />
        <SkeletonBlock className="skeleton-card__pill skeleton-card__pill--sm" />
      </div>
    </div>
  );
}

export function FeedSkeletonGrid({ count = 8 }) {
  return (
    <div className="idea-grid" aria-busy="true" aria-label="Loading ideas">
      {Array.from({ length: count }, (_, i) => (
        <IdeaCardSkeleton key={i} />
      ))}
    </div>
  );
}

export function LeaderboardSkeleton({ rows = 8 }) {
  return (
    <div className="skeleton-lb" aria-busy="true" aria-label="Loading leaderboard">
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="skeleton-lb__row glass-card">
          <SkeletonBlock className="skeleton-lb__rank" />
          <div className="skeleton-lb__main">
            <SkeletonBlock className="skeleton-card__line skeleton-card__line--short" />
            <SkeletonBlock className="skeleton-card__line" />
          </div>
          <SkeletonBlock className="skeleton-lb__score" />
        </div>
      ))}
    </div>
  );
}

export function HallOfFameSkeleton() {
  return (
    <div className="skeleton-hof" aria-busy="true" aria-label="Loading hall of fame">
      <SkeletonBlock className="skeleton-hof__title" />
      <div className="skeleton-hof__grid">
        {Array.from({ length: 6 }, (_, i) => (
          <div key={i} className="skeleton-card glass-card skeleton-hof__card">
            <SkeletonBlock className="skeleton-card__line skeleton-card__line--title" />
            <SkeletonBlock className="skeleton-card__line" />
          </div>
        ))}
      </div>
    </div>
  );
}

export function ProfileSkeleton() {
  return (
    <div className="skeleton-profile" aria-busy="true" aria-label="Loading profile">
      <div className="skeleton-profile__hero glass-card">
        <SkeletonBlock className="skeleton-profile__avatar" />
        <div className="skeleton-profile__info">
          <SkeletonBlock className="skeleton-card__line skeleton-card__line--title" />
          <SkeletonBlock className="skeleton-card__line" />
          <SkeletonBlock className="skeleton-profile__bar" />
        </div>
      </div>
      {Array.from({ length: 4 }, (_, i) => (
        <SkeletonBlock key={i} className="skeleton-profile__row" />
      ))}
    </div>
  );
}

export function CollabSkeleton() {
  return (
    <div className="skeleton-collab" aria-busy="true">
      {Array.from({ length: 3 }, (_, i) => (
        <div key={i} className="skeleton-card glass-card skeleton-collab__card">
          <SkeletonBlock className="skeleton-card__line skeleton-card__line--title" />
          <SkeletonBlock className="skeleton-card__line" />
        </div>
      ))}
    </div>
  );
}

export function BattleSkeleton() {
  return (
    <div className="skeleton-battle glass-card" aria-busy="true" aria-label="Loading battle">
      <SkeletonBlock className="skeleton-battle__meta" />
      <div className="skeleton-battle__vs">
        <SkeletonBlock className="skeleton-battle__side" />
        <SkeletonBlock className="skeleton-battle__side" />
      </div>
    </div>
  );
}

export function AdminReportSkeleton() {
  return (
    <div className="skeleton-card glass-card admin-skel-row">
      <div className="admin-skel-row__grid">
        <SkeletonBlock className="skeleton-card__line skeleton-card__line--title" />
        <SkeletonBlock className="skeleton-card__line skeleton-card__line--title" />
      </div>
      <SkeletonBlock className="skeleton-card__line" />
    </div>
  );
}
