"""
Kidney Stone Predictor (clinical inputs)
Model: Random Forest trained on Euniceyeee/kidney-ct-abnormality
Inputs (6): urine_gravity, urine_ph, urine_osmolality,
            urine_conductivity, urea, calcium
"""

import numpy as np
from .base_predictor import BaseTabularPredictor


class KidneyPredictor(BaseTabularPredictor):

    FEATURES = [
        "urine_gravity", "urine_ph", "urine_osmolality",
        "urine_conductivity", "urea", "calcium"
    ]

    def __init__(self):
        super().__init__(
            model_filename="kidney_stone_model.pkl",
            disease_name="Kidney Stone",
            model_source="Random Forest on Euniceyeee/kidney-ct-abnormality"
        )

    def _prepare_features(self, data: dict) -> np.ndarray:
        return np.array([float(data.get(f, 0)) for f in self.FEATURES])
