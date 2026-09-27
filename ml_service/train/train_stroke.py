"""
Train Random Forest model for Stroke prediction.
Dataset: fedesoriano/stroke-prediction-dataset via HuggingFace or synthetic
Runtime: ~1 minute
Output: ml_service/models/tabular/stroke_model.pkl
"""

import os
import sys
import numpy as np
import pandas as pd
import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score
from sklearn.preprocessing import LabelEncoder

try:
    from datasets import load_dataset
except ImportError:
    print("Run: pip install datasets")
    sys.exit(1)

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "models", "tabular")
OUTPUT_PATH = os.path.join(OUTPUT_DIR, "stroke_model.pkl")
os.makedirs(OUTPUT_DIR, exist_ok=True)

def main():
    print("[1/4] Loading stroke dataset...")
    df = None
    for repo in ["fedesoriano/stroke-prediction-dataset",
                 "datasets/stroke-prediction"]:
        try:
            ds = load_dataset(repo, split="train")
            df = ds.to_pandas()
            print(f"   Loaded {len(df)} rows from {repo}")
            break
        except Exception:
            continue

    if df is None:
        print("   Generating synthetic stroke data...")
        np.random.seed(42)
        n = 5110
        df = pd.DataFrame({
            "age": np.random.uniform(0.08, 82, n),
            "gender": np.random.choice(["Male", "Female", "Other"], n),
            "hypertension": np.random.randint(0, 2, n),
            "heart_disease_history": np.random.randint(0, 2, n),
            "ever_married": np.random.choice(["Yes", "No"], n),
            "work_type": np.random.choice(["Private", "Self-employed", "Govt_job", "children", "Never_worked"], n),
            "residence_type": np.random.choice(["Urban", "Rural"], n),
            "avg_glucose_level": np.random.uniform(55, 272, n),
            "bmi": np.random.uniform(10, 98, n),
            "smoking_status": np.random.choice(["formerly smoked", "never smoked", "smokes", "Unknown"], n),
            "stroke": np.random.choice([0, 1], n, p=[0.95, 0.05])
        })

    print("[2/4] Preprocessing...")
    df = df.fillna(df.median(numeric_only=True))

    # Encode categoricals
    le = LabelEncoder()
    for col in df.select_dtypes(include="object").columns:
        df[col] = le.fit_transform(df[col].astype(str))

    # Find target
    target_col = None
    for tc in ["stroke", "target", "label"]:
        if tc in df.columns:
            target_col = tc
            break
    if target_col is None:
        target_col = df.columns[-1]

    numeric_cols = df.select_dtypes(include=[np.number]).columns.tolist()
    feature_cols = [c for c in numeric_cols if c != target_col]
    X = df[feature_cols].values
    y = df[target_col].values.astype(int)
    print(f"   Features: {len(feature_cols)}, Samples: {len(X)}, Positive: {y.sum()}")

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    print("[3/4] Training Random Forest with class balance...")
    model = RandomForestClassifier(
        n_estimators=200, max_depth=10, random_state=42,
        n_jobs=-1, class_weight="balanced"
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
