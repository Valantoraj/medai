# MedAI — AI-Powered Smart Healthcare Platform

A unified healthcare web application built on **Spring Boot 3** (Java) with a **Vanilla HTML/CSS/JS** frontend and a **Python Flask ML microservice**. All AI runs locally via **Ollama** — no cloud APIs, no data leaves your server.

---

## Features

| Feature | Model | Notes |
|---|---|---|
| Mental Health Chatbot | `medllama2` | Cross-session memory, crisis detection |
| Common Disease Diagnoser | `medllama2` | Confidence scoring, OTC suggestions |
| Complex Disease Diagnoser | `meditron` | 85% threshold, hospital finder trigger |
| Medicine Information | `qwen3.5:latest` | Conversational + single-ask modes |
| Post-Prediction Guidance | `qwen3.5:latest` | After every ML prediction |
| Personalisation Engine | `nomic-embed-text` | pgvector semantic retrieval |
| Hospital Finder | OpenStreetMap/Overpass | Live GPS, Leaflet routing |
| Heart Disease Prediction | Stacked Ensemble | Juan12Dev/heart-risk-ai-v4 |
| Stroke Prediction | Random Forest | emlacodeuse/ml-stroke-prediction |
| Diabetes Prediction | Ensemble | shabari-vignesh8/Diabetes-prediction |
| Lung Cancer (tabular) | XGBoost | nateraw/lung-cancer dataset |
| Kidney Stone | Random Forest | Euniceyeee dataset |
| Liver Disease | XGBoost | Francesco/liver-disease |
| Lung Cancer CT | Transformer | jawbra/lungevaty |
| Skin Cancer | EfficientNetV2S | Miguel764/efficientnetv2s-skin |
| Blood/Leukemia | ResNet50/YOLO | LeukemiaAttri MICCAI 2024 |
| Kidney CT | RenalCLIP (VLM) | taoyh/RenalCLIP |
| Brain Tumor MRI | ResNet50 | Abuzaid01/brain-tumor-resnet50 |

---

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.3.4, Spring AI 1.0.0-M6, Spring Security (JWT), Spring Data JPA, WebSocket/SSE
- **Database**: PostgreSQL + pgvector extension
- **Migrations**: Flyway
- **AI**: Ollama (local, port 11434)
- **ML Service**: Python 3.10+, Flask, scikit-learn, XGBoost, PyTorch, HuggingFace Transformers
- **Frontend**: HTML5 + Vanilla CSS + Vanilla JS, Leaflet.js, OpenStreetMap
- **Build**: Maven

---

## Prerequisites

1. Java 17+
2. Maven 3.8+
3. PostgreSQL 15+ with `pgvector` extension
4. Ollama installed and running (`http://localhost:11434`)
5. Python 3.10+ with pip

---

## Server Setup

### 1. PostgreSQL

```sql
CREATE DATABASE medai;
CREATE USER medai WITH PASSWORD 'medai_password';
GRANT ALL PRIVILEGES ON DATABASE medai TO medai;
\c medai
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. Ollama — pull required models

```bash
ollama pull medllama2
ollama pull meditron
# Already installed (verify):
# ollama pull qwen3.5:latest
# ollama pull llama3.1:8b
# ollama pull nomic-embed-text:latest
```

### 3. Python ML Service

```bash
cd ml_service
pip install -r requirements.txt

# Download pretrained tabular models from HuggingFace:
python train/download_models.py

# Train the 3 models that need local training (~5 min total):
python train/train_lung_tabular.py
python train/train_kidney_tabular.py
python train/train_liver.py

# Start ML service (port 5001):
python app.py
```

### 4. Spring Boot Application

```bash
# From project root:
mvn clean package -DskipTests
java -jar target/medai-1.0.0.jar

# Or with Maven:
mvn spring-boot:run
```

Open **http://localhost:8080** in your browser.

---

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/medai
    username: medai
    password: medai_password

app:
  jwt:
    secret: <your-256-bit-base64-secret>
    expiration: 86400000   # 24 hours

  ollama:
    models:
      mental-health: medllama2
      common-disease: medllama2
      complex-disease: meditron
      medicine: qwen3.5:latest
      guidance: qwen3.5:latest
      embedding: nomic-embed-text:latest
      fallback: llama3.1:8b

  ml-service:
    base-url: http://localhost:5001
```

---

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, sets JWT cookie |
| POST | `/api/chat/mental-health` | Mental health chatbot |
| POST | `/api/chat/common-disease` | Common disease diagnoser |
| POST | `/api/chat/complex-disease` | Complex disease diagnoser |
| POST | `/api/medicine/chat` | Medicine info (conversational) |
| POST | `/api/medicine/query` | Medicine info (single-ask) |
| GET | `/api/hospitals/nearby` | `?lat=X&lon=Y&radius=5000` |
| POST | `/api/predict/heart` | Heart disease risk |
| POST | `/api/predict/stroke` | Stroke risk |
| POST | `/api/predict/diabetes` | Diabetes risk |
| POST | `/api/predict/lung-tabular` | Lung cancer risk factors |
| POST | `/api/predict/kidney-tabular` | Kidney stone risk |
| POST | `/api/predict/liver` | Liver disease risk |
| POST | `/api/predict/image/lung` | Lung CT scan |
| POST | `/api/predict/image/skin` | Skin dermoscopy |
| POST | `/api/predict/image/blood` | Blood smear leukemia |
| POST | `/api/predict/image/kidney` | Kidney CT scan |
| POST | `/api/predict/image/brain` | Brain MRI tumor |
| PUT | `/api/personalisation/toggle` | Toggle personalisation on/off |
| GET | `/api/predict/history` | Past predictions |

---

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory database — no PostgreSQL needed for testing.

---

## ⚠️ Disclaimer

MedAI is a **screening and informational tool only**. It is **not a substitute for professional medical advice, diagnosis, or treatment**. Always consult a qualified healthcare professional for medical decisions.
