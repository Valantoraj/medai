/* Image inference using the local YOLO weights from the training scripts. */
if (!isLoggedIn()) window.location.href = '/login.html';

let currentScan = 'lung';
let selectedFile = null;

const SCAN_CONFIG = {
  lung: {
    title: 'Lung Cancer Detection', iconName: 'wind', endpoint: '/api/predict/image/lung',
    imageType: 'CT Scan',
    desc: 'Upload a lung CT slice. Powered by a locally trained YOLO detection model (lung_cancer_model.pt). Reports detected class and confidence score.',
    threshold: 50
  },
  liver: {
    title: 'Liver Cancer Detection', iconName: 'layers', endpoint: '/api/predict/image/liver',
    imageType: 'CT Scan',
    desc: 'Upload a liver CT slice. Uses the liver_cancer_yolo_fold0 detection model trained with cross-validation (liver_cancer_detect_v2). Reports lesion class and confidence.',
    threshold: 50
  },
  skin: {
    title: 'Skin Cancer Detection', iconName: 'bandage', endpoint: '/api/predict/image/skin',
    imageType: 'Dermoscopy',
    desc: 'Upload a dermoscopy image. The locally trained YOLO classification model distinguishes melanoma, BCC, SCC, and benign lesions.',
    threshold: 70
  },
  blood: {
    title: 'Blood Cancer / Leukemia Detection', iconName: 'droplets', endpoint: '/api/predict/image/blood',
    imageType: 'Blood Smear',
    desc: 'Upload a blood smear image. The YOLO classification model (blood_cancer_model.pt) identifies leukemic blast cells vs. normal lymphocytes.',
    threshold: 70
  },
  kidney: {
    title: 'Kidney CT Scan Analysis', iconName: 'bean', endpoint: '/api/predict/image/kidney',
    imageType: 'CT Scan',
    desc: 'Upload a kidney CT slice. The YOLO detection model (kidney_cancer_cyst_stone_model.pt) identifies cysts, stones, and tumors.',
    threshold: 25
  },
  brain: {
    title: 'Brain Tumor MRI Analysis', iconName: 'brain', endpoint: '/api/predict/image/brain',
    imageType: 'MRI',
    desc: 'Upload a brain MRI slice. Powered by the brain_tumor_yolo model (brain_tumor_yolo/weights/best.pt). Detects and classifies tumor regions.',
    threshold: 70
  }
};

document.addEventListener('DOMContentLoaded', () => selectScan('lung'));

function selectScan(scan) {
  currentScan = scan;
  selectedFile = null;
  document.getElementById('analyze-btn').disabled = true;
  resetUploadZone();
  document.querySelectorAll('.scan-item').forEach(el => {
    el.classList.toggle('active', el.dataset.scan === scan);
  });
  const config = SCAN_CONFIG[scan];
  document.getElementById('scan-title').textContent = config.title;
  document.getElementById('scan-desc').textContent = config.desc;
  document.getElementById('result-container').innerHTML = '';
}

function onFileSelect(event) {
  const file = event.target.files[0];
  if (file) handleFile(file);
}

function onDragOver(event) {
  event.preventDefault();
  document.getElementById('upload-zone').classList.add('dragover');
}

function onDragLeave() {
  document.getElementById('upload-zone').classList.remove('dragover');
}

