# MedAI Launcher for Windows (PowerShell)
# Run this from PowerShell: .\run-on-windows.ps1

$ErrorActionPreference = "Stop"
$Host.UI.RawUI.WindowTitle = "MedAI Launcher"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  MedAI - Starting from Docker Hub" -ForegroundColor Cyan
Write-Host "  No setup needed. Just Docker Desktop + Ollama." -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ── Check Docker is running ───────────────────────────────────
Write-Host "[INFO] Checking Docker..." -ForegroundColor Yellow
try {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw }
    Write-Host "[OK] Docker is running." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Docker Desktop is not running." -ForegroundColor Red
    Write-Host "Please start Docker Desktop, wait for it to fully load, then re-run this script." -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# ── Check Ollama is running ───────────────────────────────────
Write-Host "[INFO] Checking Ollama..." -ForegroundColor Yellow
try {
    ollama list 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[INFO] Starting Ollama..." -ForegroundColor Yellow
        Start-Process "ollama" -ArgumentList "serve" -WindowStyle Hidden
        Start-Sleep -Seconds 6
    }
    Write-Host "[OK] Ollama is running." -ForegroundColor Green
} catch {
    Write-Host "[WARNING] Could not start Ollama. Make sure it's installed." -ForegroundColor Yellow
}
Write-Host ""

# ── Pull Ollama models ─────────────────────────────────────────
Write-Host "[INFO] Ensuring Ollama models are available..." -ForegroundColor Yellow

$models = ollama list 2>&1 | Out-String
if ($models -notmatch "llama3.2") {
    Write-Host "[INFO] Pulling llama3.2:3b (~2GB, one-time download)..." -ForegroundColor Yellow
    ollama pull llama3.2:3b
} else {
    Write-Host "[OK] llama3.2:3b already present." -ForegroundColor Green
}

if ($models -notmatch "nomic-embed") {
    Write-Host "[INFO] Pulling nomic-embed-text (~274MB, one-time download)..." -ForegroundColor Yellow
    ollama pull nomic-embed-text
} else {
    Write-Host "[OK] nomic-embed-text already present." -ForegroundColor Green
}

Write-Host "[OK] Ollama models ready." -ForegroundColor Green
Write-Host ""

# ── Pull latest Docker images ─────────────────────────────────
Write-Host "[INFO] Pulling latest images from Docker Hub..." -ForegroundColor Yellow
docker pull valantorajg/medai-app:latest
docker pull valantorajg/medai-ml:latest
Write-Host ""

# ── Start containers ─────────────────────────────────────────
Write-Host "[INFO] Starting all services..." -ForegroundColor Yellow
docker compose -f docker-compose.hub.yml up -d
Write-Host ""

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  MedAI is starting up!" -ForegroundColor Cyan
Write-Host "  Opening browser in 35 seconds..." -ForegroundColor Cyan
Write-Host "" -ForegroundColor Cyan
Write-Host "  URL: http://localhost:8090" -ForegroundColor Green
Write-Host "" -ForegroundColor Cyan
Write-Host "  To stop:  docker compose -f docker-compose.hub.yml down" -ForegroundColor Yellow
Write-Host "  To start again: just run this script again" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

Start-Sleep -Seconds 35
Start-Process "http://localhost:8090"
