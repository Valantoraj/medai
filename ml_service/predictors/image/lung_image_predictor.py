"""
Lung Cancer CT Scan Predictor
Model: jawbra/lungevaty (Transformer-based, LDCT)
Classes: Normal / Benign / Malignant
"""

import os
import numpy as np
from PIL import Image
from .base_image_predictor import BaseImagePredictor


class LungImagePredictor(BaseImagePredictor):
    CANCER_TYPE = "Lung Cancer"
    IMAGE_TYPE = "CT Scan"
    MODEL_SOURCE = "jawbra/lungevaty"
    CONFIDENCE_THRESHOLD = 0.80
    SEVERITY_MAP = {
        "normal": "LOW",
        "benign": "MEDIUM",
        "malignant": "CRITICAL"
    }

    def __init__(self):
        from transformers import AutoFeatureExtractor, AutoModelForImageClassification
        import torch
        model_path = os.path.join(self.MODEL_DIR, "lung_cancer_image")
        if os.path.isdir(model_path):
            self.extractor = AutoFeatureExtractor.from_pretrained(model_path)
            self.model = AutoModelForImageClassification.from_pretrained(model_path)
        else:
            # Download from HuggingFace Hub on first run
            self.extractor = AutoFeatureExtractor.from_pretrained("jawbra/lungevaty")
            self.model = AutoModelForImageClassification.from_pretrained("jawbra/lungevaty")
            os.makedirs(model_path, exist_ok=True)
            self.extractor.save_pretrained(model_path)
            self.model.save_pretrained(model_path)
        self.model.eval()

    def predict(self, image: Image.Image) -> dict:
        import torch
        inputs = self.extractor(images=image, return_tensors="pt")
        with torch.no_grad():
            logits = self.model(**inputs).logits
        probs = self._softmax(logits.numpy()[0])
        label_map = self.model.config.id2label
        idx = int(np.argmax(probs))
        predicted_class = label_map[idx].capitalize()
        confidence = float(probs[idx])
        all_scores = {label_map[i]: round(float(p), 4) for i, p in enumerate(probs)}
        return self._build_result(predicted_class, confidence, all_scores)