function onDrop(event) {
  event.preventDefault();
  document.getElementById('upload-zone').classList.remove('dragover');
  const file = event.dataTransfer.files[0];
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
  reader.onload = event => {
    document.getElementById('upload-placeholder').style.display = 'none';
    document.getElementById('image-preview-container').style.display = '';
    document.getElementById('image-preview').src = event.target.result;
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

async function analyzeImage() {
  if (!selectedFile) return;
  const config = SCAN_CONFIG[currentScan];
  const button = document.getElementById('analyze-btn');
  button.disabled = true;
  button.innerHTML = `${icon('loader-2', 15, 'spin')} Analyzing&hellip;`;

  const formData = new FormData();
  formData.append('image', selectedFile);
  const response = await api('POST', config.endpoint, formData, true);
  button.disabled = false;
  button.textContent = 'Analyze Image';

  if (!response || !response.ok) {
    showToast(response?.data?.error || 'Analysis failed. Is the ML service running?', 'error');
    return;
  }
  renderImageResult(response.data, config);
}

function renderImageResult(data, config) {
  if (data.error) {
    document.getElementById('result-container').innerHTML = `
      <div class="result-card" style="border-color:var(--danger);">
        <strong style="color:var(--danger);display:flex;align-items:center;gap:6px;">
          ${icon('x-circle', 16)} Analysis Error
        </strong>
        <p style="font-size:13px;margin-top:8px;">${data.error}</p>
      </div>`;
    return;
  }

  const colors = { LOW: 'var(--success)', MEDIUM: 'var(--warning)', HIGH: 'var(--danger)', CRITICAL: 'var(--danger)' };
  const severity = data.riskLevel || data.severity || 'MEDIUM';
  const color = colors[severity] || 'var(--text-muted)';
  const confidencePct = data.confidencePct || (data.confidence ? Math.round(data.confidence * 100) + '%' : '0%');
  const confidence = data.confidence ? Math.round(data.confidence * 100) : 0;

  document.getElementById('result-container').innerHTML = `
    <div class="result-card">
      <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:16px;">
        <div>
          <h3 style="font-size:18px;font-weight:700;display:flex;align-items:center;gap:8px;">
            ${icon(config.iconName, 20)} ${data.disease || config.title}
          </h3>
          <p style="font-size:12px;color:var(--text-muted);margin-top:3px;">Model: ${data.modelUsed || 'Local YOLO model'} · ${config.imageType}</p>
        </div>
        <span class="badge badge-${severity.toLowerCase()}" style="font-size:13px;padding:6px 14px;">${severity}</span>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px;">
        <div style="background:var(--bg-hover);border-radius:10px;padding:16px;text-align:center;">
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:6px;">Detected Class</div>
          <div style="font-size:20px;font-weight:800;color:${color};">${data.predictedClass || '—'}</div>
        </div>
        <div style="background:var(--bg-hover);border-radius:10px;padding:16px;text-align:center;">
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:6px;">Confidence</div>
          <div style="font-size:20px;font-weight:800;font-family:monospace;color:var(--medical);">${confidencePct}</div>
        </div>
      </div>

      <div class="confidence-bar" style="height:8px;margin-bottom:16px;">
        <div class="confidence-fill" style="width:${confidence}%;background:${color};height:8px;"></div>
      </div>

      ${data.annotatedImageB64 ? `
        <div style="margin-bottom:16px;">
          <div class="section-title" style="margin-bottom:8px;display:flex;align-items:center;gap:6px;">
            ${icon('scan', 14)} Annotated Scan
          </div>
          <img src="data:image/jpeg;base64,${data.annotatedImageB64}"
               style="max-width:100%;border-radius:10px;border:1px solid var(--border);display:block;"
               alt="YOLO-annotated scan with detected regions"/>
        </div>` : ''}

      ${data.triggerHospitalFinder ? `
        <div class="hospital-trigger" style="margin-bottom:16px;display:flex;align-items:center;gap:8px;">
          ${icon('alert-octagon', 15)} <strong>Critical finding.</strong>
          <a href="/hospitals.html?urgent=true&specialty=${encodeURIComponent(data.hospitalSpecialtyFilter || 'Oncology')}" style="color:var(--danger);font-weight:700;margin-left:4px;">
            Find nearest ${data.hospitalSpecialtyFilter || 'oncology'} hospital &rarr;
          </a>
        </div>` : ''}

      ${data.guidance ? `
        <div style="border-top:1px solid var(--border);padding-top:16px;">
          <div class="section-title" style="margin-bottom:10px;display:flex;align-items:center;gap:6px;">
            ${icon('stethoscope', 14)} Dr. MedAI Guidance
          </div>
          <div style="font-size:13px;color:var(--text);line-height:1.8;white-space:pre-wrap;">${data.guidance}</div>
        </div>` : ''}

      <div style="margin-top:16px;padding:10px;background:color-mix(in srgb,var(--danger) 8%,transparent);border-radius:8px;font-size:11px;color:var(--text-muted);display:flex;align-items:flex-start;gap:6px;">
        ${icon('alert-triangle', 13)} This screening result does not replace a professional diagnosis. Have scans reviewed by a qualified specialist.
      </div>
    </div>`;
  document.getElementById('result-container').scrollIntoView({ behavior: 'smooth' });
}
