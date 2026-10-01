FROM python:3.13
WORKDIR /app

# Install CPU-only PyTorch (latest available for Python 3.13)
RUN pip install --no-cache-dir \
    torch torchvision \
    --index-url https://download.pytorch.org/whl/cpu

# Install all other dependencies using exact versions from requirements.txt
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Force remove opencv-python if installed (ultralytics pulls it in)
# and ensure only opencv-python-headless is present (no libGL needed)
RUN pip uninstall -y opencv-python opencv-contrib-python || true && \
    pip install --no-cache-dir --force-reinstall opencv-python-headless==5.0.0.93

# Copy application code
COPY app.py .
COPY routes/ routes/
COPY predictors/ predictors/

# Copy model weights directly into the image — self-contained, no volume mounts needed
COPY lung_cancer_model.pt .
COPY blood_cancer_model.pt .
COPY kidney_cancer_cyst_stone_model.pt .
COPY skin_cancer_model.pt .
COPY runs/ runs/
COPY run_ml_models/ run_ml_models/

# Non-root user
RUN useradd -m mluser && chown -R mluser /app
USER mluser

EXPOSE 5001
ENV PYTHONUNBUFFERED=1

CMD ["python", "app.py"]
