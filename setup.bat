@echo off
setlocal enabledelayedexpansion

title MedAI Setup
color 0A

echo.
echo ============================================================
echo   MedAI - Automated Setup for Windows
echo ============================================================
echo.

:: ── Check admin privileges ───────────────────────────────────
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Please run this script as Administrator.
    echo Right-click setup.bat and select "Run as administrator"
    pause
    exit /b 1
)

:: ── Set working directory to script location ─────────────────
cd /d "%~dp0"
echo [INFO] Working directory: %CD%
echo.

:: ─────────────────────────────────────────────────────────────
:: STEP 1 — Check prerequisites
:: ─────────────────────────────────────────────────────────────
echo [1/7] Checking prerequisites...
echo.

:: Check Java
java -version >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Java 17 not found.
    echo Please download and install Java 17 from:
    echo https://adoptium.net
    echo After installing, re-run this script.
    pause
    exit /b 1
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    echo [OK] Java found: %%v
)

:: Check Maven
mvn -version >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Maven not found.
    echo Please download Maven from https://maven.apache.org/download.cgi
    echo Extract to C:\maven and add C:\maven\bin to your PATH.
    echo After adding to PATH, re-run this script.
    pause
    exit /b 1
)
echo [OK] Maven found.

:: Check Python
python --version >nul 2>&1
if %errorLevel% neq 0 (
    py --version >nul 2>&1
    if %errorLevel% neq 0 (
        echo [ERROR] Python not found.
        echo Please download Python 3.11+ from https://python.org
        echo IMPORTANT: Check "Add Python to PATH" during install.
        pause
        exit /b 1
    )
    set PYTHON=py
) else (
    set PYTHON=python
)
echo [OK] Python found.

:: Check Git
git --version >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Git not found.
    echo Please download Git from https://git-scm.com/download/win
    pause
    exit /b 1
)
echo [OK] Git found.

:: Check Ollama
ollama --version >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Ollama not found.
    echo Please download Ollama from https://ollama.com/download/windows
    echo After installing, re-run this script.
    pause
    exit /b 1
)
echo [OK] Ollama found.

:: Check PostgreSQL (psql)
psql --version >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] PostgreSQL (psql) not found in PATH.
    echo Please install PostgreSQL 16 from:
    echo https://www.enterprisedb.com/downloads/postgres-postgresql-downloads
    echo During install, note the password you set for the postgres user.
    echo After installing, re-run this script.
    pause
    exit /b 1
)
echo [OK] PostgreSQL found.

echo.
echo [INFO] All prerequisites satisfied.
echo.

:: ─────────────────────────────────────────────────────────────
:: STEP 2 — Set up PostgreSQL database
:: ─────────────────────────────────────────────────────────────
echo [2/7] Setting up PostgreSQL database...
echo.

:: Ask for postgres password
set /p PG_PASS=Enter your PostgreSQL postgres user password (set during install): 

:: Create user and database
echo Creating medai user and database...
set PGPASSWORD=%PG_PASS%

psql -U postgres -h localhost -c "CREATE USER medai WITH PASSWORD 'medai_password';" 2>nul
psql -U postgres -h localhost -c "CREATE DATABASE medai OWNER medai;" 2>nul
psql -U postgres -h localhost -d medai -c "CREATE EXTENSION IF NOT EXISTS vector;" 2>nul

if %errorLevel% neq 0 (
    echo [WARN] pgvector extension not available. Personalisation feature will be limited.
    echo [INFO] Switching to ddl-auto: update mode to allow table creation without pgvector.
    powershell -Command "(gc src\main\resources\application.yml) -replace 'ddl-auto: validate', 'ddl-auto: update' | sc src\main\resources\application.yml"
)

echo [OK] Database setup complete.
echo.

:: ─────────────────────────────────────────────────────────────
:: STEP 3 — Set up Python virtual environment
:: ─────────────────────────────────────────────────────────────
echo [3/7] Setting up Python virtual environment...
echo.

cd ml_service

if not exist venv (
    %PYTHON% -m venv venv
    echo [OK] Virtual environment created.
) else (
    echo [OK] Virtual environment already exists, skipping.
)

