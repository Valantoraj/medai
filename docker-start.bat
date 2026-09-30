@echo off
title MedAI
color 0B
cd /d "%~dp0"

echo.
echo ============================================================
echo   MedAI - Starting with Docker
echo ============================================================
echo.

docker info >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Docker Desktop is not running.
    echo Please start Docker Desktop first, then re-run this.
    pause
    exit /b 1
)

docker compose up -d
echo.
echo [INFO] All services starting...
echo [INFO] Opening browser in 35 seconds...
timeout /t 35 /nobreak >nul
start http://localhost:8090
