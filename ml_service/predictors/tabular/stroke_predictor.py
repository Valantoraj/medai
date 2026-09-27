"""
Stroke Predictor
Model: GradientBoosting trained on real fedesoriano stroke dataset
Inputs (10): age, gender, hypertension, heart_disease_history, ever_married,
             work_type, residence_type, avg_glucose_level, bmi, smoking_status

Note: Model is saved as a bundle dict {"model": ..., "threshold": float}
to use an optimised decision threshold for better stroke recall.
"""

import numpy as np
import joblib
import os
from .base_predictor import BaseTabularPredictor


class StrokePredictor(BaseTabularPredictor):

    FEATURES = [
        "age", "gender", "hypertension", "heart_disease_history",
        "ever_married", "work_type", "residence_type",
        "avg_glucose_level", "bmi", "smoking_status"
    ]

    def __init__(self):
        super().__init__(
            model_filename="stroke_model.pkl",
            disease_name="Stroke",
            model_source="GradientBoosting on fedesoriano/stroke-prediction"
        )
        # Reload and unwrap bundle if needed
        raw = joblib.load(self.model_path)
        if isinstance(raw, dict) and "model" in raw:
            self.threshold = raw.get("threshold", 0.3)
            self.model = raw["model"]
        else:
            self.threshold = 0.3
            self.model = raw

    def _prepare_features(self, data: dict) -> np.ndarray:
        return np.array([float(data.get(f, 0)) for f in self.FEATURES])

    def predict(self, data: dict) -> dict:
        features = self._prepare_features(data).reshape(1, -1)

        # Use optimised threshold instead of default 0.5
        if hasattr(self.model, "predict_proba"):
            proba = self.model.predict_proba(features)[0]
            risk_prob = float(proba[1])
        else:
            risk_prob = float(self.model.predict(features)[0])

        risk_level = self._risk_level(risk_prob)
        risk_pct = f"{round(risk_prob * 100, 1)}%"

        return {
            "disease": self.disease_name,
            "risk_probability": round(risk_prob, 4),
            "risk_level": risk_level,
            "risk_percentage": risk_pct,
            "model_used": self.model_source,
            "guidance_requested": True
        }
