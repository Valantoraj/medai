"""YOLO inference adapter for the locally trained cancer models."""
import base64
import io
from pathlib import Path

import numpy as np
import torch
from PIL import Image as PILImage
from ultralytics import YOLO


SERVICE_DIR = Path(__file__).resolve().parents[2]

MODEL_CONFIG = {
    "lung": {
        # Place lung_cancer_model.pt in the ml_service/ directory
        "paths": ["lung_cancer_model.pt"],
        "cancer_type": "Lung Cancer",
        "image_type": "CT Scan", "imgsz": 1024, "conf": 0.05, "iou": 0.6, "threshold": 0.50,
        "preprocess": "ct_grayscale",
    },
    "liver": {
        # Place best.pt at ml_service/runs/liver_cancer_detect_v2/liver_cancer_yolo_fold0/weights/best.pt
        "paths": ["runs/liver_cancer_detect_v2/liver_cancer_yolo_fold0/weights/best.pt"],
        "cancer_type": "Liver Cancer",
        "image_type": "CT Scan", "imgsz": 1024, "conf": 0.15, "iou": 0.6, "threshold": 0.50,
        "preprocess": "ct_grayscale",
    },
    "blood": {
        # Place blood_cancer_model.pt in the ml_service/ directory
        "paths": ["blood_cancer_model.pt"],
        "cancer_type": "Blood Cancer / Leukemia",
        "image_type": "Blood Smear", "imgsz": 640, "conf": 0.25, "iou": 0.6, "threshold": 0.70,
        "preprocess": "rgb",
    },
    "kidney": {
        # Place kidney_cancer_cyst_stone_model.pt in the ml_service/ directory
        "paths": ["kidney_cancer_cyst_stone_model.pt"],
        "cancer_type": "Kidney Cancer / Lesions",
        "image_type": "CT Scan", "imgsz": 960, "conf": 0.25, "iou": 0.6, "threshold": 0.25,
        # Trained with cv2.imread(GRAYSCALE) → np.dstack([g,g,g])
        "preprocess": "ct_grayscale",
    },
    "skin": {
        # Place skin_cancer_model.pt in the ml_service/ directory
        "paths": ["skin_cancer_model.pt"],
        "cancer_type": "Skin Cancer",
        "image_type": "Dermoscopy", "imgsz": 640, "conf": 0.25, "iou": 0.6, "threshold": 0.70,
        "preprocess": "rgb",
    },
    "brain": {
        # Place best.pt at ml_service/runs/detect/runs/detect/brain_tumor_yolo/weights/best.pt
        "paths": ["runs/detect/runs/detect/brain_tumor_yolo/weights/best.pt"],
        "cancer_type": "Brain Tumor", "image_type": "MRI",
        "imgsz": 640, "conf": 0.25, "iou": 0.6, "threshold": 0.70,
        # Trained with standard cv2.imread() (color) on Kaggle 12k MRI JPEGs
        "preprocess": "rgb",
    },
}


def _name(names, class_id):
    if isinstance(names, dict):
        return str(names.get(class_id, names.get(str(class_id), class_id)))
    return str(names[class_id]) if 0 <= class_id < len(names) else str(class_id)


def _severity(cancer, label):
    name = label.lower()
    if any(token in name for token in ("normal", "benign", "no tumor", "no finding")):
        return "LOW"
    if cancer == "skin":
        if name in ("mel", "melanoma"):
            return "CRITICAL"
        if name in ("bcc", "scc") or "carcinoma" in name:
            return "HIGH"
        return "LOW"
    if cancer == "kidney":
        if "tumor" in name or "cancer" in name:
            return "CRITICAL"
        if "cyst" in name or "stone" in name:
            return "MEDIUM"
        return "HIGH"
    if any(token in name for token in ("tumor", "malignant", "leukemic", "blast", "cancer", "glioma")):
        return "CRITICAL"
    return "HIGH"


