/* ============================================================
   MedAI — predict.js
   Disease risk prediction forms (6 tabular ML models)
   ============================================================ */

if (!isLoggedIn()) window.location.href = '/login.html';

let currentDisease = 'heart';

const DISEASE_CONFIG = {
  heart: {
    title: 'Heart Disease Risk Assessment',
    icon: '❤️',
    endpoint: '/api/predict/heart',
    description: 'Assess your risk for coronary heart disease using 13 clinical parameters.',
    fields: [
      { id: 'age', label: 'Age', type: 'number', min: 1, max: 120, placeholder: '45' },
      { id: 'sex', label: 'Sex', type: 'select', options: [['1','Male'],['0','Female']] },
      { id: 'chest_pain_type', label: 'Chest Pain Type', type: 'select', options: [['1','Typical Angina'],['2','Atypical Angina'],['3','Non-Anginal Pain'],['4','Asymptomatic']] },
      { id: 'resting_bp', label: 'Resting Blood Pressure (mmHg)', type: 'number', min: 60, max: 220, placeholder: '120' },
      { id: 'cholesterol', label: 'Serum Cholesterol (mg/dl)', type: 'number', min: 100, max: 600, placeholder: '200' },
      { id: 'fasting_blood_sugar', label: 'Fasting Blood Sugar > 120 mg/dl', type: 'select', options: [['1','Yes'],['0','No']] },
      { id: 'resting_ecg', label: 'Resting ECG Result', type: 'select', options: [['0','Normal'],['1','ST-T Wave Abnormality'],['2','Left Ventricular Hypertrophy']] },
      { id: 'max_heart_rate', label: 'Maximum Heart Rate Achieved', type: 'number', min: 60, max: 220, placeholder: '150' },
      { id: 'exercise_angina', label: 'Exercise-Induced Angina', type: 'select', options: [['1','Yes'],['0','No']] },
      { id: 'st_depression', label: 'ST Depression (Exercise vs Rest)', type: 'number', min: 0, max: 10, placeholder: '1.0', step: '0.1' },
      { id: 'slope_of_st', label: 'Slope of Peak Exercise ST', type: 'select', options: [['1','Upsloping'],['2','Flat'],['3','Downsloping']] },
      { id: 'num_vessels', label: 'Number of Major Vessels (0-3)', type: 'number', min: 0, max: 3, placeholder: '0' },
      { id: 'thal', label: 'Thalassemia', type: 'select', options: [['3','Normal'],['6','Fixed Defect'],['7','Reversible Defect']] }
    ]
  },
  stroke: {
    title: 'Stroke Risk Assessment',
    icon: '🧠',
    endpoint: '/api/predict/stroke',
    description: 'Evaluate your stroke risk based on 10 clinical and lifestyle factors.',
    fields: [
      { id: 'age', label: 'Age', type: 'number', min: 1, max: 120, placeholder: '55' },
      { id: 'gender', label: 'Gender', type: 'select', options: [['1','Male'],['0','Female'],['0.5','Other']] },
      { id: 'hypertension', label: 'Hypertension', type: 'select', options: [['1','Yes'],['0','No']] },
      { id: 'heart_disease_history', label: 'Heart Disease History', type: 'select', options: [['1','Yes'],['0','No']] },
      { id: 'ever_married', label: 'Ever Married', type: 'select', options: [['1','Yes'],['0','No']] },
      { id: 'work_type', label: 'Work Type', type: 'select', options: [['0','Never Worked'],['1','Children'],['2','Govt Job'],['3','Private'],['4','Self-employed']] },
      { id: 'residence_type', label: 'Residence Type', type: 'select', options: [['1','Urban'],['0','Rural']] },
      { id: 'avg_glucose_level', label: 'Average Glucose Level (mg/dL)', type: 'number', min: 50, max: 300, placeholder: '90' },
      { id: 'bmi', label: 'BMI', type: 'number', min: 10, max: 60, placeholder: '25', step: '0.1' },
      { id: 'smoking_status', label: 'Smoking Status', type: 'select', options: [['0','Never Smoked'],['1','Formerly Smoked'],['2','Smokes'],['3','Unknown']] }
    ]
  },
  diabetes: {
    title: 'Diabetes Risk Assessment',
    icon: '🩸',
    endpoint: '/api/predict/diabetes',
    description: 'Predict diabetes likelihood using the Pima Indian Diabetes dataset model.',
    fields: [
      { id: 'pregnancies', label: 'Number of Pregnancies', type: 'number', min: 0, max: 20, placeholder: '0' },
      { id: 'glucose', label: 'Plasma Glucose (mg/dL)', type: 'number', min: 0, max: 300, placeholder: '100' },
      { id: 'blood_pressure', label: 'Diastolic Blood Pressure (mmHg)', type: 'number', min: 0, max: 150, placeholder: '72' },
      { id: 'skin_thickness', label: 'Triceps Skin Fold Thickness (mm)', type: 'number', min: 0, max: 100, placeholder: '20' },
      { id: 'insulin', label: '2-Hour Serum Insulin (μU/mL)', type: 'number', min: 0, max: 900, placeholder: '80' },
      { id: 'bmi', label: 'BMI', type: 'number', min: 0, max: 70, placeholder: '25', step: '0.1' },
      { id: 'diabetes_pedigree_function', label: 'Diabetes Pedigree Function', type: 'number', min: 0, max: 3, placeholder: '0.5', step: '0.001' },
      { id: 'age', label: 'Age', type: 'number', min: 1, max: 120, placeholder: '30' }
    ]
  },
  'lung-tabular': {
    title: 'Lung Cancer Risk Assessment',
    icon: '🫁',
    endpoint: '/api/predict/lung-tabular',
    description: 'Assess lung cancer risk based on environmental, lifestyle, and symptom factors.',
    fields: [
      { id: 'age', label: 'Age', type: 'number', min: 1, max: 120, placeholder: '50' },
      { id: 'gender', label: 'Gender', type: 'select', options: [['1','Male'],['0','Female']] },
      { id: 'air_pollution', label: 'Air Pollution Exposure (1-8)', type: 'number', min: 1, max: 8, placeholder: '3' },
      { id: 'alcohol_use', label: 'Alcohol Use (1-8)', type: 'number', min: 1, max: 8, placeholder: '2' },
      { id: 'dust_allergy', label: 'Dust Allergy (1-8)', type: 'number', min: 1, max: 8, placeholder: '2' },
      { id: 'occupational_hazards', label: 'Occupational Hazards (1-8)', type: 'number', min: 1, max: 8, placeholder: '1' },
      { id: 'genetic_risk', label: 'Genetic Risk (1-7)', type: 'number', min: 1, max: 7, placeholder: '2' },
      { id: 'chronic_lung_disease', label: 'Chronic Lung Disease (1-7)', type: 'number', min: 1, max: 7, placeholder: '1' },
      { id: 'balanced_diet', label: 'Balanced Diet (1-7)', type: 'number', min: 1, max: 7, placeholder: '4' },
      { id: 'obesity', label: 'Obesity (1-7)', type: 'number', min: 1, max: 7, placeholder: '2' },
      { id: 'smoking', label: 'Smoking (1-8)', type: 'number', min: 1, max: 8, placeholder: '2' },
      { id: 'passive_smoker', label: 'Passive Smoker (1-8)', type: 'number', min: 1, max: 8, placeholder: '2' },
      { id: 'chest_pain', label: 'Chest Pain (1-9)', type: 'number', min: 1, max: 9, placeholder: '2' },
      { id: 'coughing_of_blood', label: 'Coughing of Blood (1-9)', type: 'number', min: 1, max: 9, placeholder: '1' },
      { id: 'fatigue', label: 'Fatigue (1-9)', type: 'number', min: 1, max: 9, placeholder: '2' }
    ]
  },
  'kidney-tabular': {
    title: 'Kidney Stone Risk Assessment',
    icon: '🫘',
    endpoint: '/api/predict/kidney-tabular',
    description: 'Assess kidney stone risk from 6 urine and blood chemistry parameters.',
    fields: [
      { id: 'urine_gravity', label: 'Urine Specific Gravity', type: 'number', min: 1.001, max: 1.040, placeholder: '1.015', step: '0.001' },
      { id: 'urine_ph', label: 'Urine pH', type: 'number', min: 4.5, max: 8.5, placeholder: '6.5', step: '0.1' },
      { id: 'urine_osmolality', label: 'Urine Osmolality (mOsm/kg)', type: 'number', min: 50, max: 1200, placeholder: '500' },
      { id: 'urine_conductivity', label: 'Urine Conductivity (mS/cm)', type: 'number', min: 0.5, max: 40, placeholder: '14', step: '0.1' },
      { id: 'urea', label: 'Blood Urea (mmol/L)', type: 'number', min: 10, max: 500, placeholder: '100' },
      { id: 'calcium', label: 'Urinary Calcium (mmol/L)', type: 'number', min: 0, max: 10, placeholder: '2', step: '0.1' }
    ]
  },
  liver: {
    title: 'Liver Disease Risk Assessment',
    icon: '🟤',
    endpoint: '/api/predict/liver',
    description: 'Predict liver disease risk from 10 liver function test parameters.',
    fields: [
      { id: 'age', label: 'Age', type: 'number', min: 1, max: 120, placeholder: '45' },
      { id: 'gender', label: 'Gender', type: 'select', options: [['1','Male'],['0','Female']] },
      { id: 'total_bilirubin', label: 'Total Bilirubin (mg/dL)', type: 'number', min: 0, max: 75, placeholder: '1.0', step: '0.1' },
      { id: 'direct_bilirubin', label: 'Direct Bilirubin (mg/dL)', type: 'number', min: 0, max: 20, placeholder: '0.3', step: '0.1' },
      { id: 'alkaline_phosphotase', label: 'Alkaline Phosphotase (IU/L)', type: 'number', min: 60, max: 2200, placeholder: '150' },
      { id: 'alamine_aminotransferase', label: 'Alamine Aminotransferase / ALT (IU/L)', type: 'number', min: 10, max: 2000, placeholder: '40' },
      { id: 'aspartate_aminotransferase', label: 'Aspartate Aminotransferase / AST (IU/L)', type: 'number', min: 10, max: 5000, placeholder: '40' },
      { id: 'total_proteins', label: 'Total Proteins (g/dL)', type: 'number', min: 2, max: 10, placeholder: '6.5', step: '0.1' },
      { id: 'albumin', label: 'Albumin (g/dL)', type: 'number', min: 0, max: 6, placeholder: '3.5', step: '0.1' },
      { id: 'albumin_globulin_ratio', label: 'Albumin/Globulin Ratio', type: 'number', min: 0, max: 3, placeholder: '1.0', step: '0.01' }
    ]
  }
};

