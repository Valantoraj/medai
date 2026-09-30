@echo off
setlocal enabledelayedexpansion

title MedAI Docker Setup
color 0B

cd /d "%~dp0"

echo.
echo ============================================================
echo   MedAI - Docker Setup for Windows
echo   Requires: Docker Desktop installed and running
echo ============================================================
echo.

:: ── Check Docker is running ───────────────────────────────────
docker info >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Docker is not running.
    echo Please start Docker Desktop and wait for it to fully load,
    echo then re-run this script.
    pause
    exit /b 1
)
echo [OK] Docker is running.
echo.

:: ── Check .env file ───────────────────────────────────────────
if not exist .env (
    echo [INFO] Creating .env from template...
    copy .env.example .env >nul

    :: Generate simple random passwords using PowerShell
    for /f %%i in ('powershell -Command "[System.Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))"') do set DB_PASS=%%i
    for /f %%i in ('powershell -Command "[System.Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))"') do set JWT_SECRET=%%i

    powershell -Command "(gc .env) -replace 'change_me_strong_password', '!DB_PASS!' | sc .env"
    powershell -Command "(gc .env) -replace 'change_me_64_char_hex_string', '!JWT_SECRET!' | sc .env"
    powershell -Command "(gc .env) -replace 'YOUR_SERVER_IP', 'localhost' | sc .env"
    powershell -Command "(gc .env) -replace 'llama3.2:3b', 'llama3.2:3b' | sc .env"

    echo [OK] .env created with auto-generated secrets.
) else (
    echo [OK] .env already exists, keeping existing values.
)
echo.

:: ── Build images ──────────────────────────────────────────────
echo [1/4] Building Docker images (first time takes 10-20 minutes)...
echo.
docker compose build
if %errorLevel% neq 0 (
    echo [ERROR] Docker build failed. Check output above.
    pause
    exit /b 1
)
echo [OK] Images built.
echo.

:: ── Start services ────────────────────────────────────────────
echo [2/4] Starting all services...
docker compose up -d
if %errorLevel% neq 0 (
    echo [ERROR] Failed to start services.
    pause
    exit /b 1
)
echo [OK] Services started.
echo.

:: ── Wait for Ollama to be ready ───────────────────────────────
echo [3/4] Waiting for Ollama to be ready (20 seconds)...
timeout /t 20 /nobreak >nul

:: ── Pull Ollama models ────────────────────────────────────────
echo [4/4] Pulling Ollama models inside container...
echo.

:: Check RAM to decide which model
for /f "skip=1" %%p in ('wmic os get TotalVisibleMemorySize') do (
    set RAM_KB=%%p
    goto :ramcheck
)
:ramcheck
set /a RAM_GB=!RAM_KB! / 1048576
echo [INFO] Detected ~!RAM_GB! GB RAM.

if !RAM_GB! GEQ 20 (
    echo [INFO] Pulling qwen3-coder (~18GB, this will take a while)...
    docker exec medai-ollama-1 ollama pull qwen3-coder
) else (
    echo [INFO] Less than 20GB RAM - pulling llama3.2:3b (~2GB)...
    docker exec medai-ollama-1 ollama pull llama3.2:3b

    echo [INFO] Updating Spring Boot config to use llama3.2:3b...
    docker exec medai-spring-boot-1 sh -c "sed -i 's/qwen3-coder/llama3.2:3b/g' /app/application.yml" 2>nul
    :: Restart spring boot to pick up the change
    docker compose restart spring-boot
)

echo [INFO] Pulling nomic-embed-text (~274MB)...
docker exec medai-ollama-1 ollama pull nomic-embed-text

echo.
echo ============================================================
echo   MedAI is ready!
echo.
echo   Open your browser and go to:
echo        http://localhost:8080
echo.
echo   Useful commands:
echo     View logs:    docker compose logs -f
echo     Stop:         docker compose down
echo     Start again:  docker compose up -d
echo ============================================================
echo.

:: Open browser
timeout /t 5 /nobreak >nul
start http://localhost:8080

pause
