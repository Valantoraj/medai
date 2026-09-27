"""
Tabular (form-based) disease prediction routes.
All endpoints called by Spring Boot via HTTP POST with JSON body.
"""

from flask import Blueprint, request, jsonify
from predictors.tabular.heart_predictor import HeartPredictor
from predictors.tabular.stroke_predictor import StrokePredictor
from predictors.tabular.diabetes_predictor import DiabetesPredictor
from predictors.tabular.lung_tabular_predictor import LungTabularPredictor
from predictors.tabular.kidney_predictor import KidneyPredictor
from predictors.tabular.liver_predictor import LiverPredictor

tabular_bp = Blueprint("tabular", __name__)

# Lazy-loaded singletons
_heart = None
_stroke = None
_diabetes = None
_lung = None
_kidney = None
_liver = None


def get_heart():
    global _heart
    if _heart is None:
        _heart = HeartPredictor()
    return _heart


def get_stroke():
    global _stroke
    if _stroke is None:
        _stroke = StrokePredictor()
    return _stroke


def get_diabetes():
    global _diabetes
    if _diabetes is None:
        _diabetes = DiabetesPredictor()
    return _diabetes


def get_lung():
    global _lung
    if _lung is None:
        _lung = LungTabularPredictor()
    return _lung


def get_kidney():
    global _kidney
    if _kidney is None:
        _kidney = KidneyPredictor()
    return _kidney


def get_liver():
    global _liver
    if _liver is None:
        _liver = LiverPredictor()
    return _liver


@tabular_bp.route("/predict/heart", methods=["POST"])
def predict_heart():
    try:
        data = request.get_json()
        result = get_heart().predict(data)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@tabular_bp.route("/predict/stroke", methods=["POST"])
def predict_stroke():
    try:
        data = request.get_json()
        result = get_stroke().predict(data)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@tabular_bp.route("/predict/diabetes", methods=["POST"])
def predict_diabetes():
    try:
        data = request.get_json()
        result = get_diabetes().predict(data)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@tabular_bp.route("/predict/lung-tabular", methods=["POST"])
def predict_lung():
    try:
        data = request.get_json()
        result = get_lung().predict(data)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@tabular_bp.route("/predict/kidney-tabular", methods=["POST"])
def predict_kidney():
    try:
        data = request.get_json()
        result = get_kidney().predict(data)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@tabular_bp.route("/predict/liver", methods=["POST"])
def predict_liver():
    try:
        data = request.get_json()
        result = get_liver().predict(data)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500
