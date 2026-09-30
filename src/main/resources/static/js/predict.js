/* Tabular inference forms for the models trained by ml_service/train_ml_models.py. */
if (!isLoggedIn()) window.location.href = '/login.html';

let currentDisease = 'heart';

const FIELD_GROUPS = [
  {
    title: 'Demographics and history',
    fields: [
      { id: 'age_years', label: 'Age (years)', max: 120 },
      { id: 'sex_male1_female2', label: 'Sex', type: 'select', options: [['1', 'Male'], ['2', 'Female']] },
      { id: 'bmi', label: 'BMI (kg/m²)', max: 80, step: '0.1' },
      { id: 'waist_cm', label: 'Waist circumference (cm)', max: 250, step: '0.1' },
      { id: 'height_cm', label: 'Height (cm)', max: 230, step: '0.1' },
      { id: 'weight_kg', label: 'Weight (kg)', max: 350, step: '0.1' },
      { id: 'race_ethnicity', label: 'Race/ethnicity (NHANES code)', type: 'select', options: [['1', 'Mexican American'], ['2', 'Other Hispanic'], ['3', 'Non-Hispanic White'], ['4', 'Non-Hispanic Black'], ['6', 'Non-Hispanic Asian'], ['7', 'Other']] },
      { id: 'education_level', label: 'Education level', type: 'select', options: [['1', 'Less than 9th grade'], ['2', '9th–11th grade'], ['3', 'High school/GED'], ['4', 'Some college'], ['5', 'College graduate or above']] },
      { id: 'income_poverty_ratio', label: 'Income-to-poverty ratio', max: 10, step: '0.01' },
      { id: 'smoking_status', label: 'Smoking status', type: 'select', options: [['0', 'Never'], ['1', 'Former'], ['2', 'Current']] },
      { id: 'cigs_per_day', label: 'Cigarettes per day', max: 100 },
      { id: 'smoke_years', label: 'Years smoked', max: 100, step: '0.1' },
      { id: 'pack_years', label: 'Pack-years', max: 200, step: '0.1' },
      { id: 'told_high_bp', label: 'Previously told you have high blood pressure', type: 'select', options: [['1', 'Yes'], ['0', 'No']] },
      { id: 'told_high_chol', label: 'Previously told you have high cholesterol', type: 'select', options: [['1', 'Yes'], ['0', 'No']] },
      { id: 'family_hx_mi', label: 'Family history of heart attack', type: 'select', options: [['1', 'Yes'], ['0', 'No']] },
      { id: 'family_hx_diabetes', label: 'Family history of diabetes', type: 'select', options: [['1', 'Yes'], ['0', 'No']] },
      { id: 'alcohol_drinks_per_day', label: 'Drinks per drinking day (average)', max: 30, step: '0.1' }
    ]
  },
  {
    title: 'Blood pressure',
    fields: [
      { id: 'sbp_mmhg', label: 'Systolic blood pressure (mmHg)', max: 300 },
      { id: 'dbp_mmhg', label: 'Diastolic blood pressure (mmHg)', max: 200 }
    ]
  },
  {
    title: 'Blood chemistry',
    fields: [
      { id: 'albumin_g_dl', label: 'Albumin (g/dL)', max: 10, step: '0.1' },
      { id: 'alt_u_l', label: 'ALT (U/L)', max: 5000, step: '0.1' },
      { id: 'ast_u_l', label: 'AST (U/L)', max: 5000, step: '0.1' },
      { id: 'alp_u_l', label: 'Alkaline phosphatase (U/L)', max: 5000, step: '0.1' },
      { id: 'bun_mg_dl', label: 'Blood urea nitrogen (mg/dL)', max: 300, step: '0.1' },
      { id: 'calcium_mg_dl', label: 'Calcium (mg/dL)', max: 30, step: '0.1' },
      { id: 'bicarbonate_mmol_l', label: 'Bicarbonate (mmol/L)', max: 100, step: '0.1' },
      { id: 'creatinine_mg_dl', label: 'Creatinine (mg/dL)', max: 100, step: '0.01' },
      { id: 'globulin_g_dl', label: 'Globulin (g/dL)', max: 15, step: '0.1' },
      { id: 'glucose_serum_mg_dl', label: 'Serum glucose (mg/dL)', max: 1000, step: '0.1' },
      { id: 'ggt_u_l', label: 'GGT (U/L)', max: 5000, step: '0.1' },
      { id: 'iron_ug_dl', label: 'Iron (µg/dL)', max: 2000, step: '0.1' },
      { id: 'ldh_u_l', label: 'LDH (U/L)', max: 10000, step: '0.1' },
      { id: 'phosphorus_mg_dl', label: 'Phosphorus (mg/dL)', max: 30, step: '0.1' },
      { id: 'bilirubin_total_mg_dl', label: 'Total bilirubin (mg/dL)', max: 100, step: '0.01' },
      { id: 'total_protein_g_dl', label: 'Total protein (g/dL)', max: 20, step: '0.1' },
      { id: 'uric_acid_mg_dl', label: 'Uric acid (mg/dL)', max: 50, step: '0.1' },
      { id: 'sodium_mmol_l', label: 'Sodium (mmol/L)', max: 250, step: '0.1' },
      { id: 'potassium_mmol_l', label: 'Potassium (mmol/L)', max: 30, step: '0.1' },
      { id: 'chloride_mmol_l', label: 'Chloride (mmol/L)', max: 300, step: '0.1' }
    ]
  },
  {
    title: 'Complete blood count',
    fields: [
      { id: 'wbc_1000_ul', label: 'White blood cells (1000/µL)', max: 500, step: '0.01' },
      { id: 'lymph_pct', label: 'Lymphocytes (%)', max: 100, step: '0.1' },
      { id: 'mono_pct', label: 'Monocytes (%)', max: 100, step: '0.1' },
      { id: 'neut_pct', label: 'Neutrophils (%)', max: 100, step: '0.1' },
      { id: 'eos_pct', label: 'Eosinophils (%)', max: 100, step: '0.1' },
      { id: 'baso_pct', label: 'Basophils (%)', max: 100, step: '0.1' },
      { id: 'rbc_million_ul', label: 'Red blood cells (million/µL)', max: 20, step: '0.01' },
      { id: 'hemoglobin_g_dl', label: 'Hemoglobin (g/dL)', max: 30, step: '0.1' },
      { id: 'hematocrit_pct', label: 'Hematocrit (%)', max: 100, step: '0.1' },
      { id: 'mcv_fl', label: 'MCV (fL)', max: 250, step: '0.1' },
      { id: 'mch_pg', label: 'MCH (pg)', max: 100, step: '0.1' },
      { id: 'mchc_g_dl', label: 'MCHC (g/dL)', max: 100, step: '0.1' },
      { id: 'rdw_pct', label: 'RDW (%)', max: 100, step: '0.1' },
      { id: 'platelets_1000_ul', label: 'Platelets (1000/µL)', max: 3000, step: '0.1' },
      { id: 'mpv_fl', label: 'Mean platelet volume (fL)', max: 100, step: '0.1' }
    ]
  },
  {
    title: 'Lipids, glucose, and urine',
    fields: [
      { id: 'total_cholesterol_mg_dl', label: 'Total cholesterol (mg/dL)', max: 1500, step: '0.1' },
      { id: 'hdl_mg_dl', label: 'HDL cholesterol (mg/dL)', max: 500, step: '0.1' },
      { id: 'triglycerides_mg_dl', label: 'Triglycerides (mg/dL)', max: 10000, step: '0.1' },
      { id: 'ldl_mg_dl', label: 'LDL cholesterol (mg/dL)', max: 1500, step: '0.1' },
      { id: 'hba1c_pct', label: 'HbA1c (%)', max: 30, step: '0.1' },
      { id: 'fasting_glucose_mg_dl', label: 'Fasting glucose (mg/dL)', max: 1500, step: '0.1' },
      { id: 'urine_albumin_mg_l', label: 'Urine albumin (mg/L)', max: 100000, step: '0.1' },
      { id: 'urine_creatinine_mg_dl', label: 'Urine creatinine (mg/dL)', max: 10000, step: '0.1' },
      { id: 'acr_mg_g', label: 'Urine albumin/creatinine ratio (mg/g)', max: 100000, step: '0.1' },
      { id: 'cotinine_ng_ml', label: 'Cotinine (ng/mL)', max: 10000, step: '0.1' },
      { id: 'hscrp_mg_l', label: 'High-sensitivity CRP (mg/L)', max: 1000, step: '0.1' }
    ]
  }
];

