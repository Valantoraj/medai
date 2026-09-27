"""
Skin Cancer Dermoscopy Predictor
Model: Miguel764/efficientnetv2s-skin-cancer-classifier (EfficientNetV2S)
Classes: 7 lesion types (benign/malignant subtypes)
"""

import os
import numpy as np
from PIL import Image
from .base_image_predictor import BaseImagePredictor


class SkinPredictor(BaseImagePredictor):
    CANCER_TYPE = "Skin Cancer"
    IMAGE_TYPE = "Dermoscopy"
    MODEL_SOURCE = "Miguel764/efficientnetv2s-skin-cancer-classifier"
    CONFIDENCE_THRESHOLD = 0.70
    SEVERITY_MAP = {
        "melanoma": "CRITICAL",
        "basal cell carcinoma": "HIGH",
        "squamous cell carcinoma": "HIGH",
        "benign keratosis": "MEDIUM",
        "dermatofibroma": "LOW",
        "melanocytic nevi": "LOW",
        "vascular lesion": "MEDIUM"
    }

    def __init__(self):
        from transformers import AutoFeatureExtractor, AutoModelForImageClassification
        import torch
        model_path = os.path.join(self.MODEL_DIR, "skin_cancer_model")
        if os.path.isdir(model_path):
            self.extractor = AutoFeatureExtractor.from_pretrained(model_path)
            self.model = AutoModelForImageClassification.from_pretrained(model_path)
        else:
            self.extractor = AutoFeatureExtractor.from_pretrained(
                "Miguel764/efficientnetv2s-skin-cancer-classifier")
            self.model = AutoModelForImageClassification.from_pretrained(
                "Miguel764/efficientnetv2s-skin-cancer-classifier")
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
        predicted_class = label_map[idx]
        confidence = float(probs[idx])
        all_scores = {label_map[i]: round(float(p), 4) for i, p in enumerate(probs)}
        return self._build_result(predicted_class, confidence, all_scores)
