"""
Brain Tumor MRI Predictor
Model: Abuzaid01/brain-tumor-resnet50-classifier (ResNet50, 224x224 MRI)
Classes: Glioma / Meningioma / Pituitary / No Tumor
"""

import os
import numpy as np
from PIL import Image
from .base_image_predictor import BaseImagePredictor


class BrainPredictor(BaseImagePredictor):
    CANCER_TYPE = "Brain Tumor"
    IMAGE_TYPE = "MRI"
    MODEL_SOURCE = "Abuzaid01/brain-tumor-resnet50-classifier"
    CONFIDENCE_THRESHOLD = 0.75
    SEVERITY_MAP = {
        "no tumor": "LOW",
        "meningioma": "HIGH",
        "pituitary": "HIGH",
        "glioma": "CRITICAL"
    }

    def __init__(self):
        from transformers import AutoFeatureExtractor, AutoModelForImageClassification
        import torch
        model_path = os.path.join(self.MODEL_DIR, "brain_tumor")
        if os.path.isdir(model_path):
            self.extractor = AutoFeatureExtractor.from_pretrained(model_path)
            self.model = AutoModelForImageClassification.from_pretrained(model_path)
        else:
            self.extractor = AutoFeatureExtractor.from_pretrained(
                "Abuzaid01/brain-tumor-resnet50-classifier")
            self.model = AutoModelForImageClassification.from_pretrained(
                "Abuzaid01/brain-tumor-resnet50-classifier")
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