const DISEASE_CONFIG = {
  heart: {
    title: 'Heart Disease Risk Assessment', iconName: 'heart', endpoint: '/api/predict/heart',
    description: 'NHANES stacked-ensemble model (heart_disease_model.joblib) for doctor-diagnosed heart disease — CHF, coronary heart disease, angina, or heart attack (MCQ160B–E).'
  },
  stroke: {
    title: 'Stroke Risk Assessment', iconName: 'brain', endpoint: '/api/predict/stroke',
    description: 'NHANES stacked-ensemble model (stroke_model.joblib) for doctor-diagnosed stroke (MCQ160F). Lab panel and clinical history features.'
  },
  diabetes: {
    title: 'Diabetes Screening', iconName: 'droplets', endpoint: '/api/predict/diabetes',
    description: 'NHANES screening model (diabetes_screening_model.joblib). Predicts diagnosed or lab-defined diabetes (HbA1c ≥ 6.5 / FPG ≥ 126 mg/dL). Glucose and HbA1c are withheld from inputs to prevent label leakage.'
  },
  'lung-tabular': {
    title: 'Lung Disease Risk Assessment', iconName: 'wind', endpoint: '/api/predict/lung-tabular',
    description: 'NHANES stacked-ensemble model (lung_disease_model.joblib) for emphysema, chronic bronchitis, and COPD (MCQ160G/K/P). Smoking history and lab features.'
  },
  'kidney-tabular': {
    title: 'Chronic Kidney Disease Risk', iconName: 'bean', endpoint: '/api/predict/kidney-tabular',
    description: 'NHANES stacked-ensemble model (kidney_ckd_selfreport_model.joblib) for self-reported weak or failing kidneys (KIQ022). eGFR, creatinine, and urine albumin are key inputs.'
  },
  liver: {
    title: 'Liver Disease Risk Assessment', iconName: 'layers', endpoint: '/api/predict/liver',
    description: 'NHANES stacked-ensemble model (liver_disease_model.joblib) for doctor-diagnosed liver condition (MCQ160L). ALT, AST, GGT, bilirubin, and FIB-4 index are key inputs.'
  }
};

