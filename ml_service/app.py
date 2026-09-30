"""
MedAI Python ML Microservice
Flask server on port 5001
Serves 6 tabular disease prediction models + 5 image cancer diagnosis models
Called only by Spring Boot backend (internal service)
"""

from flask import Flask, jsonify
from flask_cors import CORS

from routes.tabular_routes import tabular_bp
from routes.image_routes import image_bp

import os

app = Flask(__name__)

# In production ALLOWED_ORIGIN is set via environment variable to your domain.
# Locally it falls back to Spring Boot on 8080.
_allowed_origin = os.environ.get("ALLOWED_ORIGIN", "http://localhost:8080")
CORS(app, origins=[_allowed_origin])

# Register route blueprints
app.register_blueprint(tabular_bp)
app.register_blueprint(image_bp)


@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint for Spring Boot to verify ML service is running."""
    return jsonify({
        "status": "ok",
        "service": "MedAI ML Microservice",
        "version": "1.0.0"
    })


@app.errorhandler(404)
def not_found(e):
    return jsonify({"error": "Endpoint not found", "path": str(e)}), 404


@app.errorhandler(500)
def server_error(e):
    return jsonify({"error": "Internal server error", "detail": str(e)}), 500


if __name__ == "__main__":
    print("=" * 60)
    print("  MedAI ML Microservice starting on http://0.0.0.0:5001")
    print("  Health check: http://localhost:5001/health")
    print("=" * 60)
    app.run(host="0.0.0.0", port=5001, debug=False)
