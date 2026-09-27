"""
Diabetes Predictor
Model: shabari-vignesh8/Diabetes-prediction (Ensemble, Pima dataset)
Inputs (8): pregnancies, glucose, blood_pressure, skin_thickness,
            insulin, bmi, diabetes_pedigree_function, age
"""

import numpy as np
from .base_predictor import BaseTabularPredictor


class DiabetesPredictor(BaseTabularPredictor):

    FEATURES = [
        "pregnancies", "glucose", "blood_pressure", "skin_thickness",
        "insulin", "bmi", "diabetes_pedigree_function", "age"
    ]

    def __init__(self):
        super().__init__(
            model_filename="diabetes_model.pkl",
            disease_name="Diabetes",
            model_source="shabari-vignesh8/Diabetes-prediction"
        )

    def _prepare_features(self, data: dict) -> np.ndarray:
        return np.array([float(data.get(f, 0)) for f in self.FEATURES])
