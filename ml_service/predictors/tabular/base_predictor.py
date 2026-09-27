"""
Base class for all tabular ML predictors.
Handles model loading, risk level calculation, and standard output format.
"""

import os
import joblib
import numpy as np


class BaseTabularPredictor:
    MODEL_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "models", "tabular")

    def __init__(self, model_filename, disease_name, model_source):
        self.model_path = os.path.join(self.MODEL_DIR, model_filename)
        self.disease_name = disease_name
        self.model_source = model_source
        self.model = self._load_model()

    def _load_model(self):
        if not os.path.exists(self.model_path):
            raise FileNotFoundError(
                f"Model file not found: {self.model_path}\n"
                f"Run the download/training script first:\n"
                f"  python ml_service/train/download_models.py"
            )
        return joblib.load(self.model_path)

    def _prepare_features(self, data: dict) -> np.ndarray:
        """Override in subclass to extract and order features."""
        raise NotImplementedError

    def predict(self, data: dict) -> dict:
        features = self._prepare_features(data)
        features_2d = features.reshape(1, -1)

        # Get probability of positive class
        if hasattr(self.model, "predict_proba"):
            proba = self.model.predict_proba(features_2d)[0]
            # Handle binary and multiclass
            risk_prob = float(proba[1]) if len(proba) == 2 else float(max(proba))
        else:
            pred = self.model.predict(features_2d)[0]
            risk_prob = float(pred)

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

    @staticmethod
    def _risk_level(prob: float) -> str:
        if prob < 0.30:
            return "LOW"
        elif prob < 0.60:
            return "MEDIUM"
        elif prob < 0.80:
            return "HIGH"
        else:
            return "CRITICAL"
