"""
Image-based cancer diagnosis routes.
Accepts multipart file uploads from Spring Boot.
"""

from flask import Blueprint, request, jsonify
from predictors.image.lung_image_predictor import LungImagePredictor
from predictors.image.skin_predictor import SkinPredictor
from predictors.image.blood_predictor import BloodPredictor
from predictors.image.kidney_image_predictor import KidneyImagePredictor
from predictors.image.brain_predictor import BrainPredictor
import io
from PIL import Image

image_bp = Blueprint("image", __name__)

_lung_img = None
_skin = None
_blood = None
_kidney_img = None
_brain = None


def get_lung_img():
    global _lung_img
    if _lung_img is None:
        _lung_img = LungImagePredictor()
    return _lung_img


def get_skin():
    global _skin
    if _skin is None:
        _skin = SkinPredictor()
    return _skin


def get_blood():
    global _blood
    if _blood is None:
        _blood = BloodPredictor()
    return _blood


def get_kidney_img():
    global _kidney_img
    if _kidney_img is None:
        _kidney_img = KidneyImagePredictor()
    return _kidney_img


def get_brain():
    global _brain
    if _brain is None:
        _brain = BrainPredictor()
    return _brain


def read_image(request):
    if "image" not in request.files:
        raise ValueError("No image file provided. Use multipart field 'image'.")
    file = request.files["image"]
    img_bytes = file.read()
    image = Image.open(io.BytesIO(img_bytes)).convert("RGB")
    return image


@image_bp.route("/predict/image/lung", methods=["POST"])
def predict_lung_image():
    try:
        image = read_image(request)
        result = get_lung_img().predict(image)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@image_bp.route("/predict/image/skin", methods=["POST"])
def predict_skin():
    try:
        image = read_image(request)
        result = get_skin().predict(image)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@image_bp.route("/predict/image/blood", methods=["POST"])
def predict_blood():
    try:
        image = read_image(request)
        result = get_blood().predict(image)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@image_bp.route("/predict/image/kidney", methods=["POST"])
def predict_kidney_image():
    try:
        image = read_image(request)
        result = get_kidney_img().predict(image)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@image_bp.route("/predict/image/brain", methods=["POST"])
def predict_brain():
    try:
        image = read_image(request)
        result = get_brain().predict(image)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500
