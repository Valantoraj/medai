# MedAI - Windows Setup Guide

## Prerequisites

1. **Docker Desktop** - Download from https://www.docker.com/products/docker-desktop/
2. **Ollama** - Download from https://ollama.com/download

Install both, then restart your computer.

---

## Quick Start (Choose ONE method)

### Method 1: PowerShell (Recommended if you use PowerShell)

```powershell
cd D:\medai-new
git pull
.\run-on-windows.ps1
```

**First time only:** You may need to enable script execution:
```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### Method 2: Command Prompt / Double-click

1. Open File Explorer
2. Navigate to `D:\medai-new`
3. **Double-click `run-on-windows.bat`**

---

## What It Does

1. Checks Docker Desktop is running
2. Checks/starts Ollama if needed
3. Downloads Ollama models (llama3.2:3b, nomic-embed-text) - ~2.3 GB first time only
4. Pulls Docker images from Docker Hub (~13 GB first time, cached after)
5. Starts 3 containers: postgres, ml-service, spring-boot
6. Opens http://localhost:8090 after 35 seconds

---

## First Run

**Expect 15-30 minutes** depending on internet speed:
- Ollama models: ~2.3 GB
- Docker images: ~13 GB

**Every run after:** Starts in ~30 seconds (everything cached)

---

## Troubleshooting

### Port 8090 already in use
If you see "port is already allocated", another app is using 8090. 
Stop it or change MedAI's port in `docker-compose.hub.yml` line 55: `"8091:8080"`

### Ollama connection failed
Make sure Ollama is installed and running:
```cmd
ollama list
```
Should show your models. If not, run:
```cmd
ollama serve
```
in a separate terminal.

### Docker not running
Start Docker Desktop and wait until the whale icon stops animating in the taskbar.

### Script execution policy error (PowerShell only)
Run PowerShell as Administrator and execute:
```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

---

## Stopping MedAI

```cmd
cd D:\medai-new
docker compose -f docker-compose.hub.yml down
```

---

## System Requirements

- **RAM:** 8 GB minimum, 16 GB recommended
- **Disk:** 20 GB free space
- **CPU:** Any modern 64-bit processor (4+ cores recommended)
- **GPU:** Optional (NVIDIA speeds up image predictions significantly)
