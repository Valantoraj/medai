/* ============================================================
   MedAI — confidence.js
   Confidence score sidebar rendering and updates
   ============================================================ */

function renderConfidenceScores(scores) {
  const list = document.getElementById('confidence-list');
  if (!list) return;

  if (!scores || scores.length === 0) {
    list.innerHTML = '<p style="font-size:11px;color:var(--text-muted);">No conditions tracked yet. Keep answering questions.</p>';
    return;
  }

  list.innerHTML = scores.map(s => {
    const pct = Math.round(s.confidence);
    const fillClass = pct >= 80 ? 'threshold' : pct >= 60 ? 'warning' : '';
    return `
      <div class="confidence-item">
        <div class="confidence-label">
          <span class="confidence-name">${s.condition}</span>
          <span class="confidence-pct">${pct}%</span>
        </div>
        <div class="confidence-bar">
          <div class="confidence-fill ${fillClass}" style="width:${pct}%"></div>
        </div>
        ${s.reasoning ? `<div class="confidence-reasoning">${s.reasoning}</div>` : ''}
      </div>`;
  }).join('');
}

function showThresholdCard(condition, confidence) {
  const card = document.getElementById('threshold-card');
  if (!card) return;
  card.style.display = '';
  card.innerHTML = `
    <div class="threshold-card">
      <div class="threshold-card-title">✅ Assessment Ready</div>
      <div style="font-size:12px;margin-top:4px;">
        <strong>${condition}</strong> — ${Math.round(confidence)}% confidence
      </div>
      <div style="font-size:11px;color:var(--text-muted);margin-top:4px;">
        Threshold reached. Dr. MedAI has provided an assessment above.
      </div>
    </div>`;
}

function showHospitalTrigger(specialty) {
  const el = document.getElementById('hospital-trigger');
  if (!el) return;
  el.style.display = '';
  el.innerHTML = `🏥 <span>Critical condition detected${specialty ? ` (${specialty})` : ''}. <a href="/hospitals.html" style="color:var(--warning);font-weight:700;">Find nearby hospitals →</a></span>`;
}