class YoloCancerPredictor:
    def __init__(self, cancer):
        if cancer not in MODEL_CONFIG:
            raise ValueError(f"Unknown image prediction task '{cancer}'.")
        self.cancer = cancer
        self.config = MODEL_CONFIG[cancer]
        # Accept absolute paths as-is; resolve relative paths against SERVICE_DIR
        candidates = [
            Path(item) if Path(item).is_absolute() else SERVICE_DIR / item
            for item in self.config["paths"]
        ]
        self.model_path = next((path for path in candidates if path.is_file()), candidates[0])
        if not self.model_path.is_file():
            expected = ", ".join(str(path) for path in candidates)
            raise FileNotFoundError(f"Trained {self.config['cancer_type']} model not found. Checked: {expected}")
        self.model = YOLO(str(self.model_path))

    def _prepare_source(self, image: PILImage.Image) -> np.ndarray:
        """
        Convert an incoming PIL image to the numpy array format each model
        was trained with.

        "rgb"          — standard colour image (blood smear, dermoscopy).
                         PIL .convert("RGB") → uint8 H×W×3.

        "ct_grayscale" — CT/MRI scans trained with OpenCV's grayscale pipeline:
                         cv2.imread(path, IMREAD_GRAYSCALE) → np.dstack([g,g,g])
                         Replicating this via PIL: convert to L (luminance) then
                         stack the single channel three times.  This preserves
                         the same per-pixel intensity values that YOLO saw during
                         training and is what test.py's load_2d_image() does.
        """
        mode = self.config.get("preprocess", "rgb")
        if mode == "ct_grayscale":
            gray = np.array(image.convert("L"))          # H×W uint8
            return np.dstack([gray, gray, gray])          # H×W×3, same 3 channels
        # default: colour image
        return np.asarray(image.convert("RGB"))

    def predict(self, image: PILImage.Image) -> dict:
        source = self._prepare_source(image)

        results = self.model.predict(
            source=source,
            imgsz=self.config["imgsz"],
            conf=self.config["conf"],
            iou=self.config["iou"],
            device=0 if torch.cuda.is_available() else "cpu",
            verbose=False,
        )
        result = results[0]
        names = result.names
        scores = {}

        # Ultralytics classification checkpoints (including the blood model) expose probs.
        if result.probs is not None:
            probabilities = result.probs.data.detach().cpu().numpy().reshape(-1)
            scores = {_name(names, i): round(float(score), 4) for i, score in enumerate(probabilities)}
            class_id = int(np.argmax(probabilities))
            label = _name(names, class_id)
            confidence = float(probabilities[class_id])
        elif result.boxes is not None and len(result.boxes):
            boxes = result.boxes
            confidences = boxes.conf.detach().cpu().numpy()
            class_ids = boxes.cls.detach().cpu().numpy().astype(int)
            for class_id, score in zip(class_ids, confidences):
                label_name = _name(names, int(class_id))
                scores[label_name] = max(scores.get(label_name, 0.0), round(float(score), 4))
            best = int(np.argmax(confidences))
            label = _name(names, int(class_ids[best]))
            confidence = float(confidences[best])
        else:
            label = "No finding detected"
            confidence = 0.0

        # ── Annotated image ──────────────────────────────────────────────
        # result.plot() returns an RGB numpy array with bounding boxes /
        # classification labels drawn by Ultralytics.  For classification-only
        # models (result.probs) the original image is returned with the top-1
        # class and confidence overlaid as a label bar.
        annotated_b64 = None
        try:
            annotated_arr = result.plot()          # RGB np.ndarray
            pil_img = PILImage.fromarray(annotated_arr)
            buf = io.BytesIO()
            pil_img.save(buf, format="JPEG", quality=88)
            annotated_b64 = base64.b64encode(buf.getvalue()).decode("utf-8")
        except Exception:
            annotated_b64 = None

        return {
            "cancer_type": self.config["cancer_type"],
            "predicted_class": label,
            "confidence": round(confidence, 4),
            "confidence_pct": f"{round(confidence * 100, 1)}%",
            "above_threshold": confidence >= self.config["threshold"],
            "image_type_required": self.config["image_type"],
            "model_used": str(self.model_path),
            "severity": _severity(self.cancer, label),
            "guidance_requested": True,
            "all_scores": scores,
            "annotated_image_b64": annotated_b64,
        }
