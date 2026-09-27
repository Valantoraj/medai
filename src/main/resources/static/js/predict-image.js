/* ============================================================
   MedAI — predict-image.js
   Image-based cancer detection (5 CNN/ViT models)
   ============================================================ */

if (!isLoggedIn()) window.location.href = '/login.html';

let currentScan = 'lung';
let selectedFile = null;

const SCAN_CONFIG = {
  lung: {
    title: 'Lung Cancer Detection',
    icon: '🫁',
    endpoint: '/api/predict/image/lung',
    imageType: 'CT Scan',
    desc: 'Upload a chest CT scan image. The AI will classify it as Normal, Benign, or Malignant.',
    classes: ['Normal', 'Benign', 'Malignant'],
    threshold: 80
  },
  skin: {
    title: 'Skin Cancer Detection',
    icon: '🩹',
    endpoint: '/api/predict/image/skin',
    imageType: 'Dermoscopy',
    desc: 'Upload a dermoscopy image of a skin lesion. The model classifies 7 lesion types.',
    classes: ['Melanoma', 'Basal Cell Carcinoma', 'Squamous Cell Carcinoma', 'Benign Keratosis', 'Dermatofibroma', 'Melanocytic Nevi', 'Vascular Lesion'],
    threshold: 70
  },
  blood: {
    title: 'Blood Cancer / Leukemia Detection',
    icon: '🩸',
    endpoint: '/api/predict/image/blood',
    imageType: 'Blood Smear',
    desc: 'Upload a blood smear microscopy image. The model detects leukemia subtypes.',
    classes: ['Benign', '[Malignant] early Pre-B', '[Malignant] Pre-B', '[Malignant] Pro-B'],
    threshold: 70
  },
  kidney: {
    title: 'Kidney CT Scan Analysis',
    icon: '🫘',
    endpoint: '/api/predict/image/kidney',
    imageType: 'CT Scan',
    desc: 'Upload a kidney CT scan. RenalCLIP classifies as Cyst, Normal, Stone, or Tumor.',
    classes: ['Cyst', 'Normal', 'Stone', 'Tumor'],
    threshold: 80
  },
  brain: {
    title: 'Brain Tumor MRI Analysis',
    icon: '🧠',
    endpoint: '/api/predict/image/brain',
    imageType: 'MRI',
    desc: 'Upload a brain MRI scan. ResNet50 detects Glioma, Meningioma, Pituitary, or No Tumor.',
    classes: ['Glioma', 'Meningioma', 'Pituitary', 'No Tumor'],
    threshold: 75
  }
};

document.addEventListener('DOMContentLoaded', () => {
  selectScan('lung');
});

function selectScan(scan) {
  currentScan = scan;
  selectedFile = null;
  document.getElementById('analyze-btn').disabled = true;
  resetUploadZone();

  document.querySelectorAll('.scan-item').forEach(el => {
    el.classList.toggle('active', el.dataset.scan === scan);
  });

  const cfg = SCAN_CONFIG[scan];
  document.getElementById('scan-title').textContent = cfg.title;
  document.getElementById('scan-desc').textContent = cfg.desc;
  document.getElementById('result-container').innerHTML = '';
}

// ── File handling ──────────────────────────────────────────
function onFileSelect(event) {
  const file = event.target.files[0];
  if (file) handleFile(file);
}

function onDragOver(e) {
  e.preventDefault();
  document.getElementById('upload-zone').classList.add('dragover');
}

function onDragLeave(e) {
  document.getElementById('upload-zone').classList.remove('dragover');
}

function onDrop(e) {
  e.preventDefault();
  document.getElementById('upload-zone').classList.remove('dragover');
  const file = e.dataTransfer.files[0];
  if (file) handleFile(file);
}