document.addEventListener('DOMContentLoaded', () => selectDisease('heart'));

function selectDisease(disease) {
  currentDisease = disease;
  document.querySelectorAll('.disease-item').forEach(el => {
    el.classList.toggle('active', el.dataset.disease === disease);
  });
  renderForm(disease);
  document.getElementById('result-container').innerHTML = '';
}

function fieldHtml(field) {
  if (field.type === 'select') {
    return `<div class="form-group">
      <label class="form-label" for="field-${field.id}">${field.label}</label>
      <select class="form-input" id="field-${field.id}" name="${field.id}">
        <option value="">Choose if known</option>
        ${field.options.map(([value, label]) => `<option value="${value}">${label}</option>`).join('')}
      </select>
    </div>`;
  }
  return `<div class="form-group">
    <label class="form-label" for="field-${field.id}">${field.label}</label>
    <input class="form-input" type="number" id="field-${field.id}" name="${field.id}"
      min="0" max="${field.max || ''}" step="${field.step || 'any'}" placeholder="Leave blank if unknown"/>
  </div>`;
}

function renderForm(disease) {
  const cfg = DISEASE_CONFIG[disease];
  const groups = FIELD_GROUPS.map((group, index) => `
    <details class="input-group" ${index < 2 ? 'open' : ''}>
      <summary>${group.title}</summary>
      <div class="predict-grid input-grid">${group.fields.map(fieldHtml).join('')}</div>
    </details>`).join('');

  document.getElementById('form-container').innerHTML = `
    <div style="margin-bottom:20px;">
      <h2 style="font-size:20px;font-weight:700;display:flex;align-items:center;gap:10px;">
        ${icon(cfg.iconName, 20)} ${cfg.title}
      </h2>
      <p style="font-size:13px;color:var(--text-muted);margin-top:6px;">${cfg.description}</p>
      <p style="font-size:12px;color:var(--text-muted);margin-top:8px;">
        Enter at least five available measurements or history values. Unknown fields can stay blank;
        the saved model pipeline handles missing values.
      </p>
    </div>
    <form id="predict-form" onsubmit="submitPrediction(event)">
      ${groups}
      <div style="margin-top:12px;display:flex;gap:12px;">
        <button type="submit" class="btn btn-primary btn-lg" id="predict-btn">${icon('bar-chart-2', 15)} Run Risk Assessment</button>
        <button type="reset" class="btn btn-ghost">Reset</button>
      </div>
    </form>`;
}

