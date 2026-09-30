FROM python:3.13-slim
WORKDIR /app

# System dependencies required by OpenCV / PIL / ultralytics
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Install CPU-only PyTorch first (keeps image ~800 MB smaller than default CUDA build)
RUN pip install --no-cache-dir \
    torch==2.4.0 \
    torchvision==0.19.0 \
    --index-url https://download.pytorch.org/whl/cpu

# Install the rest of the dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code (model files are volume-mounted at runtime — not baked in)
COPY . .

# Non-root user
RUN useradd -m mluser
USER mluser

EXPOSE 5001
ENV PYTHONUNBUFFERED=1

CMD ["python", "app.py"]