function handleFile(file) {
  if (!file.type.startsWith('image/')) {
    showToast('Please select an image file (JPG, PNG, etc.)', 'warning');
    return;
  }
  if (file.size > 50 * 1024 * 1024) {
    showToast('File too large. Maximum 50MB.', 'error');
    return;
  }

  selectedFile = file;
  document.getElementById('analyze-btn').disabled = false;

  const reader = new FileReader();
  reader.onload = (e) => {
    document.getElementById('upload-placeholder').style.display = 'none';
    document.getElementById('image-preview-container').style.display = '';
    document.getElementById('image-preview').src = e.target.result;
    document.getElementById('file-name').textContent = `${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
  };
  reader.readAsDataURL(file);
}

function clearImage() {
  selectedFile = null;
  document.getElementById('file-input').value = '';
  document.getElementById('analyze-btn').disabled = true;
  resetUploadZone();
  document.getElementById('result-container').innerHTML = '';
}

function resetUploadZone() {
  document.getElementById('upload-placeholder').style.display = '';
  document.getElementById('image-preview-container').style.display = 'none';
  document.getElementById('image-preview').src = '';
  document.getElementById('file-name').textContent = '';
}

// ── Run analysis ───────────────────────────────────────────
async function analyzeImage() {
  if (!selectedFile) return;

  const cfg = SCAN_CONFIG[currentScan];
  const btn = document.getElementById('analyze-btn');
  btn.disabled = true;
  btn.textContent = '⏳ Analyzing…';

  const formData = new FormData();
  formData.append('image', selectedFile);

  const res = await api('POST', cfg.endpoint, formData, true);
  btn.disabled = false;
  btn.textContent = 'Analyze Image';

  if (!res || !res.ok) {
    showToast(res?.data?.error || 'Analysis failed. Is the ML service running?', 'error');
    return;
  }

  renderImageResult(res.data, cfg);
}

function renderImageResult(data, cfg) {
  if (data.error) {
    document.getElementById('result-container').innerHTML = `
      <div class="result-card" style="border-color:var(--danger);">
        <strong style="color:var(--danger);">❌ Analysis Error</strong>
        <p style="font-size:13px;margin-top:8px;">${data.error}</p>
      </div>`;
    return;
  }

  const severityColors = { LOW: 'var(--success)', MEDIUM: 'var(--warning)', HIGH: 'var(--danger)', CRITICAL: 'var(--danger)' };
  const severity = data.riskLevel || data.severity || 'MEDIUM';
  const color = severityColors[severity] || 'var(--text-muted)';
  const confidencePct = data.confidencePct || (data.confidence ? Math.round(data.confidence * 100) + '%' : '?%');
  const confNum = data.confidence ? Math.round(data.confidence * 100) : 0;

  document.getElementById('result-container').innerHTML = `
    <div class="result-card">
      <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:16px;">
        <div>
          <h3 style="font-size:18px;font-weight:700;">${cfg.icon} ${data.disease || cfg.title}</h3>
          <p style="font-size:12px;color:var(--text-muted);">Model: ${data.modelUsed || 'Vision Model'} · ${cfg.imageType}</p>
        </div>
        <span class="badge badge-${severity.toLowerCase()}" style="font-size:13px;padding:6px 14px;">${severity}</span>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px;">
        <div style="background:var(--bg-hover);border-radius:10px;padding:16px;text-align:center;">
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:6px;">Predicted Class</div>
          <div style="font-size:20px;font-weight:800;color:${color};">${data.predictedClass || '—'}</div>
        </div>
        <div style="background:var(--bg-hover);border-radius:10px;padding:16px;text-align:center;">
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:6px;">Confidence</div>
          <div style="font-size:20px;font-weight:800;font-family:monospace;color:var(--medical);">${confidencePct}</div>
        </div>
      </div>

      <div style="margin-bottom:12px;">
        <div class="confidence-bar" style="height:8px;">
          <div class="confidence-fill" style="width:${confNum}%;background:${color};height:8px;"></div>
        </div>
        ${!data.aboveThreshold ? `<p style="font-size:11px;color:var(--text-muted);margin-top:4px;">⚠️ Below confidence threshold (${cfg.threshold}%). Result may be uncertain.</p>` : ''}
      </div>

      ${data.triggerHospitalFinder ? `
        <div class="hospital-trigger" style="margin-bottom:16px;">
          🚨 <strong>Critical finding.</strong>
          <a href="/hospitals.html?urgent=true&specialty=${encodeURIComponent(data.hospitalSpecialtyFilter || 'Oncology')}" style="color:var(--danger);font-weight:700;margin-left:6px;">
            Find nearest ${data.hospitalSpecialtyFilter || 'oncology'} hospital →
          </a>
        </div>` : ''}

      ${data.guidance ? `
        <div style="border-top:1px solid var(--border);padding-top:16px;">
          <div class="section-title" style="margin-bottom:10px;">🩺 Dr. MedAI Guidance</div>
          <div style="font-size:13px;color:var(--text);line-height:1.8;white-space:pre-wrap;">${data.guidance}</div>
        </div>` : ''}

      <div style="margin-top:16px;padding:10px;background:color-mix(in srgb,var(--danger) 8%,transparent);border-radius:8px;font-size:11px;color:var(--text-muted);">
        ⚠️ This AI screening tool does not replace professional radiological diagnosis. Always have your scans reviewed by a qualified specialist.
      </div>
    </div>`;

  document.getElementById('result-container').scrollIntoView({ behavior: 'smooth' });
}
