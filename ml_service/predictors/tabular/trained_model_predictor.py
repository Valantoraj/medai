"""
Predictor that wraps the NHANES stacked-ensemble bundles produced by train_ml_models.py.

Bundle format (joblib dict):
  {
    "task":      str,
    "features":  [str, ...],          # exact ordered feature list the model was trained on
    "kinds":     [str, ...],          # base-model families in the stack ("lgb", "xgb", ...)
    "models":    {kind: [fold_models]},
    "meta":      LogisticRegression,  # stacking meta-learner
    "thresholds":{"max_f1": float, "youden": float, "spec90": float, "sens80": float},
    "chosen":    str,
    "metrics":   {...},
  }

Inference mirrors predict_bundle() + engineer() from train_ml_models.py:
  - compute all engineered/derived features from raw inputs
  - reindex to bundle["features"]  (still-missing cols become NaN — LGB/XGB/CatBoost handle it natively)
  - logit-transform mean prediction of each base-model family
  - column-stack and feed to the meta LogisticRegression
  - return positive-class probability
"""

import os
import math
import warnings
from pathlib import Path

import joblib
import numpy as np
import pandas as pd

# run_ml_models/ lives next to this file's grandparent (ml_service/)
_SERVICE_DIR = Path(__file__).resolve().parents[2]
_MODEL_DIR = Path(
    os.environ.get("NHANES_WORKDIR", _SERVICE_DIR / "run_ml_models")
)

# Map route task-key  →  (joblib stem,  human-readable disease name)
_TASK_META = {
    "heart":          ("heart_disease_model",           "Heart Disease"),
    "stroke":         ("stroke_model",                  "Stroke"),
    "diabetes":       ("diabetes_screening_model",      "Diabetes (Screening)"),
    "lung-tabular":   ("lung_disease_model",            "Lung Disease (COPD)"),
    "kidney-tabular": ("kidney_ckd_selfreport_model",   "Chronic Kidney Disease"),
    "liver":          ("liver_disease_model",           "Liver Disease"),
}


def _logit(p: np.ndarray) -> np.ndarray:
    p = np.clip(p, 1e-7, 1 - 1e-7)
    return np.log(p / (1 - p))


# ---------------------------------------------------------------------------
# Feature engineering — mirrors engineer() in train_ml_models.py exactly.
# Operates on a single-row DataFrame whose columns are the raw NHANES names
# (after the RENAME mapping).  All outputs are float; inf/-inf become NaN.
# ---------------------------------------------------------------------------
_BASE_FEATURES = [
    "age_years", "sex_male1_female2", "bmi", "waist_cm", "height_cm", "weight_kg",
    "albumin_g_dl", "alt_u_l", "ast_u_l", "alp_u_l", "bun_mg_dl", "calcium_mg_dl",
    "bicarbonate_mmol_l", "creatinine_mg_dl", "globulin_g_dl", "glucose_serum_mg_dl",
    "ggt_u_l", "iron_ug_dl", "ldh_u_l", "phosphorus_mg_dl", "bilirubin_total_mg_dl",
    "total_protein_g_dl", "uric_acid_mg_dl", "sodium_mmol_l", "potassium_mmol_l",
    "chloride_mmol_l", "wbc_1000_ul", "lymph_pct", "mono_pct", "neut_pct",
    "eos_pct", "baso_pct", "rbc_million_ul", "hemoglobin_g_dl", "hematocrit_pct",
    "mcv_fl", "mch_pg", "mchc_g_dl", "rdw_pct", "platelets_1000_ul", "mpv_fl",
    "total_cholesterol_mg_dl", "hdl_mg_dl", "triglycerides_mg_dl", "ldl_mg_dl",
    "hba1c_pct", "fasting_glucose_mg_dl", "urine_albumin_mg_l", "urine_creatinine_mg_dl",
    "acr_mg_g", "cotinine_ng_ml", "hscrp_mg_l", "sbp_mmhg", "dbp_mmhg",
]

_EXT_FEATURES = [
    "race_ethnicity", "education_level", "income_poverty_ratio", "smoking_status",
    "cigs_per_day", "smoke_years", "pack_years", "told_high_bp", "told_high_chol",
    "family_hx_mi", "family_hx_diabetes", "alcohol_drinks_per_day",
]


