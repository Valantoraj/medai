/* ============================================================
   MedAI — personalisation.js
   Personalisation toggle + status sync helpers
   (Shared by all pages; included via app.js initPersonalisationToggle)
   ============================================================ */

// This module extends app.js personalisation functions with visual feedback

async function syncPersonalisationStatus() {
  if (!isLoggedIn()) return;
  const res = await api('GET', '/api/personalisation/status');
  if (!res?.ok) return;

  const on = res.data.personalisationEnabled;
  const toggle = document.getElementById('personalisation-toggle');
  if (toggle) updateToggleUI(toggle, on);

  // Update user object in sessionStorage
  const user = getUser();
  if (user) {
    user.personalisationEnabled = on;
    setUser(user);
  }
}

// Call on page load to sync state
document.addEventListener('DOMContentLoaded', syncPersonalisationStatus);
