# MedAI — AI-Powered Healthcare Platform

A full-stack medical AI application combining disease risk prediction, cancer image diagnosis, and LLM-powered chatbots — running entirely on your own machine with no external API calls.

## Features

- **6 Chatbots** — Mental health, common disease, complex disease, medicine info (powered by Ollama)
- **6 Tabular risk models** — Heart, stroke, diabetes, lung, kidney, liver (NHANES YOLO ensemble)
- **6 Cancer image models** — Brain, liver, blood, lung, kidney, skin (YOLO detection)
- **Hospital finder** — Live map using OpenStreetMap + GPS
- **Dashboard** — Prediction history, chat sessions, activity log
- **Personalisation** — Vector-search memory across sessions

## Prerequisites

Install these on your machine before starting:

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17 | https://adoptium.net |
| Maven | 3.6+ | https://maven.apache.org |
| Python | 3.10+ | https://python.org |
| PostgreSQL | 14+ | https://postgresql.org |
| Ollama | latest | https://ollama.com |

## Step 1 — Get the model weights

The trained model files are not in this repository (too large for GitHub).
Download them and place them in the correct paths:

**Cancer image models** — place in `ml_service/`
```
ml_service/
├── lung_cancer_model.pt
├── blood_cancer_model.pt
├── kidney_cancer_cyst_stone_model.pt
├── skin_cancer_model.pt
├── runs/
│   ├── detect/runs/detect/brain_tumor_yolo/weights/best.pt
│   └── liver_cancer_detect_v2/liver_cancer_yolo_fold0/weights/best.pt
```

**Tabular models** — place in `ml_service/run_ml_models/`
```
ml_service/run_ml_models/
├── heart_disease_model.joblib
├── stroke_model.joblib
├── diabetes_screening_model.joblib
├── lung_disease_model.joblib
├── kidney_ckd_selfreport_model.joblib
└── liver_disease_model.joblib
```

> You can retrain the tabular models with: `python ml_service/train_ml_models.py --task all`
> You can retrain the image models by running the respective `train_*.py` scripts.

## Step 2 — Set up PostgreSQL

```bash
# Create the database and user
psql -U postgres -c "CREATE USER medai WITH PASSWORD 'medai_password';"
psql -U postgres -c "CREATE DATABASE medai OWNER medai;"
psql -U postgres -c "CREATE EXTENSION IF NOT EXISTS vector;" -d medai
```

## Step 3 — Pull Ollama models

```bash
ollama pull qwen3-coder          # main LLM (~18 GB, use llama3.2:3b for lower RAM)
ollama pull nomic-embed-text     # embeddings for personalisation
```

If you have limited RAM (< 16 GB), use a smaller model instead:
```bash
ollama pull llama3.2:3b
```
Then open `src/main/resources/application.yml` and change every `qwen3-coder` to `llama3.2:3b`.

## Step 4 — Set up the ML service

```bash
cd ml_service
python -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate
pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
pip install -r requirements.txt
```

## Step 5 — Start the ML service

```bash
cd ml_service
source venv/bin/activate
python app.py
```

The Flask service starts on `http://localhost:5001`. Leave this running.

## Step 6 — Start the Spring Boot backend

Open a new terminal:

```bash
cd /path/to/this/repo
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Open that URL in your browser.

---

## Docker (alternative to steps 4–6)

If you have Docker installed, you can run everything except Ollama with:

```bash
cp .env.example .env
# Edit .env with your values (DB password, JWT secret, etc.)
docker compose up -d
```

Then pull Ollama models separately (Ollama runs on your host, not in Docker):
```bash
ollama pull llama3.2:3b
ollama pull nomic-embed-text
```

---

## Configuration

All configuration is in `src/main/resources/application.yml`.
Sensitive values (DB password, JWT secret) can be set via environment variables — see `.env.example`.

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_PASSWORD` | `medai_password` | PostgreSQL password |
| `APP_JWT_SECRET` | (hardcoded dev key) | JWT signing secret — **change for production** |
| `SPRING_AI_OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `APP_ML_SERVICE_BASE_URL` | `http://localhost:5001` | Flask ML service URL |
| `OLLAMA_MODEL_CHAT` | `qwen3-coder` | LLM model for all chatbots |
| `ALLOWED_ORIGIN` | `http://localhost:8080` | CORS allowed origin |

---

## Project Structure

```
medai/
├── src/main/java/com/medai/       # Spring Boot backend
│   ├── controller/                # REST API endpoints
│   ├── service/                   # Business logic + Ollama integration
│   ├── model/                     # JPA entities
│   ├── repository/                # Spring Data repositories
│   ├── config/                    # Security, CORS, WebSocket config
│   └── dto/                       # Request/response DTOs
├── src/main/resources/
│   ├── application.yml            # All configuration
│   ├── db/migration/              # Flyway SQL migrations (auto-run on startup)
│   └── static/                    # Frontend (HTML + CSS + JS)
├── ml_service/
│   ├── app.py                     # Flask entry point
│   ├── routes/                    # Image and tabular prediction routes
│   ├── predictors/
│   │   ├── image/                 # YOLO cancer predictors
│   │   └── tabular/               # NHANES ensemble predictors
│   └── requirements.txt
├── Dockerfile.spring              # Docker image for Spring Boot
├── ml_service/Dockerfile.ml       # Docker image for ML service
├── docker-compose.yml             # Runs all services together
└── .env.example                   # Environment variable template
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3, Spring Security (JWT), Spring AI, WebFlux |
| Database | PostgreSQL 16, Flyway migrations, pgvector |
| ML Service | Flask, Ultralytics YOLO, scikit-learn, LightGBM, XGBoost, CatBoost |
| LLM | Ollama (local, no cloud) |
| Frontend | Vanilla HTML/CSS/JS, Leaflet maps, Lucide icons |
| Deployment | Docker Compose, Caddy (HTTPS) |

## Notes

- All AI runs locally via Ollama — no data leaves your machine
- The `⚠️ WARNING` regex in `medicine.js` is intentional — it formats LLM warning text
- Model weights are not included — download or train them separately
- For production deployment, see `docs/deploy-guide.txt`
