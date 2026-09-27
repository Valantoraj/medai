"""
Download pretrained tabular models from HuggingFace Hub.
Run this ONCE before starting the ML service:
    cd ml_service
    python train/download_models.py
"""

import os
import sys
import joblib

try:
    from huggingface_hub import hf_hub_download, snapshot_download
except ImportError:
    print("huggingface_hub not installed. Run: pip install huggingface_hub")
    sys.exit(1)

MODELS_DIR = os.path.join(os.path.dirname(__file__), "..", "models", "tabular")
os.makedirs(MODELS_DIR, exist_ok=True)


def download_heart():
    print("[1/3] Downloading Heart Disease model (Juan12Dev/heart-risk-ai-v4)...")
    try:
        path = hf_hub_download(
            repo_id="Juan12Dev/heart-risk-ai-v4",
            filename="heart_risk_model.pkl",
            local_dir=MODELS_DIR
        )
        # Rename to expected filename
        dest = os.path.join(MODELS_DIR, "heart_disease_model.pkl")
        if not os.path.exists(dest):
            os.rename(path, dest)
        print(f"   Saved: {dest}")
    except Exception as e:
        print(f"   WARNING: Could not download heart model: {e}")
        print("   You may need to train it manually or check model availability.")


def download_stroke():
    print("[2/3] Downloading Stroke model (emlacodeuse/ml-stroke-prediction)...")
    try:
        path = hf_hub_download(
            repo_id="emlacodeuse/ml-stroke-prediction",
            filename="stroke_model.pkl",
            local_dir=MODELS_DIR
        )
        dest = os.path.join(MODELS_DIR, "stroke_model.pkl")
        if path != dest and not os.path.exists(dest):
            os.rename(path, dest)
        print(f"   Saved: {dest}")
    except Exception as e:
        print(f"   WARNING: Could not download stroke model: {e}")


def download_diabetes():
    print("[3/3] Downloading Diabetes model (shabari-vignesh8/Diabetes-prediction)...")
    try:
        # Try common filename patterns
        for fname in ["diabetes_model.pkl", "model.pkl", "diabetes_prediction.pkl"]:
            try:
                path = hf_hub_download(
                    repo_id="shabari-vignesh8/Diabetes-prediction",
                    filename=fname,
                    local_dir=MODELS_DIR
                )
                dest = os.path.join(MODELS_DIR, "diabetes_model.pkl")
                if path != dest and not os.path.exists(dest):
                    os.rename(path, dest)
                print(f"   Saved: {dest}")
                break
            except Exception:
                continue
        else:
            print("   WARNING: Could not download diabetes model. Run train/train_diabetes.py instead.")
    except Exception as e:
        print(f"   WARNING: {e}")


if __name__ == "__main__":
    print("=" * 60)
    print("  MedAI — Downloading Pretrained Tabular Models")
    print("=" * 60)
    download_heart()
    download_stroke()
    download_diabetes()
    print()
    print("Done! Now run the training scripts for lung, kidney, and liver:")
    print("  python train/train_lung_tabular.py")
    print("  python train/train_kidney_tabular.py")
    print("  python train/train_liver.py")