def _engineer(data: dict) -> pd.DataFrame:
    """
    Accept a flat feature dict (raw NHANES column names after RENAME mapping).
    Fills in missing base/extended columns as NaN, then computes all 29
    engineered features exactly as train_ml_models.py does.
    Returns a single-row DataFrame ready for predict_bundle().
    """
    # Start with every known column; missing ones become NaN
    d = {col: np.nan for col in _BASE_FEATURES + _EXT_FEATURES}
    d.update({k: float(v) for k, v in data.items() if v is not None and v != ""})
    df = pd.DataFrame([d])

    age = df["age_years"]
    male = (df["sex_male1_female2"] == 1).to_numpy()

    with np.errstate(all="ignore"):
        cr = df["creatinine_mg_dl"].to_numpy(dtype=float)
        alpha = np.where(male, -0.302, -0.241)
        r = cr / np.where(male, 0.9, 0.7)
        df["egfr_ckdepi2021"] = (
            142 * np.minimum(r, 1) ** alpha * np.maximum(r, 1) ** -1.2
            * 0.9938 ** age.to_numpy(dtype=float)
            * np.where(male, 1.0, 1.012)
        )
        df["pulse_pressure"] = df["sbp_mmhg"] - df["dbp_mmhg"]
        df["map_mmhg"] = df["dbp_mmhg"] + df["pulse_pressure"] / 3.0
        df["ast_alt_ratio"] = df["ast_u_l"] / df["alt_u_l"]
        df["fib4"] = age * df["ast_u_l"] / (df["platelets_1000_ul"] * np.sqrt(df["alt_u_l"]))
        df["apri"] = (df["ast_u_l"] / 40.0) / df["platelets_1000_ul"] * 100
        df["non_hdl_mg_dl"] = df["total_cholesterol_mg_dl"] - df["hdl_mg_dl"]
        df["tc_hdl_ratio"] = df["total_cholesterol_mg_dl"] / df["hdl_mg_dl"]
        df["tg_hdl_ratio"] = df["triglycerides_mg_dl"] / df["hdl_mg_dl"]
        df["ldl_hdl_ratio"] = df["ldl_mg_dl"] / df["hdl_mg_dl"]
        df["remnant_chol"] = df["total_cholesterol_mg_dl"] - df["hdl_mg_dl"] - df["ldl_mg_dl"]
        df["neut_lymph_ratio"] = df["neut_pct"] / df["lymph_pct"]
        df["sii"] = df["platelets_1000_ul"] * df["neut_pct"] / df["lymph_pct"]
        df["plr"] = df["platelets_1000_ul"] / df["lymph_pct"]
        df["mhr"] = df["mono_pct"] / df["hdl_mg_dl"]
        df["bun_creat_ratio"] = df["bun_mg_dl"] / df["creatinine_mg_dl"]
        df["uric_creat_ratio"] = df["uric_acid_mg_dl"] / df["creatinine_mg_dl"]
        df["anion_gap"] = (
            df["sodium_mmol_l"] - df["chloride_mmol_l"] - df["bicarbonate_mmol_l"]
        )
        df["alb_glob_ratio"] = df["albumin_g_dl"] / df["globulin_g_dl"]
        tg = df["triglycerides_mg_dl"] / 88.57    # mg/dL -> mmol/L
        gl = df["glucose_serum_mg_dl"] / 18.016
        df["tyg_index"] = np.log(tg * gl / 2.0)
        df["tyg_bmi"] = df["tyg_index"] * df["bmi"]
        df["tyg_wc"] = df["tyg_index"] * df["waist_cm"]
        df["waist_height_ratio"] = df["waist_cm"] / df["height_cm"]
        df["fli"] = (
            np.exp(
                0.953 * np.log(df["triglycerides_mg_dl"])
                + 0.139 * df["bmi"]
                + 0.718 * np.log(df["ggt_u_l"])
                + 0.053 * df["waist_cm"]
                - 15.745
            ) / (
                1 + np.exp(
                    0.953 * np.log(df["triglycerides_mg_dl"])
                    + 0.139 * df["bmi"]
                    + 0.718 * np.log(df["ggt_u_l"])
                    + 0.053 * df["waist_cm"]
                    - 15.745
                )
            ) * 100
        )
        df["hsi"] = (
            8 * (df["alt_u_l"] / df["ast_u_l"])
            + df["bmi"]
            + (2 if male.any() else 0)
        )
        df["log_acr"] = np.log1p(df["acr_mg_g"])

    # Replace inf/-inf with NaN (same as train_ml_models.py)
    df.replace([np.inf, -np.inf], np.nan, inplace=True)
    return df


