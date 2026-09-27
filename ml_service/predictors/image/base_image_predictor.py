"""
Base class for image-based cancer predictors.
Provides standard output format and severity mapping.
"""

import os


class BaseImagePredictor:
    MODEL_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "models", "image")

    # Subclasses override these
    CANCER_TYPE = "Unknown"
    IMAGE_TYPE = "Medical Image"
    MODEL_SOURCE = "Unknown"
    CONFIDENCE_THRESHOLD = 0.70

    # Map predicted class → severity for hospital trigger logic
    SEVERITY_MAP = {}  # Override in subclasses

    def predict(self, image) -> dict:
        """Run inference; returns standard output dict."""
        raise NotImplementedError

    def _build_result(self, predicted_class: str, confidence: float, all_scores: dict = None) -> dict:
        above_threshold = confidence >= self.CONFIDENCE_THRESHOLD
        severity = self.SEVERITY_MAP.get(predicted_class.lower(), "MEDIUM")

        return {
            "cancer_type": self.CANCER_TYPE,
            "predicted_class": predicted_class,
            "confidence": round(confidence, 4),
            "confidence_pct": f"{round(confidence * 100, 1)}%",
            "above_threshold": above_threshold,
            "image_type_required": self.IMAGE_TYPE,
            "model_used": self.MODEL_SOURCE,
            "severity": severity,
            "guidance_requested": True,
            "all_scores": all_scores or {}
        }

    @staticmethod
    def _softmax(logits):
        import numpy as np
        e = np.exp(logits - np.max(logits))
        return e / e.sum()
