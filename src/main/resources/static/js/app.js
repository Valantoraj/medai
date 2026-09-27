/* ============================================================
   MedAI — app.js
   Global init: theme toggle, navbar active state, toast, API helpers
   ============================================================ */

// ── Theme ──────────────────────────────────────────────────
const THEME_KEY = 'medai-theme';

function getTheme() {
  return localStorage.getItem(THEME_KEY) || 'dark';
}

function applyTheme(theme) {
  document.documentElement.classList.remove('dark', 'light');
  document.documentElement.classList.add(theme);
  localStorage.setItem(THEME_KEY, theme);
  const btn = document.getElementById('theme-toggle');
  if (btn) btn.textContent = theme === 'dark' ? '☀️' : '🌙';
}

function toggleTheme() {
  applyTheme(getTheme() === 'dark' ? 'light' : 'dark');
}

// Apply theme immediately (before DOM fully loads) to prevent flash
applyTheme(getTheme());

// ── Auth helpers ───────────────────────────────────────────
function getToken() {
  // JWT is stored in httpOnly cookie automatically; for JS access we also
  // keep a copy in sessionStorage for use in Authorization header
  return sessionStorage.getItem('medai_jwt') || null;
}

function setToken(token) {
  sessionStorage.setItem('medai_jwt', token);
}

function clearToken() {
  sessionStorage.removeItem('medai_jwt');
  sessionStorage.removeItem('medai_user');
}

function getUser() {
  try {
    return JSON.parse(sessionStorage.getItem('medai_user') || 'null');
  } catch { return null; }
}

function setUser(user) {
  sessionStorage.setItem('medai_user', JSON.stringify(user));
}

function isLoggedIn() {
  return !!getToken();
}

function requireAuth() {
  if (!isLoggedIn()) {
    window.location.href = '/login.html';
    return false;
  }
  return true;
}

// ── API fetch wrapper ──────────────────────────────────────
async function api(method, path, body = null, isFormData = false) {
  const headers = {};
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (body && !isFormData) headers['Content-Type'] = 'application/json';

  const opts = { method, headers, credentials: 'include' };
  if (body) opts.body = isFormData ? body : JSON.stringify(body);

  const res = await fetch(path, opts);

  if (res.status === 401) {
    clearToken();
    window.location.href = '/login.html';
    return null;
  }

  const text = await res.text();
  try {
    return { ok: res.ok, status: res.status, data: JSON.parse(text) };
  } catch {
    return { ok: res.ok, status: res.status, data: text };
  }
}

// ── Toast notifications ────────────────────────────────────
function showToast(message, type = 'info', duration = 3500) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;

  const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
  toast.innerHTML = `<span>${icons[type] || 'ℹ️'}</span><span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.3s';
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

// ── Personalisation toggle ─────────────────────────────────
async function initPersonalisationToggle() {
  const toggle = document.getElementById('personalisation-toggle');
  if (!toggle || !isLoggedIn()) return;

  // Get current status
  const res = await api('GET', '/api/personalisation/status');
  if (res && res.ok) {
    const on = res.data.personalisationEnabled;
    updateToggleUI(toggle, on);
  }

  toggle.addEventListener('click', async () => {
    const res = await api('PUT', '/api/personalisation/toggle');
    if (res && res.ok) {
      const on = res.data.personalisationEnabled;
      updateToggleUI(toggle, on);
      showToast(on ? 'Personalisation enabled' : 'Personalisation disabled', 'info');
    }
  });
}

function updateToggleUI(el, on) {
  el.classList.toggle('on', on);
  const label = el.querySelector('.toggle-label');
  if (label) label.textContent = on ? 'Personalised' : 'General';
}

// ── Navbar active link ─────────────────────────────────────
function highlightNav() {
  const path = window.location.pathname;
  document.querySelectorAll('.nav-link').forEach(link => {
    const href = link.getAttribute('href');
    if (href && path.endsWith(href)) {
      link.classList.add('active');
    }
  });
}

// ── User display in navbar ─────────────────────────────────
function renderUserInNav() {
  const user = getUser();
  const el = document.getElementById('nav-username');
  if (el && user) el.textContent = user.fullName || user.username;

  // Show/hide auth-gated links
  const gated = document.querySelectorAll('[data-auth]');
  gated.forEach(el => {
    el.style.display = isLoggedIn() ? '' : 'none';
  });
  const guestOnly = document.querySelectorAll('[data-guest]');
  guestOnly.forEach(el => {
    el.style.display = isLoggedIn() ? 'none' : '';
  });
}

// ── Logout ─────────────────────────────────────────────────
async function logout() {
  await api('POST', '/api/auth/logout');
  clearToken();
  window.location.href = '/login.html';
}

// ── Format date ────────────────────────────────────────────
function formatDate(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
}

// ── DOM ready init ─────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  applyTheme(getTheme());

  const themeBtn = document.getElementById('theme-toggle');
  if (themeBtn) themeBtn.addEventListener('click', toggleTheme);

  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) logoutBtn.addEventListener('click', logout);

  highlightNav();
  renderUserInNav();
  initPersonalisationToggle();
});