async function submitPrediction(event) {
  event.preventDefault();
  const cfg = DISEASE_CONFIG[currentDisease];
  const button = document.getElementById('predict-btn');
  button.disabled = true;
  button.innerHTML = `${icon('loader-2', 15, 'spin')} Analyzing&hellip;`;

  const features = {};
  FIELD_GROUPS.flatMap(group => group.fields).forEach(field => {
    const value = document.getElementById(`field-${field.id}`)?.value.trim();
    if (value !== undefined && value !== '') features[field.id] = Number(value);
  });

  const response = await api('POST', cfg.endpoint, features);
  button.disabled = false;
  button.innerHTML = `${icon('bar-chart-2', 15)} Run Risk Assessment`;

  if (!response || !response.ok) {
    showToast(response?.data?.error || 'Prediction failed. Is the ML service running?', 'error');
    return;
  }
  renderResult(response.data);
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
        <span class="badge badge-${(data.riskLevel || 'medium').toLowerCase()}" style="font-size:13px;padding:6px 14px;">
          ${data.riskLevel || 'MEDIUM'}
        </span>
      </div>
      <div style="margin-bottom:16px;">
        <div style="display:flex;justify-content:space-between;margin-bottom:6px;">
          <span style="font-size:13px;color:var(--text-2);">Risk Probability</span>
          <span style="font-size:22px;font-weight:800;font-family:monospace;color:${color};">${riskPct}</span>
        </div>
        <div class="risk-meter"><div class="risk-fill" style="width:${fillPct}%;background:${color};"></div></div>
        ${data.decisionThreshold !== undefined ? `<p style="font-size:11px;color:var(--text-muted);margin-top:5px;">Model decision threshold: ${Math.round(data.decisionThreshold * 100)}%</p>` : ''}
      </div>
      ${data.triggerHospitalFinder ? `
        <div class="hospital-trigger" style="margin-bottom:16px;display:flex;align-items:center;gap:8px;">
          ${icon('hospital', 15)} <strong>High risk detected.</strong>
          <a href="/hospitals.html?urgent=true&specialty=${encodeURIComponent(data.hospitalSpecialtyFilter || '')}" style="color:var(--warning);font-weight:700;margin-left:4px;">
            Find nearby ${data.hospitalSpecialtyFilter || 'hospitals'} &rarr;
          </a>
        </div>` : ''}
      ${data.guidance ? `
        <div style="border-top:1px solid var(--border);padding-top:16px;">
          <div class="section-title" style="margin-bottom:10px;display:flex;align-items:center;gap:6px;">
            ${icon('stethoscope', 14)} Dr. MedAI Guidance
          </div>
          <div style="font-size:13px;color:var(--text);line-height:1.8;white-space:pre-wrap;">${data.guidance}</div>
        </div>` : ''}
      <div style="margin-top:16px;padding:10px;background:color-mix(in srgb,var(--warning) 8%,transparent);border-radius:8px;font-size:11px;color:var(--text-muted);display:flex;align-items:flex-start;gap:6px;">
        ${icon('alert-triangle', 13)} This is a screening tool, not a medical diagnosis. Always consult a healthcare professional.
      </div>
    </div>`;
  document.getElementById('result-container').scrollIntoView({ behavior: 'smooth' });
}
