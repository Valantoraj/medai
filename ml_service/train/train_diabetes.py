"""
Train ensemble model for Diabetes prediction.
Dataset: Pima Indians Diabetes — loaded via HuggingFace or synthetic
Runtime: ~1 minute
Output: ml_service/models/tabular/diabetes_model.pkl
"""

import os
import sys
import numpy as np
import pandas as pd
import joblib
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score

try:
    from datasets import load_dataset
except ImportError:
    print("Run: pip install datasets")
    sys.exit(1)

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "models", "tabular")
OUTPUT_PATH = os.path.join(OUTPUT_DIR, "diabetes_model.pkl")
os.makedirs(OUTPUT_DIR, exist_ok=True)

FEATURES = ["pregnancies", "glucose", "blood_pressure", "skin_thickness",
            "insulin", "bmi", "diabetes_pedigree_function", "age"]

def main():
    print("[1/4] Loading diabetes dataset...")
    df = None
    for repo in ["jbrownlee/diabetes", "datasets/diabetes",
                 "scikit-learn/diabetes", "lammoussa/pima-indians-diabetes"]:
        try:
            ds = load_dataset(repo, split="train")
            df = ds.to_pandas()
            print(f"   Loaded {len(df)} rows from {repo}")
            break
        except Exception:
            continue

    if df is None:
        print("   Generating synthetic Pima diabetes data...")
        np.random.seed(42)
        n = 768
        df = pd.DataFrame({
            "pregnancies": np.random.randint(0, 18, n),
            "glucose": np.random.randint(0, 200, n),
            "blood_pressure": np.random.randint(0, 122, n),
            "skin_thickness": np.random.randint(0, 99, n),
            "insulin": np.random.randint(0, 846, n),
            "bmi": np.round(np.random.uniform(0, 67.1, n), 1),
            "diabetes_pedigree_function": np.round(np.random.uniform(0.078, 2.42, n), 3),
            "age": np.random.randint(21, 82, n),
            "outcome": np.random.choice([0, 1], n, p=[0.65, 0.35])
        })

    print("[2/4] Preprocessing...")
    df = df.fillna(df.median(numeric_only=True))
    numeric_cols = df.select_dtypes(include=[np.number]).columns.tolist()

    target_col = None
    for tc in ["outcome", "Outcome", "target", "label", "diabetes"]:
        if tc in df.columns:
            target_col = tc
            break
    if target_col is None:
        target_col = numeric_cols[-1]

    feature_cols = [c for c in numeric_cols if c != target_col]
    X = df[feature_cols].values
    y = df[target_col].values.astype(int)
    print(f"   Features: {len(feature_cols)}, Samples: {len(X)}, Positive: {y.sum()}")

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    print("[3/4] Training Gradient Boosting...")
    model = GradientBoostingClassifier(
        n_estimators=200, max_depth=4, learning_rate=0.1, random_state=42
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
