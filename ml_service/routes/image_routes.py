"""
Image-based cancer prediction routes.
Accepts multipart image uploads from Spring Boot.
"""
import io

from flask import Blueprint, jsonify, request
from PIL import Image

from predictors.image.yolo_cancer_predictor import YoloCancerPredictor


image_bp = Blueprint("image", __name__)
_predictors = {}


def get_predictor(cancer):
    if cancer not in _predictors:
        _predictors[cancer] = YoloCancerPredictor(cancer)
    return _predictors[cancer]


def predict(cancer):
    if "image" not in request.files:
        return jsonify({"error": "No image file provided. Use multipart field 'image'."}), 400
    try:
        image = Image.open(io.BytesIO(request.files["image"].read())).convert("RGB")
        return jsonify(get_predictor(cancer).predict(image))
    except FileNotFoundError as e:
        return jsonify({"error": str(e)}), 503
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@image_bp.route("/predict/image/lung", methods=["POST"])
def predict_lung_image():
    return predict("lung")


@image_bp.route("/predict/image/liver", methods=["POST"])
def predict_liver_image():
    return predict("liver")


@image_bp.route("/predict/image/skin", methods=["POST"])
def predict_skin():
    return predict("skin")


@image_bp.route("/predict/image/blood", methods=["POST"])
def predict_blood():
    return predict("blood")


@image_bp.route("/predict/image/kidney", methods=["POST"])
def predict_kidney_image():
    return predict("kidney")


@image_bp.route("/predict/image/brain", methods=["POST"])
def predict_brain():
    return predict("brain")
