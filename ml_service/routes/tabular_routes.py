"""
JSON prediction routes backed by the saved train_ml_models.py bundles.
"""
from flask import Blueprint, jsonify, request
from predictors.tabular.trained_model_predictor import TrainedModelPredictor

tabular_bp = Blueprint("tabular", __name__)
_predictors = {}


def predict(task):
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Provide prediction fields as a JSON object."}), 400
    try:
        if task not in _predictors:
            _predictors[task] = TrainedModelPredictor(task)
        return jsonify(_predictors[task].predict(data))
    except ValueError as e:
        return jsonify({"error": str(e)}), 400
    except FileNotFoundError as e:
        return jsonify({"error": str(e)}), 503
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@tabular_bp.route("/predict/heart", methods=["POST"])
def predict_heart():
    return predict("heart")


@tabular_bp.route("/predict/stroke", methods=["POST"])
def predict_stroke():
    return predict("stroke")


@tabular_bp.route("/predict/diabetes", methods=["POST"])
def predict_diabetes():
    return predict("diabetes")


@tabular_bp.route("/predict/lung-tabular", methods=["POST"])
def predict_lung():
    return predict("lung-tabular")


@tabular_bp.route("/predict/kidney-tabular", methods=["POST"])
def predict_kidney():
    return predict("kidney-tabular")


@tabular_bp.route("/predict/liver", methods=["POST"])
def predict_liver():
    return predict("liver")
