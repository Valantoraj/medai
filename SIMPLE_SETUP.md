# MedAI - Simple Windows Setup (No Git Required)

🏥 **AI-powered healthcare platform running 100% locally on your machine**

---

## ⚡ Quick Start (3 Steps)

### Prerequisites
1. **Docker Desktop** - [Download here](https://www.docker.com/products/docker-desktop/)
2. **Ollama** - [Download here](https://ollama.com/download/windows)

---

### Step 1: Install Prerequisites

#### Install Docker Desktop
1. Download and install Docker Desktop
2. Start Docker Desktop and wait until it shows "Engine running"

#### Install Ollama
1. Download and run the Ollama installer for Windows
2. Ollama will start automatically after installation
3. Verify it's running: Open PowerShell and type `ollama --version`

---

### Step 2: Download MedAI Files

Download these **2 files only**:
- `docker-compose.hub.yml`
- `run-on-windows.ps1`

Save them in a folder like `D:\medai-new\`

---

### Step 3: Run MedAI

1. **Right-click** on `run-on-windows.ps1`
2. Select **"Run with PowerShell"**

That's it! The script will:
- ✅ Check Docker and Ollama are running
- ✅ Download required AI models (~2.3GB, one-time)
- ✅ Pull MedAI from Docker Hub
- ✅ Start all services
- ✅ Open your browser at http://localhost:8090

---

## 🎯 What You Get

- **11 ML Models** for disease prediction and cancer detection
- **AI Chatbot** (Dr. MedAI) with cross-session memory
- **Image Analysis** for brain tumor, blood cancer, liver, pancreatic detection
- **Risk Screening** for diabetes, heart disease, kidney disease, stroke, etc.
- **100% Private** - All processing happens on your machine

---

## 🔧 Managing MedAI

### Stop All Services
```powershell
docker compose -f docker-compose.hub.yml down
```

### Start Again
Just run `run-on-windows.ps1` again (or use the command above with `up -d`)

### View Logs
```powershell
docker compose -f docker-compose.hub.yml logs -f
```

### Remove Everything (Including Data)
```powershell
docker compose -f docker-compose.hub.yml down -v
```

---

## 🐛 Troubleshooting

### "Docker is not running"
- Open Docker Desktop and wait for "Engine running" status

### "Ollama is not running"
- Open a new PowerShell window and type: `ollama serve`

### "execution policy" error
- Run this in PowerShell as Administrator:
  ```powershell
  Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
  ```
- Or always use:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\run-on-windows.ps1
  ```

### Port already in use
- MedAI uses ports 5001 and 8090
- Stop any services using these ports or change them in `docker-compose.hub.yml`

---

## 📋 System Requirements

- **OS**: Windows 10/11 (64-bit)
- **RAM**: 8GB minimum, 16GB recommended
- **Disk**: 15GB free space
- **Docker Desktop** with WSL 2 backend
- **Internet** for initial download only

---

## ⚠️ Medical Disclaimer

This application is for **informational and educational purposes only**.

- NOT a substitute for professional medical advice
- NOT for emergency medical situations
- Always consult qualified healthcare professionals
- Do not use for diagnosis or treatment decisions

---

## 📞 Support

Issues or questions? Check:
- GitHub Issues: https://github.com/Valantoraj/medai/issues
- Docker Desktop docs: https://docs.docker.com/desktop/
- Ollama docs: https://ollama.com/docs

---

**Enjoy your private AI healthcare assistant! 🚀**
