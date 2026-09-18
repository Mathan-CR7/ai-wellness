/* ===========================================================================
   App Orchestrator — Main Dashboard UI Orchestrator
   =========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  AuthManager.init();
  AIChatManager.init();

  window.addEventListener('auth:authenticated', async () => {
    try {
      // 1. Fetch User Profile
      const profile = await ApiClient.getProfile();
      const userInfo = document.getElementById('userInfo');
      if (userInfo && profile) {
        userInfo.innerText = `👤 ${profile.fullName} (${profile.email})`;
      }

      // 2. Load Activity Summary Data
      await loadActivityData();

      // 3. Connect Real-Time STOMP WebSocket
      WebSocketManager.connect(1);

      // 4. Fetch initial leaderboard HTTP fallback
      const initialLeaderboard = await ApiClient.getTeamLeaderboard(1).catch(() => null);
      if (initialLeaderboard) {
        WebSocketManager.renderLeaderboard(initialLeaderboard);
      }

    } catch (err) {
      console.error('Failed to initialize dashboard data:', err);
    }
  });

  window.addEventListener('activity:updated', () => {
    loadActivityData();
  });
});

async function loadActivityData() {
  try {
    const summary = await ApiClient.getActivitySummary().catch(() => null);
    if (!summary) return;

    const steps = summary.totalSteps || 0;
    const distanceMeters = summary.totalDistanceMeters || 0;
    const calories = Math.round(summary.totalCaloriesBurned || 0);

    // Update Step Number Display
    const stepDisplay = document.getElementById('stepDisplay');
    if (stepDisplay) {
      stepDisplay.innerText = steps.toLocaleString();
    }

    // Update Distance & Calories
    const distDisplay = document.getElementById('distDisplay');
    if (distDisplay) {
      distDisplay.innerText = (distanceMeters / 1000.0).toFixed(2);
    }

    const calDisplay = document.getElementById('calDisplay');
    if (calDisplay) {
      calDisplay.innerText = calories.toLocaleString();
    }

    // Update Circular SVG Step Ring Progress (Target: 10,000 steps)
    const ringProgress = document.getElementById('ringProgress');
    if (ringProgress) {
      const maxOffset = 314.159; // Circumference for r=50 (2 * pi * 50)
      const targetSteps = 10000;
      const progressRatio = Math.min(steps / targetSteps, 1.0);
      const newOffset = maxOffset * (1.0 - progressRatio);
      ringProgress.style.strokeDashoffset = newOffset;
    }

    // Update Source Device if available
    const myActivities = await ApiClient.getMyActivities().catch(() => []);
    const sourceDisplay = document.getElementById('sourceDisplay');
    if (sourceDisplay && myActivities.length > 0) {
      const latest = myActivities[0];
      sourceDisplay.innerText = `${latest.sourceDevice || 'Android Health Connect'} (${new Date(latest.syncedAt).toLocaleTimeString()})`;
    }

  } catch (err) {
    console.error('Error loading activity data:', err);
  }
}
