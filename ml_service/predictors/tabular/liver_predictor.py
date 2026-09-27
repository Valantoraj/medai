"""
Liver Disease Predictor
Model: XGBoost trained on Francesco/liver-disease dataset
Inputs (10): age, gender, total_bilirubin, direct_bilirubin,
             alkaline_phosphotase, alamine_aminotransferase,
             aspartate_aminotransferase, total_proteins,
             albumin, albumin_globulin_ratio
"""

import numpy as np
from .base_predictor import BaseTabularPredictor


class LiverPredictor(BaseTabularPredictor):

    FEATURES = [
        "age", "gender", "total_bilirubin", "direct_bilirubin",
        "alkaline_phosphotase", "alamine_aminotransferase",
        "aspartate_aminotransferase", "total_proteins",
        "albumin", "albumin_globulin_ratio"
    ]

    def __init__(self):
        super().__init__(
            model_filename="liver_disease_model.pkl",
            disease_name="Liver Disease",
            model_source="XGBoost on Francesco/liver-disease"
        )

    def _prepare_features(self, data: dict) -> np.ndarray:
        return np.array([float(data.get(f, 0)) for f in self.FEATURES])
