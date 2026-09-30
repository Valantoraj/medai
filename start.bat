@echo off
setlocal enabledelayedexpansion

title MedAI
color 0A

cd /d "%~dp0"

echo.
echo ============================================================
echo   MedAI - Starting Services
echo ============================================================
echo.

:: Check ML service venv exists
if not exist ml_service\venv (
    echo [ERROR] Setup has not been run yet.
    echo Please run setup.bat first.
    pause
    exit /b 1
)

:: Check Ollama is running
ollama list >nul 2>&1
if %errorLevel% neq 0 (
    echo [INFO] Starting Ollama service...
    start "" ollama serve
    timeout /t 3 /nobreak >nul
)

echo [INFO] Starting ML service (Flask)...
start "MedAI - ML Service" cmd /k "cd /d %~dp0ml_service && venv\Scripts\activate && python app.py"

echo [INFO] Waiting for ML service to start...
timeout /t 5 /nobreak >nul

echo [INFO] Starting Spring Boot backend...
start "MedAI - Backend" cmd /k "cd /d %~dp0 && mvn spring-boot:run"

echo.
echo ============================================================
echo   Both services are starting.
echo   Wait ~30 seconds then open:
echo.
echo        http://localhost:8080
echo.
echo   Close the two command windows to stop MedAI.
echo ============================================================
echo.

:: Auto-open browser after 35 seconds
echo [INFO] Browser will open automatically in 35 seconds...
timeout /t 35 /nobreak >nul
start http://localhost:8080
