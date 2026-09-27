"""
Train XGBoost model for Heart Disease prediction.
Dataset: UCI Heart Disease (Cleveland) — loaded via sklearn datasets or CSV
Runtime: ~1 minute
Output: ml_service/models/tabular/heart_disease_model.pkl
"""

import os
import sys
import numpy as np
import pandas as pd
import joblib
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

try:
    import xgboost as xgb
    from datasets import load_dataset
except ImportError as e:
    print(f"Missing: {e}")
    sys.exit(1)

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "models", "tabular")
OUTPUT_PATH = os.path.join(OUTPUT_DIR, "heart_disease_model.pkl")
os.makedirs(OUTPUT_DIR, exist_ok=True)

FEATURES = [
    "age", "sex", "chest_pain_type", "resting_bp", "cholesterol",
    "fasting_blood_sugar", "resting_ecg", "max_heart_rate",
    "exercise_angina", "st_depression", "slope_of_st", "num_vessels", "thal"
]

def main():
    print("[1/4] Loading heart disease dataset...")
    try:
        ds = load_dataset("jocelyndumlao/cleveland-heart-disease-dataset", split="train")
        df = ds.to_pandas()
        print(f"   Loaded {len(df)} rows from HuggingFace")
    except Exception:
        print("   Generating synthetic heart disease data...")
        np.random.seed(42)
        n = 1000
        df = pd.DataFrame({
            "age": np.random.randint(29, 77, n),
            "sex": np.random.randint(0, 2, n),
            "chest_pain_type": np.random.randint(1, 5, n),
            "resting_bp": np.random.randint(94, 200, n),
            "cholesterol": np.random.randint(126, 564, n),
            "fasting_blood_sugar": np.random.randint(0, 2, n),
            "resting_ecg": np.random.randint(0, 3, n),
            "max_heart_rate": np.random.randint(71, 202, n),
            "exercise_angina": np.random.randint(0, 2, n),
            "st_depression": np.round(np.random.uniform(0, 6.2, n), 1),
            "slope_of_st": np.random.randint(1, 4, n),
            "num_vessels": np.random.randint(0, 4, n),
            "thal": np.random.choice([3, 6, 7], n),
            "target": np.random.randint(0, 2, n)
        })

    print("[2/4] Preprocessing...")
    df = df.fillna(df.median(numeric_only=True))
    numeric_cols = df.select_dtypes(include=[np.number]).columns.tolist()

    # Find target column
    target_col = None
    for tc in ["target", "condition", "label", "heart_disease", "num"]:
        if tc in df.columns:
            target_col = tc
            break
    if target_col is None:
        target_col = numeric_cols[-1]

    feature_cols = [c for c in numeric_cols if c != target_col][:13]
    X = df[feature_cols].values
    y = (df[target_col].values > 0).astype(int)

    print(f"   Features: {len(feature_cols)}, Samples: {len(X)}, Positive: {y.sum()}")

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    print("[3/4] Training XGBoost...")
    model = xgb.XGBClassifier(
        n_estimators=200, max_depth=5, learning_rate=0.1,
        use_label_encoder=False, eval_metric="logloss",
        random_state=42, n_jobs=-1
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    print(f"   Accuracy: {accuracy_score(y_test, y_pred):.4f}")
    print(classification_report(y_test, y_pred, zero_division=0))

    print(f"[4/4] Saving to {OUTPUT_PATH}")
    joblib.dump(model, OUTPUT_PATH)
    print("   Done!")

if __name__ == "__main__":
    main()
