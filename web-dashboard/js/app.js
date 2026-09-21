/* ===========================================================================
   App Orchestrator — Main Dashboard UI Orchestrator
   =========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  AuthManager.init();
  AIChatManager.init();

  window.addEventListener('auth:authenticated', async () => {
    // 1. Connect Real-Time STOMP WebSocket FIRST
    try {
      WebSocketManager.connect(1);
    } catch (wsErr) {
      console.error('Failed to connect WebSocket:', wsErr);
    }

    // 2. Fetch User Profile
    try {
      const profile = await ApiClient.getProfile().catch(() => null);
      const userInfo = document.getElementById('userInfo');
      if (userInfo && profile) {
        userInfo.innerText = `👤 ${profile.fullName || 'User'} (${profile.email})`;
      }
    } catch (pErr) {
      console.warn('Could not fetch user profile:', pErr);
    }

    // 3. Load Activity Summary Data
    try {
      await loadActivityData();
    } catch (aErr) {
      console.warn('Could not fetch activity summary:', aErr);
    }

    // 4. Fetch initial leaderboard HTTP fallback
    try {
      const initialLeaderboard = await ApiClient.getTeamLeaderboard(1).catch(() => null);
      if (initialLeaderboard && initialLeaderboard.rankings && initialLeaderboard.rankings.length > 0) {
        WebSocketManager.renderLeaderboard(initialLeaderboard);
      }
    } catch (lErr) {
      console.warn('Could not fetch initial leaderboard:', lErr);
    }
  });

  window.addEventListener('activity:updated', () => {
    loadActivityData();
  });
});

async function loadActivityData() {
  try {
    const summary = await ApiClient.getActivitySummary().catch(() => null);
    const todayStepsRes = await ApiClient.request('/api/steps/today').catch(() => null);

    let steps = 0;
    if (todayStepsRes && todayStepsRes.steps && todayStepsRes.steps > 0) {
      steps = todayStepsRes.steps;
    } else if (summary && summary.totalSteps && summary.totalSteps > 0) {
      steps = summary.totalSteps;
    }

    let distanceMeters = (summary && summary.totalDistanceMeters && summary.totalDistanceMeters > 0) 
      ? summary.totalDistanceMeters 
      : (steps * 0.66); // 0.66m per step estimate

    let calories = (summary && summary.totalCaloriesBurned && summary.totalCaloriesBurned > 0) 
      ? Math.round(summary.totalCaloriesBurned) 
      : Math.round(steps * 0.04); // 0.04 kcal per step estimate

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
    if (sourceDisplay) {
      if (myActivities.length > 0 && myActivities[0].sourceDevice) {
        const latest = myActivities[0];
        sourceDisplay.innerText = `${latest.sourceDevice} (${new Date(latest.syncedAt).toLocaleTimeString()})`;
      } else if (steps > 0) {
        sourceDisplay.innerText = 'Android Health Connect (Live Sync)';
      } else {
        sourceDisplay.innerText = 'Health Connect (Waiting)';
      }
    }

  } catch (err) {
    console.error('Error loading activity data:', err);
  }
}