echo Installing PyTorch (CPU version)...
call venv\Scripts\activate.bat

:: Check if NVIDIA GPU available
nvidia-smi >nul 2>&1
if %errorLevel% equ 0 (
    echo [INFO] NVIDIA GPU detected. Installing CUDA-enabled PyTorch...
    pip install torch torchvision --index-url https://download.pytorch.org/whl/cu121 --quiet
) else (
    echo [INFO] No GPU detected. Installing CPU-only PyTorch...
    pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu --quiet
)

echo Installing ML service dependencies...
pip install -r requirements.txt --quiet

call venv\Scripts\deactivate.bat
cd ..

echo [OK] Python environment ready.
echo.

:: ─────────────────────────────────────────────────────────────
:: STEP 4 — Pull Ollama models
:: ─────────────────────────────────────────────────────────────
echo [4/7] Pulling Ollama models...
echo.
echo [INFO] Checking available RAM...

for /f "skip=1" %%p in ('wmic os get TotalVisibleMemorySize') do (
    set RAM_KB=%%p
    goto :ramcheck
)
:ramcheck
set /a RAM_GB=!RAM_KB! / 1048576

echo [INFO] Detected ~!RAM_GB! GB RAM.
echo.

if !RAM_GB! GEQ 20 (
    echo [INFO] Sufficient RAM for qwen3-coder ^(18GB model^).
    echo [INFO] Pulling qwen3-coder... ^(this will take a while - ~18GB download^)
    ollama pull qwen3-coder
    set OLLAMA_MODEL=qwen3-coder
) else (
    echo [INFO] Less than 20GB RAM detected. Using llama3.2:3b ^(2GB model^) instead.
    echo [INFO] Pulling llama3.2:3b...
    ollama pull llama3.2:3b
    set OLLAMA_MODEL=llama3.2:3b

    echo [INFO] Updating application.yml to use llama3.2:3b...
    powershell -Command "(gc src\main\resources\application.yml) -replace 'qwen3-coder', 'llama3.2:3b' | sc src\main\resources\application.yml"
)

echo [INFO] Pulling nomic-embed-text...
ollama pull nomic-embed-text

echo [OK] Ollama models ready.
echo.

:: ─────────────────────────────────────────────────────────────
:: STEP 5 — Build Spring Boot JAR
:: ─────────────────────────────────────────────────────────────
echo [5/7] Building Spring Boot application...
echo.

mvn clean package -DskipTests -q
if %errorLevel% neq 0 (
    echo [ERROR] Maven build failed. Check the output above for errors.
    pause
    exit /b 1
)

echo [OK] Build successful.
echo.

:: ─────────────────────────────────────────────────────────────
:: STEP 6 — Create start script
:: ─────────────────────────────────────────────────────────────
echo [6/7] Setup complete. Creating start shortcut...
echo.

echo [OK] Use start.bat to launch MedAI next time.
echo.

:: ─────────────────────────────────────────────────────────────
:: STEP 7 — Launch the application
:: ─────────────────────────────────────────────────────────────
echo [7/7] Starting MedAI...
echo.
echo [INFO] Starting ML service in a new window...
start "MedAI - ML Service" cmd /k "cd /d %CD%\ml_service && venv\Scripts\activate && python app.py"

echo [INFO] Waiting 5 seconds for ML service to initialize...
timeout /t 5 /nobreak >nul

echo [INFO] Starting Spring Boot backend in a new window...
start "MedAI - Backend" cmd /k "cd /d %CD% && mvn spring-boot:run"

echo.
echo ============================================================
echo   MedAI is starting up!
echo.
echo   Wait about 30 seconds for Spring Boot to fully start,
echo   then open your browser and go to:
echo.
echo        http://localhost:8080
echo.
echo   Two windows have opened:
echo     - "MedAI - ML Service"  (Flask on port 5001)
echo     - "MedAI - Backend"     (Spring Boot on port 8080)
echo.
echo   To start MedAI next time, just run: start.bat
echo ============================================================
echo.
pause
