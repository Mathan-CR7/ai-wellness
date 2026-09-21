/* ===========================================================================
   API Client — REST API Wrapper with Bearer Token Injection
   =========================================================================== */

const API_BASE_URL = 'https://ai-wellness-jt1d.onrender.com';

const ApiClient = {
  getToken() {
    return localStorage.getItem('wellness_token');
  },

  setToken(token) {
    localStorage.setItem('wellness_token', token);
  },

  clearToken() {
    localStorage.removeItem('wellness_token');
  },

  getHeaders(skipAuth = false) {
    const headers = {
      'Content-Type': 'application/json'
    };
    if (!skipAuth) {
      const token = this.getToken();
      if (token && token !== 'undefined' && token !== 'null') {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }
    return headers;
  },

  async request(endpoint, options = {}, skipAuth = false) {
    const defaultHeaders = this.getHeaders(skipAuth);
    options.headers = {
      ...defaultHeaders,
      ...(options.headers || {})
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
    
    if (response.status === 401 || response.status === 403) {
      this.clearToken();
      window.dispatchEvent(new Event('auth:unauthorized'));
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'HTTP Error' }));
      throw new Error(errorData.detail || errorData.message || `HTTP ${response.status}`);
    }

    return response.json();
  },

  async login(email, password) {
    const res = await this.request('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    }, true);
    if (res.token) {
      this.setToken(res.token);
    }
    return res;
  },

  async getProfile() {
    return this.request('/api/users/me');
  },

  async getActivitySummary() {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1).toISOString();
    const end = now.toISOString();
    return this.request(`/api/activities/summary?startTime=${encodeURIComponent(start)}&endTime=${encodeURIComponent(end)}`);
  },

  async getMyActivities() {
    return this.request('/api/activities/my');
  },

  async getTeamLeaderboard(teamId = 1) {
    return this.request(`/api/teams/${teamId}/leaderboard`);
  },

  async sendAIChat(prompt, conversationId = null) {
    return this.request('/api/ai/chat', {
      method: 'POST',
      body: JSON.stringify({ prompt, conversationId })
    });
  }
};
