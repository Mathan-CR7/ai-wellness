/* ===========================================================================
   WebSocket Manager — SockJS & STOMP Real-Time Live Streaming Listener
   =========================================================================== */

const WebSocketManager = {
  stompClient: null,
  activeTeamId: 1,

  pollInterval: null,

  connect(teamId = 1) {
    this.activeTeamId = teamId;
    const wsBadge = document.getElementById('wsBadge');
    const wsStatusText = document.getElementById('wsStatusText');

    if (wsBadge) wsBadge.style.display = 'flex';
    if (wsStatusText) wsStatusText.innerText = 'Connecting WebSocket...';

    // 1. Fetch initial HTTP REST Leaderboard immediately so UI is never empty
    this.fetchRestLeaderboard();

    // 2. Start REST polling fallback (every 5 seconds) until WebSocket confirms live connection
    if (!this.pollInterval) {
      this.pollInterval = setInterval(() => this.fetchRestLeaderboard(), 5000);
    }

    try {
      // 3. Create SockJS connection with explicit transports
      const socket = new SockJS(`${API_BASE_URL}/ws`, null, {
        transports: ['xhr-streaming', 'xhr-polling', 'websocket']
      });
      this.stompClient = Stomp.over(socket);
      this.stompClient.debug = (msg) => console.log('[STOMP]', msg);

      // 4. Connect via STOMP Protocol
      this.stompClient.connect({}, () => {
        if (wsStatusText) wsStatusText.innerText = 'Live WebSocket Connected';
        
        // Stop REST polling fallback since WebSocket is live
        if (this.pollInterval) {
          clearInterval(this.pollInterval);
          this.pollInterval = null;
        }

        // Subscribe to Team Leaderboard Topic
        this.stompClient.subscribe(`/topic/leaderboard/${this.activeTeamId}`, (message) => {
          try {
            const leaderboardData = JSON.parse(message.body);
            this.renderLeaderboard(leaderboardData);
            window.dispatchEvent(new Event('activity:updated'));
          } catch (e) {
            console.error('[WebSocket STOMP] Failed to parse team payload:', e);
          }
        });

        // Subscribe to Challenge Leaderboard Topic
        this.subscribeToChallenge(1);
      }, (error) => {
        console.warn('[WebSocket STOMP] Disconnected, using REST fallback:', error);
        if (wsStatusText) wsStatusText.innerText = 'Live REST Streaming Active';
        setTimeout(() => this.connect(this.activeTeamId), 10000);
      });
    } catch (e) {
      console.warn('[WebSocket Init] Falling back to REST stream:', e);
      if (wsStatusText) wsStatusText.innerText = 'Live REST Streaming Active';
    }
  },

  async fetchRestLeaderboard() {
    try {
      // Check Team Leaderboard FIRST
      const teamData = await ApiClient.getTeamLeaderboard(this.activeTeamId).catch(() => null);
      if (teamData && teamData.rankings && teamData.rankings.length > 0) {
        this.renderLeaderboard(teamData);
        return;
      }

      // Check Challenge Leaderboard if team rankings are empty
      const challengeData = await ApiClient.request('/api/challenges/1/leaderboard').catch(() => null);
      if (challengeData && challengeData.leaderboard && challengeData.leaderboard.length > 0) {
        this.renderChallengeLeaderboard(challengeData);
        return;
      }

      this.renderEmptyState();
    } catch (e) {
      this.renderEmptyState();
    }
  },

  renderEmptyState() {
    const tbody = document.getElementById('leaderboardBody');
    if (!tbody) return;
    tbody.innerHTML = `
      <tr>
        <td colspan="4" style="text-align: center; color: var(--text-muted); padding: 2rem;">
          No activity recorded yet. Sync Health Connect from your phone to start the live leaderboard!
        </td>
      </tr>
    `;
  },

  subscribeToChallenge(challengeId = 1) {
    if (!this.stompClient || !this.stompClient.connected) return;
    this.stompClient.subscribe(`/topic/challenges/${challengeId}/leaderboard`, (message) => {
      try {
        const challengeData = JSON.parse(message.body);
        console.log('[WebSocket STOMP] Real-Time Challenge Leaderboard Broadcast Received:', challengeData);
        this.renderChallengeLeaderboard(challengeData);
        window.dispatchEvent(new Event('activity:updated'));
      } catch (e) {
        console.error('[WebSocket STOMP] Failed to parse challenge payload:', e);
      }
    });
  },

  renderChallengeLeaderboard(data) {
    const tbody = document.getElementById('leaderboardBody');
    if (!tbody || !data || !data.leaderboard) return;

    if (!data.leaderboard || data.leaderboard.length === 0) {
      this.renderEmptyState();
      return;
    }

    tbody.innerHTML = data.leaderboard.map((item, index) => {
      const rank = item.rank || (index + 1);
      const rankClass = rank === 1 ? 'rank-1' : rank === 2 ? 'rank-2' : rank === 3 ? 'rank-3' : 'rank-other';
      const initial = item.fullName ? item.fullName.charAt(0).toUpperCase() : 'U';
      const steps = item.totalSteps ? item.totalSteps.toLocaleString() : '0';
      const progress = item.progressPercentage !== undefined ? item.progressPercentage + '%' : '0%';

      return `
        <tr>
          <td><span class="rank-pill ${rankClass}">${rank}</span></td>
          <td>
            <div class="user-cell">
              <div class="avatar-sm">${initial}</div>
              <div>
                <div>${item.fullName || 'User'}</div>
              </div>
            </div>
          </td>
          <td style="text-align: right; font-family: var(--font-heading); font-weight: 700; color: #fff;">
            ${steps}
          </td>
          <td style="text-align: right; color: var(--text-secondary);">
            ${progress}
          </td>
        </tr>
      `;
    }).join('');
  },

  renderLeaderboard(data) {
    const tbody = document.getElementById('leaderboardBody');
    if (!tbody || !data || !data.rankings) return;

    if (!data.rankings || data.rankings.length === 0) {
      this.renderEmptyState();
      return;
    }

    tbody.innerHTML = data.rankings.map((item, index) => {
      const rank = item.rank || (index + 1);
      const rankClass = rank === 1 ? 'rank-1' : rank === 2 ? 'rank-2' : rank === 3 ? 'rank-3' : 'rank-other';
      const initial = item.fullName ? item.fullName.charAt(0).toUpperCase() : 'U';
      const steps = item.totalSteps ? item.totalSteps.toLocaleString() : '0';
      const distance = item.totalDistanceMeters ? (item.totalDistanceMeters / 1000.0).toFixed(2) : '0.00';

      return `
        <tr>
          <td><span class="rank-pill ${rankClass}">${rank}</span></td>
          <td>
            <div class="user-cell">
              <div class="avatar-sm">${initial}</div>
              <div>
                <div>${item.fullName || 'Team Member'}</div>
                <div style="font-size: 0.75rem; color: var(--text-muted); font-weight: normal;">${item.email || ''}</div>
              </div>
            </div>
          </td>
          <td style="text-align: right; font-family: var(--font-heading); font-weight: 700; color: #fff;">
            ${steps}
          </td>
          <td style="text-align: right; color: var(--text-secondary);">
            ${distance} km
          </td>
        </tr>
      `;
    }).join('');
  }
};
