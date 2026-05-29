import axios from 'axios';

/**
 * Axios instance pre-configured for the IdeaForge API.
 *
 * Request interceptor  → attaches JWT from localStorage.
 * Response interceptor → on 401 clears auth state and redirects to /login.
 */
const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

/* ---------- Request interceptor ---------- */
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/* ---------- Response interceptor ---------- */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear all auth data and force re-login
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // Hard redirect — avoids stale React state
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

/* ================================================================
   Typed API helpers — used by pages and components
   ================================================================ */

// ── Auth ──────────────────────────────────────────────────────────
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login:    (data) => api.post('/auth/login', data),
};

// ── Users ────────────────────────────────────────────────────────
export const userAPI = {
  getMe:             ()       => api.get('/users/me'),
  getById:         (id)     => api.get(`/users/${id}`),
  getIdeas:        (id)     => api.get(`/users/${id}/ideas`),
  getCollaborations:(id)   => api.get(`/users/${id}/collaborations`),
  getBadges:       (userId) => api.get(`/users/${userId}/badges`),
};

// ── Ideas ────────────────────────────────────────────────────────
export const ideaAPI = {
  create:         (data)              => api.post('/ideas', data),
  checkDuplicate: (data)              => api.post('/ideas/check-duplicate', data),
  getAll:         (params)            => api.get('/ideas', { params }),
  getById:        (id, inviteToken)   => api.get(`/ideas/${id}`, { params: inviteToken ? { inviteToken } : {} }),
  update:         (id, data)          => api.put(`/ideas/${id}`, data),
  delete:         (id)                => api.delete(`/ideas/${id}`),
  getCertificate: (id)                => api.get(`/ideas/${id}/certificate`),
  createInvite:   (id)                => api.post(`/ideas/${id}/invites`),
  getInvites:     (id)                => api.get(`/ideas/${id}/invites`),
  revokeInvite:   (ideaId, inviteId)  => api.delete(`/ideas/${ideaId}/invites/${inviteId}`),
};

// ── Votes ────────────────────────────────────────────────────────
export const voteAPI = {
  cast:       (ideaId, data)  => api.post(`/ideas/${ideaId}/vote`, data),
  getUserVote:(ideaId)        => api.get(`/ideas/${ideaId}/vote`),
};

// ── Comments ─────────────────────────────────────────────────────
export const commentAPI = {
  post:       (ideaId, data)  => api.post(`/ideas/${ideaId}/comments`, data),
  getAll:     (ideaId, tag)   => api.get(`/ideas/${ideaId}/comments`, { params: tag ? { tag } : {} }),
  answer:     (commentId)     => api.put(`/comments/${commentId}/answer`),
};

// ── Collab Requests ───────────────────────────────────────────────
export const collabAPI = {
  create:     (ideaId, data)  => api.post(`/ideas/${ideaId}/collab-request`, data),
  getReceived:()              => api.get('/collab-requests/received'),
  getSent:    ()              => api.get('/collab-requests/sent'),
  accept:     (id)            => api.put(`/collab-requests/${id}/accept`),
  reject:     (id)            => api.put(`/collab-requests/${id}/reject`),
};

// ── Battles ───────────────────────────────────────────────────────
export const battleAPI = {
  getActive:  ()              => api.get('/battles/active'),
  getHistory: ()              => api.get('/battles/history'),
  vote:       (battleId, data)=> api.post(`/battles/${battleId}/vote`, data),
  create:     (data)          => api.post('/battles', data),
};

// ── Hall of Fame ──────────────────────────────────────────────────
export const hallOfFameAPI = {
  getAll: () => api.get('/hall-of-fame'),
};

// ── Plagiarism Reports ────────────────────────────────────────────
export const reportAPI = {
  /** POST /api/ideas/{reportedIdeaId}/report  body: { originalIdeaId, reason } */
  submit: (reportedIdeaId, data) => api.post(`/ideas/${reportedIdeaId}/report`, data),
  getAll: ()                     => api.get('/reports'),
};

// ── Admin ──────────────────────────────────────────────────────────
export const adminAPI = {
  getReports:     ()          => api.get('/admin/reports'),
  getStats:       ()          => api.get('/admin/stats'),
  confirmReport:  (id)        => api.put(`/admin/reports/${id}/confirm`),
  dismissReport:  (id)        => api.put(`/admin/reports/${id}/dismiss`),
  setupAdmin:     ()          => api.post('/admin/setup'),
};
