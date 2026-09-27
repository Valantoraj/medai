"""
Train XGBoost model for Liver Disease prediction.
Dataset: Francesco/liver-disease (HuggingFace)
Runtime: ~2 minutes
Output: ml_service/models/tabular/liver_disease_model.pkl

Usage:
    cd ml_service
    python train/train_liver.py
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
OUTPUT_PATH = os.path.join(OUTPUT_DIR, "liver_disease_model.pkl")
os.makedirs(OUTPUT_DIR, exist_ok=True)

FEATURE_COLS = [
    "Age", "Gender", "Total_Bilirubin", "Direct_Bilirubin",
    "Alkaline_Phosphotase", "Alamine_Aminotransferase",
    "Aspartate_Aminotransferase", "Total_Protiens",
    "Albumin", "Albumin_and_Globulin_Ratio"
]
TARGET_COL = "Dataset"  # 1 = liver disease, 2 = no disease


def main():
    print("[1/4] Loading dataset from HuggingFace: Francesco/liver-disease...")
    try:
        ds = load_dataset("Francesco/liver-disease", split="train")
        df = ds.to_pandas()
    except Exception as e:
        print(f"Could not load from HuggingFace: {e}")
        print("Trying to load Indian Liver Patient Dataset CSV...")
        csv_path = os.path.join(os.path.dirname(__file__), "liver.csv")
        if os.path.exists(csv_path):
            df = pd.read_csv(csv_path)
        else:
            print("Generating synthetic liver disease data...")
            np.random.seed(42)
            n = 583
            df = pd.DataFrame({
                "Age": np.random.randint(4, 90, n),
                "Gender": np.random.choice(["Male", "Female"], n),
                "Total_Bilirubin": np.random.exponential(1.5, n),
                "Direct_Bilirubin": np.random.exponential(0.5, n),
                "Alkaline_Phosphotase": np.random.randint(63, 2110, n),
                "Alamine_Aminotransferase": np.random.randint(10, 2000, n),
                "Aspartate_Aminotransferase": np.random.randint(10, 4929, n),
                "Total_Protiens": np.random.uniform(2.7, 9.6, n),
                "Albumin": np.random.uniform(0.9, 5.5, n),
                "Albumin_and_Globulin_Ratio": np.random.uniform(0.3, 2.8, n),
                TARGET_COL: np.random.randint(1, 3, n)
            })

    print(f"   Loaded {len(df)} rows")

    print("[2/4] Preprocessing...")
    # Encode Gender
    gender_col = None
    for c in df.columns:
        if "gender" in c.lower() or "sex" in c.lower():
            gender_col = c
            break
    if gender_col:
        df[gender_col] = LabelEncoder().fit_transform(df[gender_col].astype(str))

    # Handle missing values
    df = df.fillna(df.median(numeric_only=True))

    # Determine feature columns
    numeric_cols = df.select_dtypes(include=[np.number]).columns.tolist()
    target_col = None
    for tc in [TARGET_COL, "label", "target", "class", "output"]:
        if tc in df.columns:
            target_col = tc
            break
    if target_col is None:
        target_col = numeric_cols[-1]

    feature_cols = [c for c in numeric_cols if c != target_col]
    X = df[feature_cols].values
    y = (df[target_col].values == 1).astype(int)  # 1 = has liver disease

    print(f"   Features: {len(feature_cols)}, Samples: {len(X)}, Positive: {y.sum()}")

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print("[3/4] Training XGBoost classifier...")
    model = xgb.XGBClassifier(
        n_estimators=200,
        max_depth=5,
        learning_rate=0.1,
        use_label_encoder=False,
        eval_metric="logloss",
        random_state=42,
        scale_pos_weight=(y == 0).sum() / (y == 1).sum(),
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
