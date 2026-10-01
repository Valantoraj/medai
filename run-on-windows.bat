@echo off
setlocal enabledelayedexpansion
title MedAI
color 0B
cd /d "%~dp0"

echo.
echo ============================================================
echo   MedAI - Starting from Docker Hub
echo   No setup needed. Just Docker Desktop + Ollama.
echo ============================================================
echo.

:: ── Check Docker is running ───────────────────────────────────
docker info >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Docker Desktop is not running.
    echo Please start Docker Desktop, wait for it to fully load,
    echo then re-run this file.
    pause
    exit /b 1
)
echo [OK] Docker is running.

:: ── Check Ollama is running ───────────────────────────────────
ollama list >nul 2>&1
if %errorLevel% neq 0 (
    echo [INFO] Starting Ollama...
    start "" ollama serve
    timeout /t 6 /nobreak >nul
)
echo [OK] Ollama is running.
echo.

:: ── Pull Ollama models (always attempt, ollama skips if present) ─
echo [INFO] Ensuring Ollama models are available...
ollama pull llama3.2:3b
ollama pull nomic-embed-text
echo [OK] Ollama models ready.
echo.

:: ── Pull latest Docker images ─────────────────────────────────
echo [INFO] Pulling latest images from Docker Hub...
docker pull valantorajg/medai-app:latest
docker pull valantorajg/medai-ml:latest
echo.

:: ── Start containers ─────────────────────────────────────────
echo [INFO] Starting all services...
docker compose -f docker-compose.hub.yml up -d
echo.

echo ============================================================
echo   MedAI is starting up!
echo   Opening browser in 35 seconds...
echo.
echo   URL: http://localhost:8090
echo.
echo   To stop:  docker compose -f docker-compose.hub.yml down
echo   To start again: just run this file again
echo ============================================================

timeout /t 35 /nobreak >nul
start http://localhost:8090
