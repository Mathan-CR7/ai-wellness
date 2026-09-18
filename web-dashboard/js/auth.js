/* ===========================================================================
   Auth Module — Authentication State Management
   =========================================================================== */

const AuthManager = {
  init() {
    const loginForm = document.getElementById('loginForm');
    const logoutBtn = document.getElementById('logoutBtn');

    if (loginForm) {
      loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('loginEmail').value.trim();
        const password = document.getElementById('loginPassword').value.trim();

        try {
          await ApiClient.login(email, password);
          document.getElementById('authOverlay').style.display = 'none';
          document.getElementById('mainContent').style.display = 'block';
          document.getElementById('logoutBtn').style.display = 'inline-block';
          
          window.dispatchEvent(new Event('auth:authenticated'));
        } catch (err) {
          alert(`Authentication failed: ${err.message}`);
        }
      });
    }

    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        ApiClient.clearToken();
        location.reload();
      });
    }

    window.addEventListener('auth:unauthorized', () => {
      document.getElementById('authOverlay').style.display = 'flex';
      document.getElementById('mainContent').style.display = 'none';
      document.getElementById('logoutBtn').style.display = 'none';
    });

    if (ApiClient.getToken()) {
      document.getElementById('authOverlay').style.display = 'none';
      document.getElementById('mainContent').style.display = 'block';
      document.getElementById('logoutBtn').style.display = 'inline-block';
      setTimeout(() => window.dispatchEvent(new Event('auth:authenticated')), 100);
    }
  }
};
