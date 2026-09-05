# IdeaForge

> A full-stack startup idea protection and validation platform — where innovators post ideas, communities validate them, and originality is protected.

---

##  Project Overview

**IdeaForge** is a full-stack web application built for startup founders and innovators to post, protect, and validate their startup ideas. It features real-time voting, a custom traction scoring algorithm, duplicate idea detection, ownership certificates, a live Battle Arena, and a gamification engine — all built from scratch without any paid third-party APIs.

- **Type:** Full-Stack Web Application  
- **Developer:** Velmurugan (MCA Graduate)  
- **Duration:** 2026 – Present  
- **Purpose:** Startup idea protection, community validation, and competitive ranking

---

## Tech Stack

### Frontend
| Technology | Purpose |
|---|---|
| React JS 18 | UI framework |
| React Router v6 | Client-side routing |
| Axios | API communication |
| SockJS + STOMP | Real-time WebSocket connection |
| react-hot-toast | Notifications |

### Backend
| Technology | Purpose |
|---|---|
| Java 17 | Core language |
| Spring Boot 3.x | Backend framework |
| Spring Security + JWT | Authentication & authorization |
| Spring Data JPA + Hibernate | ORM / database layer |
| Spring WebSocket + STOMP | Real-time communication |
| Spring @Scheduled | Automated background jobs |
| Maven | Build tool |

### Database
- **MySQL 8.0**
- Tables: `users`, `ideas`, `votes`, `comments`, `battles`, `badges`, `hall_of_fame`, `idea_invites`, `collab_requests`, `plagiarism_reports`, `idea_certificates`, `idea_collaborators`

---

##  Core Features

### 1. User Authentication
- JWT-based registration and login
- BCrypt password hashing
- Role-based access: **User** and **Admin**
- Protected routes on both frontend and backend

---

### 2. Idea Management
- Post startup ideas with: title, pitch, problem statement, target audience, and category
- Visibility control: `PUBLIC` | `PRIVATE` | `INVITE_ONLY` (token-based shareable links)
- Once an idea is set to PUBLIC, it cannot be made private again
- Anti-plagiarism terms agreement before posting

---

### 3. Traction Score Algorithm
A custom weighted formula designed to rank ideas by real engagement:

```
Score = (votes × 3) + (comments × 2) − (hours_since_posted × 0.1) − (unanswered_challenges × 5)
```

- Recalculates **every hour** via Spring `@Scheduled`
- Also recalculates **instantly** after every vote and comment
- Minimum score is always `0` — never negative

---

### 4. Real-Time Voting (WebSocket)
- Every vote instantly updates vote counters on **all connected browsers** — no page refresh
- Backend uses `SimpMessagingTemplate` to broadcast to `/topic/idea/{ideaId}`
- Frontend subscribes using **SockJS + STOMP** client

---

### 5. Comment System
- Three comment types: `SUPPORT` | `QUESTION` | `CHALLENGE`
- `CHALLENGE` comments reduce traction score by 5 if unanswered
- Idea owner can mark challenges as answered → earns +100 XP

---

### 6. Duplicate Idea Detector (Pure Java — No API)
Built using **Jaccard Similarity algorithm** — completely free, no external API:

1. Clean text — remove punctuation, lowercase, filter stop words
2. Calculate `Jaccard Score = (common words ÷ total unique words) × 100`
3. Combined score = `(title similarity × 40%) + (pitch similarity × 60%)`
4. Score > 60% → warning shown | Score > 80% → strong warning shown
5. Never blocks posting — only warns the user

---

### 7. Idea Ownership Certificate
- Auto-generated the moment an idea is posted
- Stores: `certificate_code` (UUID), `idea_id`, `owner_id`, `issued_at` (immutable timestamp)
- Public endpoint — no login required: `GET /api/ideas/{id}/certificate`
- Acts as digital proof of **"first submission"**
- Downloadable as PDF via browser print

---

### 8. Idea Protection System
Four layers of protection:
1. **Plagiarism Reports** — any user can report an idea as copied; admin reviews timestamps and confirms/dismisses
2. **Visibility Control** — PUBLIC / PRIVATE / INVITE_ONLY with token-based invite links
3. **Terms Agreement** — users confirm originality before posting
4. **Collaboration Requests** — instead of copying, users can request to co-own an idea; accepted requesters become co-owners

---

