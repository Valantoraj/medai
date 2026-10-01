# MedAI — AI-Powered Healthcare Platform

A full-stack medical AI application combining disease risk prediction, cancer image diagnosis, and LLM-powered chatbots — running entirely on your own machine with no external API calls.

## Features

- **6 Chatbots** — Mental health, common disease, complex disease, medicine info (powered by Ollama)
- **6 Tabular risk models** — Heart, stroke, diabetes, lung, kidney, liver (NHANES ensemble)
- **6 Cancer image models** — Brain, liver, blood, lung, kidney, skin (YOLO detection)
- **Hospital finder** — Live map using OpenStreetMap + GPS
- **Dashboard** — Prediction history, chat sessions, activity log
- **Personalisation** — Vector-search memory across sessions

---

## 🚀 Quick Start (Windows - No Build Required)

**NEW! Pre-built Docker images available on Docker Hub**

**Prerequisites (install these first):**
1. **Docker Desktop** → https://www.docker.com/products/docker-desktop/
2. **Ollama** → https://ollama.com/download

**Get MedAI:**

**With Git:**
```powershell
cd D:\
git clone https://github.com/Valantoraj/medai.git medai
cd medai
powershell -ExecutionPolicy Bypass -File .\run-on-windows.ps1
```

**Without Git (Download ZIP):**
1. Visit https://github.com/Valantoraj/medai
2. Click green **Code** button → **Download ZIP**
3. Extract to `D:\medai`
4. Open `D:\medai` and **double-click `run-on-windows.bat`**

📖 **Full Guide:** See [QUICK_START.md](QUICK_START.md) for detailed instructions and troubleshooting.

---

## Alternative Setup Methods

### Method A: One-click batch file (Windows, native)

> Best if you want native performance with your GPU.

**Prerequisites — install these first:**

| Tool | Download |
|------|----------|
| Java 17 JDK | https://adoptium.net |
| Maven | https://maven.apache.org/download.cgi → extract to `C:\maven`, add `C:\maven\bin` to PATH |
| Python 3.11+ | https://python.org → check "Add Python to PATH" |
| PostgreSQL 16 | https://www.enterprisedb.com/downloads/postgres-postgresql-downloads |
| Git | https://git-scm.com/download/win |
| Ollama | https://ollama.com/download/windows |

**Then run:**

```cmd
git clone https://github.com/Valantoraj/medai-new.git
cd medai-new
```

Right-click `setup.bat` → **Run as administrator**

That's it. `setup.bat` will:
- Check all prerequisites
- Create the database automatically
- Set up the Python virtual environment
- Detect your RAM and pull the right Ollama model automatically
- Build the Spring Boot JAR
- Start both services and open your browser

**Next time** — just double-click `start.bat`

---

### Method B: Docker (Windows, Linux, macOS)

> Best if you want a clean isolated setup with one command. Requires Docker Desktop.

**Prerequisites:**
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- Git

**Then run:**

```cmd
git clone https://github.com/Valantoraj/medai-new.git
cd medai-new
```

**Windows** — double-click `docker-setup-windows.bat`

**Linux / macOS:**
```bash
cp .env.example .env
# Edit .env — fill in DB_PASSWORD and JWT_SECRET
nano .env

docker compose up -d

# Pull Ollama models (one time)
docker exec medai-ollama-1 ollama pull llama3.2:3b
docker exec medai-ollama-1 ollama pull nomic-embed-text
```

Open http://localhost:8080

**Next time** — run `docker-start.bat` (Windows) or `docker compose up -d` (Linux/macOS)

---

### Method C: Manual setup (Linux/macOS)

```bash
git clone https://github.com/Valantoraj/medai-new.git
cd medai-new

# 1. PostgreSQL
sudo -u postgres psql -c "CREATE USER medai WITH PASSWORD 'medai_password';"
sudo -u postgres psql -c "CREATE DATABASE medai OWNER medai;"
sudo -u postgres psql -d medai -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 2. Ollama models
ollama pull llama3.2:3b        # or qwen3-coder if you have 20GB+ RAM
ollama pull nomic-embed-text

# 3. ML service
cd ml_service
python3 -m venv venv && source venv/bin/activate
pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
pip install -r requirements.txt
deactivate && cd ..

# 4. Start ML service (Terminal 1)
cd ml_service && source venv/bin/activate && python app.py

# 5. Start backend (Terminal 2)
mvn spring-boot:run
```

Open http://localhost:8080

---

## Model selection by available RAM

| RAM | Recommended model | Change in `application.yml` |
|-----|------------------|------------------------------|
| 20 GB+ | `qwen3-coder` (best quality) | default |
| 8–20 GB | `llama3.2:3b` (good quality) | replace `qwen3-coder` with `llama3.2:3b` |
| 4–8 GB | `phi4-mini` (fast, compact) | replace `qwen3-coder` with `phi4-mini` |

`setup.bat` and `docker-setup-windows.bat` detect RAM and choose automatically.

---

## Project structure

```
medai-new/
├── setup.bat                      ← Windows one-click native setup
├── start.bat                      ← Windows daily launcher (native)
├── docker-setup-windows.bat       ← Windows one-click Docker setup
├── docker-start.bat               ← Windows Docker daily launcher
├── docker-compose.yml             ← Docker Compose (all platforms)
├── Dockerfile.spring              ← Spring Boot container
├── Caddyfile                      ← HTTPS reverse proxy config
├── .env.example                   ← Environment variable template
├── src/
│   ├── main/java/com/medai/       ← Spring Boot backend
│   └── main/resources/
│       ├── application.yml        ← All configuration
│       ├── db/migration/          ← Flyway SQL migrations
│       └── static/                ← Frontend (HTML/CSS/JS)
└── ml_service/
    ├── app.py                     ← Flask entry point
    ├── routes/                    ← API routes
    ├── predictors/                ← YOLO + tabular model inference
    ├── *.pt                       ← YOLO model weights (included)
    ├── run_ml_models/*.joblib     ← Tabular model weights (included)
    ├── Dockerfile.ml
    └── requirements.txt
```

## Tech stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3, Spring Security JWT, Spring AI, WebFlux |
| Database | PostgreSQL 16 + pgvector, Flyway |
| ML service | Flask, Ultralytics YOLO, scikit-learn, LightGBM, XGBoost, CatBoost |
| LLM | Ollama — fully local, no cloud |
| Frontend | Vanilla HTML/CSS/JS, Leaflet maps, Lucide icons |
| Deployment | Docker Compose, Caddy HTTPS |

> All AI runs locally — no data ever leaves your machine.