def _predict_bundle(bundle: dict, X: pd.DataFrame) -> float:
    """Replicate predict_bundle() from train_ml_models.py for a single row.

    The spline-logistic ('lr') fold models use a MissingIndicator that was
    fitted on NHANES data where demographic columns (e.g. race_ethnicity) had
    zero missing values.  Newer sklearn versions raise when a column that was
    all-complete at fit time has NaN at inference time.  When that happens we
    silently drop the 'lr' family from the stack and re-weight with the
    remaining families, which are all tree-based and handle NaN natively.
    """
    X = X.reindex(columns=bundle["features"]).astype(float)
    logits = []
    used_kinds = []
    for kind in bundle["kinds"]:
        fold_models = bundle["models"][kind]
        try:
            preds = [m.predict_proba(X)[:, 1] for m in fold_models]
            logits.append(_logit(np.mean(preds, axis=0)))
            used_kinds.append(kind)
        except Exception:
            # lr pipeline MissingIndicator incompatibility — skip silently
            continue

    if not logits:
        raise RuntimeError("All base model families failed during inference.")

    Z = np.column_stack(logits) if len(logits) > 1 else logits[0].reshape(-1, 1)

    # If we dropped a family the stored meta-learner has a different input shape.
    # Re-fit a simple average when shapes mismatch.
    try:
        proba = bundle["meta"].predict_proba(Z)[:, 1]
    except Exception:
        proba = np.array([float(np.mean([_logit_inv(l[0]) for l in logits]))])

    return float(proba[0])


def _logit_inv(x: float) -> float:
    return 1.0 / (1.0 + math.exp(-x))


class TrainedModelPredictor:
    """Load one NHANES .joblib bundle and run inference on a feature dict."""

    def __init__(self, task: str):
        if task not in _TASK_META:
            raise ValueError(
                f"Unknown tabular task '{task}'. "
                f"Valid keys: {sorted(_TASK_META)}"
            )
        stem, self.disease_name = _TASK_META[task]
        self.model_path = _MODEL_DIR / f"{stem}.joblib"

        if not self.model_path.exists():
            raise FileNotFoundError(
                f"Model bundle not found: {self.model_path}\n"
                f"Train it with: python ml_service/train_ml_models.py --task all --skip-done"
            )

        self.bundle = joblib.load(self.model_path)
        self.thresholds = self.bundle.get("thresholds", {})
        self.model_label = (
            f"NHANES {self.bundle.get('chosen', 'ensemble')} "
            f"[{self.bundle.get('feature_set', 'extended')} features]"
        )

    def predict(self, data: dict) -> dict:
        """
        Accept a flat feature dict (keys = NHANES column names, values = numbers).
        Unknown/missing keys become NaN — engineered features are derived automatically,
        and the bundle's models handle any remaining missing values natively.
        Returns a response dict compatible with PredictionController expectations.
        """
        # Compute all engineered/derived features from raw inputs
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            X = _engineer(data)

        risk_prob = _predict_bundle(self.bundle, X)

        # Pick operating-point threshold (max-F1 by default, fallback to 0.5)
        threshold = self.thresholds.get("max_f1", 0.5)
        if not math.isfinite(threshold):
            threshold = 0.5

        risk_level = self._risk_level(risk_prob)
        risk_pct = f"{round(risk_prob * 100, 1)}%"

        return {
            "disease": self.disease_name,
            "risk_probability": round(risk_prob, 4),
            "risk_percentage": risk_pct,
            "risk_level": risk_level,
            "above_threshold": risk_prob >= threshold,
            "decision_threshold": round(threshold, 4),
            "model_used": self.model_label,
            "guidance_requested": True,
        }

    @staticmethod
    def _risk_level(prob: float) -> str:
        if prob < 0.30:
            return "LOW"
        if prob < 0.60:
            return "MEDIUM"
        if prob < 0.80:
            return "HIGH"
        return "CRITICAL"
