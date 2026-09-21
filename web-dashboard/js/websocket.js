/* ===========================================================================
   WebSocket Manager — SockJS & STOMP Real-Time Live Streaming Listener
   =========================================================================== */

const WebSocketManager = {
  stompClient: null,
  activeTeamId: 1,

  connect(teamId = 1) {
    this.activeTeamId = teamId;
    const wsBadge = document.getElementById('wsBadge');
    const wsStatusText = document.getElementById('wsStatusText');

    if (wsBadge) wsBadge.style.display = 'flex';
    if (wsStatusText) wsStatusText.innerText = 'Connecting WebSocket...';

    // 1. Create SockJS socket connection to Spring Boot WebSocket Endpoint
    const socket = new SockJS(`${API_BASE_URL}/ws`);
    this.stompClient = Stomp.over(socket);
    
    // Disable debug logging for clean console
    this.stompClient.debug = null;

    // 2. Connect via STOMP Protocol
    this.stompClient.connect({}, () => {
      if (wsStatusText) wsStatusText.innerText = 'Live WebSocket Connected';
      
      // 3. Subscribe to Real-Time Team Leaderboard Topic
      this.stompClient.subscribe(`/topic/leaderboard/${this.activeTeamId}`, (message) => {
        try {
          const leaderboardData = JSON.parse(message.body);
          console.log('[WebSocket STOMP] Real-Time Team Rank Broadcast Received:', leaderboardData);
          this.renderLeaderboard(leaderboardData);
          window.dispatchEvent(new Event('activity:updated'));
        } catch (e) {
          console.error('[WebSocket STOMP] Failed to parse team payload:', e);
        }
      });

      // 4. Subscribe to Real-Time Challenge Leaderboard Topic
      this.subscribeToChallenge(1);
    }, (error) => {
      console.warn('[WebSocket STOMP] Disconnected:', error);
      if (wsStatusText) wsStatusText.innerText = 'WebSocket Reconnecting...';
      setTimeout(() => this.connect(this.activeTeamId), 5000);
    });
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

    if (data.leaderboard.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="4" style="text-align: center; color: var(--text-muted); padding: 2rem;">
            No challenge activity recorded yet. Sync Health Connect steps to get on the leaderboard!
          </td>
        </tr>
      `;
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

    if (data.rankings.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="4" style="text-align: center; color: var(--text-muted); padding: 2rem;">
            No team activity recorded yet. Sync Health Connect from your phone to get on the leaderboard!
          </td>
        </tr>
      `;
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
