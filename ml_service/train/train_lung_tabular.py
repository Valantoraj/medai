"""
Train XGBoost model for Lung Cancer risk factor prediction.
Dataset: nateraw/lung-cancer (HuggingFace)
Runtime: ~3 minutes
Output: ml_service/models/tabular/lung_cancer_tabular_model.pkl

Usage:
    cd ml_service
    python train/train_lung_tabular.py
"""

import os
import sys
import numpy as np
import pandas as pd
import joblib
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score
from sklearn.preprocessing import LabelEncoder

try:
    import xgboost as xgb
    from datasets import load_dataset
except ImportError as e:
    print(f"Missing dependency: {e}")
    print("Run: pip install xgboost datasets")
    sys.exit(1)

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "models", "tabular")
OUTPUT_PATH = os.path.join(OUTPUT_DIR, "lung_cancer_tabular_model.pkl")
os.makedirs(OUTPUT_DIR, exist_ok=True)

FEATURE_COLS = [
    "Age", "Gender", "Air Pollution", "Alcohol use", "Dust Allergy",
    "OccuPational Hazards", "Genetic Risk", "chronic Lung Disease",
    "Balanced Diet", "Obesity", "Smoking", "Passive Smoker",
    "Chest Pain", "Coughing of Blood", "Fatigue"
]
TARGET_COL = "Level"  # Low / Medium / High


def main():
    print("[1/4] Loading dataset from HuggingFace: nateraw/lung-cancer...")
    try:
        ds = load_dataset("nateraw/lung-cancer", split="train")
        df = ds.to_pandas()
    except Exception as e:
        print(f"Could not load from HuggingFace: {e}")
        print("Attempting to use local CSV if available...")
        csv_path = os.path.join(os.path.dirname(__file__), "lung_cancer.csv")
        if os.path.exists(csv_path):
            df = pd.read_csv(csv_path)
        else:
            print("ERROR: No data source available. Download lung_cancer.csv manually.")
            sys.exit(1)

    print(f"   Loaded {len(df)} rows, columns: {list(df.columns)}")

    print("[2/4] Preprocessing...")
    # Map available columns to our feature names (flexible matching)
    col_map = {}
    for fc in FEATURE_COLS:
        for dc in df.columns:
            if fc.lower().replace(" ", "") == dc.lower().replace(" ", ""):
                col_map[fc] = dc
                break

    # Encode Gender
    if "Gender" in col_map:
        le = LabelEncoder()
        df[col_map["Gender"]] = le.fit_transform(df[col_map["Gender"]].astype(str))

    # Encode target
    if TARGET_COL in df.columns:
        le_target = LabelEncoder()
        y = le_target.fit_transform(df[TARGET_COL].astype(str))
    elif "Level" in df.columns:
        le_target = LabelEncoder()
        y = le_target.fit_transform(df["Level"].astype(str))
    else:
        # Fallback: last column as target
        y = LabelEncoder().fit_transform(df.iloc[:, -1].astype(str))

    # Build feature matrix
    available_cols = [col_map.get(f, f) for f in FEATURE_COLS if col_map.get(f, f) in df.columns]
    X = df[available_cols].fillna(0).values

    print(f"   Features: {len(available_cols)}, Samples: {len(X)}")

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print("[3/4] Training XGBoost classifier...")
    model = xgb.XGBClassifier(
        n_estimators=200,
        max_depth=6,
        learning_rate=0.1,
        use_label_encoder=False,
        eval_metric="mlogloss",
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"   Test Accuracy: {acc:.4f}")
    print(classification_report(y_test, y_pred, zero_division=0))

    print(f"[4/4] Saving model to: {OUTPUT_PATH}")
    joblib.dump(model, OUTPUT_PATH)
    print("   Done!")


if __name__ == "__main__":
    main()
