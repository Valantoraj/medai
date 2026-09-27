"""
Stroke Predictor
Model: emlacodeuse/ml-stroke-prediction (Random Forest + SMOTE)
Inputs (10): age, gender, hypertension, heart_disease_history, ever_married,
             work_type, residence_type, avg_glucose_level, bmi, smoking_status
"""

import numpy as np
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
            model_source="emlacodeuse/ml-stroke-prediction"
        )

    def _prepare_features(self, data: dict) -> np.ndarray:
        return np.array([float(data.get(f, 0)) for f in self.FEATURES])
