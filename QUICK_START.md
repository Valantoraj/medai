# 🚀 MedAI - Quick Start for Windows

**Run a complete AI healthcare platform on your Windows PC in under 5 minutes!**

---

## ⚡ Prerequisites (Install These First)

1. **Docker Desktop** → https://www.docker.com/products/docker-desktop/  
   *(Takes 5 min to install, restart required)*

2. **Ollama** → https://ollama.com/download  
   *(Takes 2 min to install, no restart needed)*

---

## 📥 Download MedAI

**Choose ONE method:**

### Option A: With Git (Easier to update later)
1. Install Git: https://git-scm.com/download/win
2. Open PowerShell and run:
   ```powershell
   cd D:\
   git clone https://github.com/Valantoraj/medai.git medai
   ```

### Option B: Without Git (Simpler for beginners)
1. Go to: https://github.com/Valantoraj/medai
2. Click the green **Code** button
3. Click **Download ZIP**
4. Right-click the ZIP file → **Extract All** to `D:\medai`

---

## ▶️ Run MedAI

**Choose ONE method:**

### Method 1: Double-click (Easiest)
1. Open `D:\medai` in File Explorer
2. **Double-click** `run-on-windows.bat`
3. Wait ~30 seconds
4. Browser opens automatically to http://localhost:8090

### Method 2: PowerShell
```powershell
cd D:\medai
powershell -ExecutionPolicy Bypass -File .\run-on-windows.ps1
```

---

## ⏱️ First Run vs. Regular Runs

| | First Time | Every Time After |
|---|---|---|
| **Download time** | 15-30 min (~15 GB) | Instant (cached) |
| **Startup time** | 35 seconds | 35 seconds |

---

## ✅ What You Get

- ✨ **AI Chatbot** - Ask health questions
- 🩺 **11 ML Models** - Disease risk prediction
- 🧠 **Medical Imaging** - Brain tumor, liver, kidney, blood cancer detection
- 🔒 **100% Private** - Everything runs locally, no data sent anywhere
- 💾 **Cross-session memory** - Chatbot remembers your health history

---

## 🛑 Stop MedAI

Open PowerShell in `D:\medai`:
```powershell
docker compose -f docker-compose.hub.yml down
```

---

## 🔄 Update to Latest Version

### If you used Git:
```powershell
cd D:\medai
git pull
docker compose -f docker-compose.hub.yml down
powershell -ExecutionPolicy Bypass -File .\run-on-windows.ps1
```

### If you downloaded ZIP:
1. Download the latest ZIP from GitHub
2. Extract and replace old files
3. Run the script again

---

## ❓ Troubleshooting

### "Docker is not running"
→ Open Docker Desktop and wait for the whale icon to stop spinning

### "Port 8090 already in use"
→ Another app is using port 8090. Stop it or change MedAI's port in `docker-compose.hub.yml`

### "Ollama not found"
→ Make sure Ollama is installed and run `ollama serve` in a terminal

### "Execution policy" error (PowerShell)
→ Use `run-on-windows.bat` instead (just double-click it)

---

## 💻 System Requirements

- **Windows 10/11** (64-bit)
- **RAM:** 8 GB minimum, 16 GB recommended
- **Disk:** 20 GB free space
- **CPU:** Any modern 4+ core processor
- **GPU:** Optional (speeds up image analysis)

---

## 📖 Full Documentation

See **WINDOWS_SETUP.md** for detailed troubleshooting and advanced options.

---

**Need help?** Open an issue: https://github.com/Valantoraj/medai/issues