document.addEventListener('DOMContentLoaded', () => {
  selectDisease('heart');
});

function selectDisease(disease) {
  currentDisease = disease;
  document.querySelectorAll('.disease-item').forEach(el => {
    el.classList.toggle('active', el.dataset.disease === disease);
  });
  renderForm(disease);
  document.getElementById('result-container').innerHTML = '';
}

function renderForm(disease) {
  const cfg = DISEASE_CONFIG[disease];
  const halfLen = Math.ceil(cfg.fields.length / 2);
  const col1 = cfg.fields.slice(0, halfLen);
  const col2 = cfg.fields.slice(halfLen);

  function fieldHtml(f) {
    if (f.type === 'select') {
      return `<div class="form-group">
        <label class="form-label">${f.label}</label>
        <select class="form-input" id="field-${f.id}" name="${f.id}">
          ${f.options.map(([v, l]) => `<option value="${v}">${l}</option>`).join('')}
        </select>
      </div>`;
    }
    return `<div class="form-group">
      <label class="form-label">${f.label}</label>
      <input class="form-input" type="number" id="field-${f.id}" name="${f.id}"
        min="${f.min}" max="${f.max}" step="${f.step || 1}" placeholder="${f.placeholder}" required/>
    </div>`;
  }

  document.getElementById('form-container').innerHTML = `
    <div style="margin-bottom:20px;">
      <h2 style="font-size:20px;font-weight:700;display:flex;align-items:center;gap:10px;">
        <span>${cfg.icon}</span> ${cfg.title}
      </h2>
      <p style="font-size:13px;color:var(--text-muted);margin-top:6px;">${cfg.description}</p>
    </div>
    <form id="predict-form" onsubmit="submitPrediction(event)">
      <div class="predict-grid">
        <div>${col1.map(fieldHtml).join('')}</div>
        <div>${col2.map(fieldHtml).join('')}</div>
      </div>
      <div style="margin-top:8px;display:flex;gap:12px;">
        <button type="submit" class="btn btn-primary btn-lg" id="predict-btn">
          📊 Run Risk Assessment
        </button>
        <button type="reset" class="btn btn-ghost">Reset</button>
      </div>
    </form>`;
}

