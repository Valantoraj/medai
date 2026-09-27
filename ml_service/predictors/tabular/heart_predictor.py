"""
Heart Disease Predictor
Model: Juan12Dev/heart-risk-ai-v4 (Stacked Ensemble: RF+XGBoost+LightGBM+CatBoost+GBM+LR)
Inputs (13): age, sex, chest_pain_type, resting_bp, cholesterol, fasting_blood_sugar,
             resting_ecg, max_heart_rate, exercise_angina, st_depression,
             slope_of_st, num_vessels, thal
"""

import numpy as np
from .base_predictor import BaseTabularPredictor


class HeartPredictor(BaseTabularPredictor):

    FEATURES = [
        "age", "sex", "chest_pain_type", "resting_bp", "cholesterol",
        "fasting_blood_sugar", "resting_ecg", "max_heart_rate",
        "exercise_angina", "st_depression", "slope_of_st", "num_vessels", "thal"
    ]

    def __init__(self):
        super().__init__(
            model_filename="heart_disease_model.pkl",
            disease_name="Heart Disease",
            model_source="Juan12Dev/heart-risk-ai-v4"
        )

    def _prepare_features(self, data: dict) -> np.ndarray:
        return np.array([float(data.get(f, 0)) for f in self.FEATURES])
