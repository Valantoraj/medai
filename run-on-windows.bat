@echo off
title MedAI
color 0B
cd /d "%~dp0"

echo.
echo ============================================================
echo   MedAI - Starting from Docker Hub
echo   No setup needed. Just Docker Desktop.
echo ============================================================
echo.

docker info >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Docker Desktop is not running.
    echo Please start Docker Desktop and wait for it to fully load,
    echo then re-run this file.
    pause
    exit /b 1
)

echo [INFO] Pulling latest images from Docker Hub...
docker pull valantorajg/medai-app:latest
docker pull valantorajg/medai-ml:latest

echo.
echo [INFO] Starting all services...
docker compose -f docker-compose.hub.yml up -d

echo.
echo [INFO] Checking if Ollama models are downloaded...
docker exec medai-ollama-1 ollama list 2>nul | findstr "llama3.2" >nul
if %errorLevel% neq 0 (
    echo [INFO] Pulling llama3.2:3b model (~2GB, one-time download)...
    docker exec medai-ollama-1 ollama pull llama3.2:3b
    echo [INFO] Pulling nomic-embed-text (~274MB)...
    docker exec medai-ollama-1 ollama pull nomic-embed-text
) else (
    echo [OK] Ollama models already present.
)

echo.
echo ============================================================
echo   MedAI is starting up!
echo   Opening browser in 30 seconds...
echo.
echo   To stop:  docker compose -f docker-compose.hub.yml down
echo   To start again: just run this file again
echo ============================================================

timeout /t 30 /nobreak >nul
start http://localhost:8080
