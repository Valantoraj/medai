"""
Lung Cancer Risk Factor Predictor (tabular)
Model: XGBoost trained on nateraw/lung-cancer dataset
Inputs (15): age, gender, air_pollution, alcohol_use, dust_allergy,
             occupational_hazards, genetic_risk, chronic_lung_disease,
             balanced_diet, obesity, smoking, passive_smoker,
             chest_pain, coughing_of_blood, fatigue
"""

import numpy as np
from .base_predictor import BaseTabularPredictor


class LungTabularPredictor(BaseTabularPredictor):

    FEATURES = [
        "age", "gender", "air_pollution", "alcohol_use", "dust_allergy",
        "occupational_hazards", "genetic_risk", "chronic_lung_disease",
        "balanced_diet", "obesity", "smoking", "passive_smoker",
        "chest_pain", "coughing_of_blood", "fatigue"
    ]

    def __init__(self):
        super().__init__(
            model_filename="lung_cancer_tabular_model.pkl",
            disease_name="Lung Cancer (Risk Factors)",
            model_source="XGBoost trained on nateraw/lung-cancer"
        )

    def _prepare_features(self, data: dict) -> np.ndarray:
        return np.array([float(data.get(f, 0)) for f in self.FEATURES])
