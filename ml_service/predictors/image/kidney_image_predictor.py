"""
Kidney CT Scan Predictor
Model: taoyh/RenalCLIP (3D Vision-Language Model)
Classes: Cyst / Normal / Stone / Tumor
"""

import os
import numpy as np
from PIL import Image
from .base_image_predictor import BaseImagePredictor


class KidneyImagePredictor(BaseImagePredictor):
    CANCER_TYPE = "Kidney Condition"
    IMAGE_TYPE = "CT Scan"
    MODEL_SOURCE = "taoyh/RenalCLIP"
    CONFIDENCE_THRESHOLD = 0.80
    SEVERITY_MAP = {
        "normal": "LOW",
        "cyst": "MEDIUM",
        "stone": "MEDIUM",
        "tumor": "CRITICAL"
    }
    CLASSES = ["Cyst", "Normal", "Stone", "Tumor"]

    def __init__(self):
        from transformers import AutoProcessor, AutoModel
        import torch
        model_path = os.path.join(self.MODEL_DIR, "kidney_renal_clip")
        if os.path.isdir(model_path):
            self.processor = AutoProcessor.from_pretrained(model_path)
            self.model = AutoModel.from_pretrained(model_path)
        else:
            self.processor = AutoProcessor.from_pretrained("taoyh/RenalCLIP")
            self.model = AutoModel.from_pretrained("taoyh/RenalCLIP")
            os.makedirs(model_path, exist_ok=True)
            self.processor.save_pretrained(model_path)
            self.model.save_pretrained(model_path)
        self.model.eval()

    def predict(self, image: Image.Image) -> dict:
        import torch
        # Use text-image similarity (CLIP-style)
        texts = [f"a CT scan showing kidney {c.lower()}" for c in self.CLASSES]
        inputs = self.processor(text=texts, images=image, return_tensors="pt", padding=True)
        with torch.no_grad():
            outputs = self.model(**inputs)
        # Get image-text similarity scores
        logits = outputs.logits_per_image.squeeze()
        probs = self._softmax(logits.numpy())
        idx = int(np.argmax(probs))
        predicted_class = self.CLASSES[idx]
        confidence = float(probs[idx])
        all_scores = {self.CLASSES[i]: round(float(p), 4) for i, p in enumerate(probs)}
        return self._build_result(predicted_class, confidence, all_scores)
