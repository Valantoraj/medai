/* ============================================================
   MedAI — auth.js
   Login / Register logic
   ============================================================ */

// Redirect if already logged in
if (isLoggedIn()) {
  window.location.href = '/dashboard.html';
}

function switchTab(tab) {
  const isLogin = tab === 'login';
  document.getElementById('login-form').style.display = isLogin ? '' : 'none';
  document.getElementById('register-form').style.display = isLogin ? 'none' : '';
  document.getElementById('login-tab').classList.toggle('active', isLogin);
  document.getElementById('register-tab').classList.toggle('active', !isLogin);
}

// ── Login ──────────────────────────────────────────────────
document.getElementById('login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const btn = document.getElementById('login-btn');
  const errEl = document.getElementById('login-error');
  errEl.style.display = 'none';
  btn.disabled = true;
  btn.textContent = 'Signing in…';

  const res = await api('POST', '/api/auth/login', {
    usernameOrEmail: document.getElementById('login-identifier').value.trim(),
    password: document.getElementById('login-password').value
  });

  btn.disabled = false;
  btn.textContent = 'Sign In';

  if (res && res.ok) {
    setToken(res.data.token);
    setUser(res.data);
    showToast('Welcome back, ' + (res.data.fullName || res.data.username) + '!', 'success');
    setTimeout(() => window.location.href = '/dashboard.html', 600);
  } else {
    const msg = res?.data?.message || res?.data || 'Invalid credentials';
    errEl.textContent = msg;
    errEl.style.display = '';
  }
});

// ── Register ───────────────────────────────────────────────
document.getElementById('register-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const btn = document.getElementById('register-btn');
  const errEl = document.getElementById('register-error');
  errEl.style.display = 'none';

  const password = document.getElementById('reg-password').value;
  if (password.length < 8) {
    errEl.textContent = 'Password must be at least 8 characters.';
    errEl.style.display = '';
    return;
  }

  btn.disabled = true;
  btn.textContent = 'Creating account…';

  const body = {
    username: document.getElementById('reg-username').value.trim(),
    email: document.getElementById('reg-email').value.trim(),
    password,
    fullName: document.getElementById('reg-fullname').value.trim(),
    gender: document.getElementById('reg-gender').value || null,
    dateOfBirth: document.getElementById('reg-dob').value || null
  };

  const res = await api('POST', '/api/auth/register', body);

  btn.disabled = false;
  btn.textContent = 'Create Account';

  if (res && res.ok) {
    setToken(res.data.token);
    setUser(res.data);
    showToast('Account created! Welcome, ' + (res.data.fullName || res.data.username) + '!', 'success');
    setTimeout(() => window.location.href = '/dashboard.html', 700);
  } else {
    const msg = res?.data?.message || res?.data || 'Registration failed';
    errEl.textContent = msg;
    errEl.style.display = '';
  }
});