### 9. Battle Arena (Real-Time)
- Admin creates a battle between two PUBLIC ideas
- Battle runs for **24 hours**
- Users vote for one idea per battle (one vote per user)
- Live split vote bar updates in real-time via WebSocket `/topic/battle/{battleId}`
- Countdown timer ticks every second
- Spring `@Scheduled` job **auto-resolves** battle at expiry → sets winner, awards XP and badge

---

### 10. Live Leaderboard
- Top 10 ideas ranked by traction score
- Auto-updates every **30 seconds** via WebSocket broadcast
- Rank changes animate: green flash for moving up, red flash for moving down

---

### 11. Gamification Engine

**XP Events:**
| Event | XP Awarded |
|---|---|
| Post an idea | +50 XP |
| Receive an upvote | +10 XP |
| Post a comment | +5 XP |
| Answer a challenge | +100 XP |
| Win a battle | +500 XP |
| Cast a battle vote | +5 XP |
| 7-day streak | +200 XP |
| Idea enters Hall of Fame | +300 XP |
| Collaboration accepted | +75 XP |

**Level Progression:**
| XP Range | Level |
|---|---|
| 0 – 199 | Newcomer |
| 200 – 499 | Thinker |
| 500 – 999 | Innovator |
| 1000 – 2499 | Visionary |
| 2500+ | Legend |

**Badges (8 Total):**
| Badge | Condition |
|---|---|
| `FIRST_IDEA` | Posted first idea |
| `TRENDSETTER` | Idea reaches 100 votes |
| `SERIAL_PITCHER` | Posted 5+ ideas |
| `PROBLEM_SOLVER` | Won a battle |
| `GHOST_BUSTER` | Answered all challenges on an idea |
| `CROWD_FAVOURITE` | Idea entered Hall of Fame |
| `PROTECTOR` | Successfully reported a plagiarised idea |
| `COLLABORATOR` | Accepted or sent a collaboration |

---

### 12. 🌟 Weekly Hall of Fame
- Spring `@Scheduled` cron job runs **every Monday at midnight**
- Picks **top 3 PUBLIC ideas** by traction score
- Awards `CROWD_FAVOURITE` badge and **+300 XP** to all 3 owners
- Public page — visible without login

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and receive JWT token |
| GET/POST/PUT/DELETE | `/api/ideas` | Idea CRUD operations |
| POST | `/api/ideas/check-duplicate` | Duplicate detection |
| GET | `/api/ideas/{id}/certificate` | Public ownership certificate |
| POST | `/api/ideas/{id}/vote` | Upvote or downvote |
| POST | `/api/ideas/{id}/comments` | Post a comment |
| POST | `/api/ideas/{id}/report` | Report as plagiarised |
| POST | `/api/ideas/{id}/collab-request` | Request collaboration |
| POST | `/api/battles` | Create battle (admin only) |
| GET | `/api/battles/active` | Get active battle |
| POST | `/api/battles/{id}/vote` | Cast battle vote |
| GET | `/api/leaderboard` | Top 10 ideas |
| GET | `/api/hall-of-fame` | All-time Hall of Fame |
| GET | `/api/users/me` | Current user profile with XP and badges |

---

## 🚀 How to Run

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL 8.0

### Step 1 — Database
```sql
CREATE DATABASE ideaforge_db;
```

### Step 2 — Backend
```bash
cd backend
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

### Step 3 — Frontend
```bash
cd frontend
npm install
npm start
# Runs on http://localhost:3000
```

> Make sure MySQL is running before starting the backend. Backend and frontend run simultaneously.

---

## What Makes This Project Stand Out

1. **Real-time WebSocket architecture** — live voting, leaderboard, and battle arena; not just REST APIs
2. **Custom traction scoring algorithm** — multi-variable weighted formula with scheduled auto-recalculation
3. **Pure Java duplicate detection** — Jaccard Similarity with stop word filtering, zero paid API cost
4. **4-layer idea protection system** — reports, visibility control, terms agreement, and collaboration
5. **Event-driven gamification engine** — XP, levels, and 8 unique badges
6. **Automated scheduled jobs** — battle resolution and Hall of Fame via Spring `@Scheduled`
7. **Role-based access control** — User vs Admin with JWT-secured endpoints
8. **Immutable ownership certificates** — UUID-based digital proof of first submission

---

## 👨‍💻 Developer

**Velmurugan**  
MCA Graduate | Full-Stack Developer  
Stack: React JS · Java · Spring Boot · MySQL · WebSocket

---

## 📄 License

This project is for educational and portfolio purposes.
