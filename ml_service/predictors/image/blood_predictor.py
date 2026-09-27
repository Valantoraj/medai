"""
Blood Cancer / Leukemia Blood Smear Predictor
Model: LeukemiaAttri (MICCAI 2024) — CNN/YOLOv8 on blood smear images
Classes: Benign / [Malignant] early Pre-B / [Malignant] Pre-B / [Malignant] Pro-B
"""

import os
import numpy as np
from PIL import Image
from .base_image_predictor import BaseImagePredictor


class BloodPredictor(BaseImagePredictor):
    CANCER_TYPE = "Blood Cancer / Leukemia"
    IMAGE_TYPE = "Blood Smear"
    MODEL_SOURCE = "LeukemiaAttri MICCAI 2024"
    CONFIDENCE_THRESHOLD = 0.70
    SEVERITY_MAP = {
        "benign": "LOW",
        "[malignant] early pre-b": "HIGH",
        "[malignant] pre-b": "CRITICAL",
        "[malignant] pro-b": "CRITICAL"
    }

    CLASSES = [
        "Benign",
        "[Malignant] early Pre-B",
        "[Malignant] Pre-B",
        "[Malignant] Pro-B"
    ]

    def __init__(self):
        import torch
        import torchvision.models as models
        model_path = os.path.join(self.MODEL_DIR, "blood_cancer", "blood_model.pt")
        self.device = torch.device("cpu")

        # Use a ResNet50 architecture (matches LeukemiaAttri paper)
        self.model = models.resnet50(weights=None)
        self.model.fc = torch.nn.Linear(self.model.fc.in_features, len(self.CLASSES))

        if os.path.exists(model_path):
            state = torch.load(model_path, map_location=self.device)
            self.model.load_state_dict(state)
        else:
            # Model weights must be downloaded separately from GitHub
            # See: ml_service/train/download_models.py
            raise FileNotFoundError(
                f"Blood cancer model not found at {model_path}.\n"
                "Download from: https://github.com/intelligentMachines-ITU/Blood-Cancer-Dataset-Lukemia-Attri-MICCAI-2024\n"
                "Then place weights at: ml_service/models/image/blood_cancer/blood_model.pt"
            )
        self.model.eval()

    def predict(self, image: Image.Image) -> dict:
        import torch
        import torchvision.transforms as T
        transform = T.Compose([
            T.Resize((224, 224)),
            T.ToTensor(),
            T.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
        ])
        tensor = transform(image).unsqueeze(0).to(self.device)
        with torch.no_grad():
            logits = self.model(tensor)
        probs = self._softmax(logits.numpy()[0])
        idx = int(np.argmax(probs))
        predicted_class = self.CLASSES[idx]
        confidence = float(probs[idx])
        all_scores = {self.CLASSES[i]: round(float(p), 4) for i, p in enumerate(probs)}
        return self._build_result(predicted_class, confidence, all_scores)