async function submitPrediction(e) {
  e.preventDefault();
  const cfg = DISEASE_CONFIG[currentDisease];
  const btn = document.getElementById('predict-btn');
  btn.disabled = true;
  btn.textContent = '⏳ Analyzing…';

  const features = {};
  cfg.fields.forEach(f => {
    const el = document.getElementById(`field-${f.id}`);
    if (el) features[f.id] = parseFloat(el.value) || 0;
  });

  const res = await api('POST', cfg.endpoint, features);
  btn.disabled = false;
  btn.textContent = '📊 Run Risk Assessment';

  if (!res || !res.ok) {
    showToast(res?.data?.error || 'Prediction failed. Is the ML service running?', 'error');
    return;
  }

  renderResult(res.data);
}

function renderResult(data) {
  const riskColors = { LOW: 'var(--success)', MEDIUM: 'var(--warning)', HIGH: 'var(--danger)', CRITICAL: 'var(--danger)' };
  const riskPct = data.riskPercentage || (data.riskProbability ? Math.round(data.riskProbability * 100) + '%' : '?%');
  const color = riskColors[data.riskLevel] || 'var(--text-muted)';
  const fillPct = data.riskProbability ? Math.round(data.riskProbability * 100) : 0;

  document.getElementById('result-container').innerHTML = `
    <div class="result-card">
      <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:16px;">
        <div>
          <h3 style="font-size:18px;font-weight:700;">${data.disease}</h3>
          <p style="font-size:12px;color:var(--text-muted);">Model: ${data.modelUsed || 'ML Model'}</p>
        </div>
        <span class="badge badge-${(data.riskLevel||'medium').toLowerCase()}" style="font-size:13px;padding:6px 14px;">
          ${data.riskLevel || 'MEDIUM'}
        </span>
      </div>

      <div style="margin-bottom:16px;">
        <div style="display:flex;justify-content:space-between;margin-bottom:6px;">
          <span style="font-size:13px;color:var(--text-2);">Risk Probability</span>
          <span style="font-size:22px;font-weight:800;font-family:monospace;color:${color};">${riskPct}</span>
        </div>
        <div class="risk-meter">
          <div class="risk-fill" style="width:${fillPct}%;background:${color};"></div>
        </div>
      </div>

      ${data.triggerHospitalFinder ? `
        <div class="hospital-trigger" style="margin-bottom:16px;">
          🏥 <strong>High risk detected.</strong>
          <a href="/hospitals.html?urgent=true&specialty=${encodeURIComponent(data.hospitalSpecialtyFilter || '')}" style="color:var(--warning);font-weight:700;margin-left:6px;">
            Find nearby ${data.hospitalSpecialtyFilter || 'hospitals'} →
          </a>
        </div>` : ''}

      ${data.guidance ? `
        <div style="border-top:1px solid var(--border);padding-top:16px;">
          <div class="section-title" style="margin-bottom:10px;">🩺 Dr. MedAI Guidance</div>
          <div style="font-size:13px;color:var(--text);line-height:1.8;white-space:pre-wrap;">${data.guidance}</div>
        </div>` : ''}

      <div style="margin-top:16px;padding:10px;background:color-mix(in srgb,var(--warning) 8%,transparent);border-radius:8px;font-size:11px;color:var(--text-muted);">
        ⚠️ This is a screening tool, not a medical diagnosis. Always consult a healthcare professional.
      </div>
    </div>`;

  document.getElementById('result-container').scrollIntoView({ behavior: 'smooth' });
}
