#!/bin/bash
# ============================================================
#  MedAI — Oracle Cloud Free Tier Bootstrap Script
#  Run this ONCE on a fresh Ubuntu 22.04 ARM instance.
#  Usage:  bash server-setup.sh
# ============================================================
set -e

echo ""
echo "======================================================"
echo "  MedAI Server Bootstrap"
echo "======================================================"

# ── 1. System update ──────────────────────────────────────
echo "[1/7] Updating system packages..."
sudo apt-get update -y && sudo apt-get upgrade -y
sudo apt-get install -y curl wget git unzip ufw ca-certificates gnupg lsb-release

# ── 2. Docker ─────────────────────────────────────────────
echo "[2/7] Installing Docker..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com | sudo sh
    sudo usermod -aG docker $USER
    echo "  Docker installed. NOTE: You may need to log out and back in."
else
    echo "  Docker already installed, skipping."
fi

# Docker Compose plugin
sudo apt-get install -y docker-compose-plugin

# ── 3. Caddy (reverse proxy + automatic HTTPS) ────────────
echo "[3/7] Installing Caddy..."
if ! command -v caddy &> /dev/null; then
    sudo apt-get install -y debian-keyring debian-archive-keyring apt-transport-https
    curl -1sLf 'https://dl.cloudflare.com/release/signing.gpg' \
        | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
    curl -1sLf 'https://dl.cloudflare.com/release/linux/ubuntu/deb/caddy-stable.list' \
        | sudo tee /etc/apt/sources.list.d/caddy-stable.list
    sudo apt-get update -y
    sudo apt-get install -y caddy
else
    echo "  Caddy already installed, skipping."
fi

# ── 4. Ollama ─────────────────────────────────────────────
echo "[4/7] Installing Ollama..."
if ! command -v ollama &> /dev/null; then
    curl -fsSL https://ollama.com/install.sh | sh
else
    echo "  Ollama already installed, skipping."
fi

# ── 5. Pull Ollama models ─────────────────────────────────
echo "[5/7] Pulling Ollama models (this will take a while)..."
echo "  Pulling llama3.2:3b (~2 GB)..."
ollama pull llama3.2:3b
echo "  Pulling nomic-embed-text (~274 MB)..."
ollama pull nomic-embed-text:latest
echo "  Models ready."

# ── 6. Firewall ───────────────────────────────────────────
echo "[6/7] Configuring firewall..."
sudo ufw allow 22/tcp    comment "SSH"
sudo ufw allow 80/tcp    comment "HTTP"
sudo ufw allow 443/tcp   comment "HTTPS"
sudo ufw --force enable
echo "  Firewall enabled. Ports 22, 80, 443 open."

# ── 7. Create app directory ───────────────────────────────
echo "[7/7] Creating app directory..."
mkdir -p ~/medai
echo "  Created ~/medai"

echo ""
echo "======================================================"
echo "  Bootstrap complete!"
echo ""
echo "  Next steps:"
echo "  1. Transfer your project files to ~/medai"
echo "  2. cd ~/medai"
echo "  3. cp .env.example .env  && nano .env   (fill in secrets)"
echo "  4. docker compose build"
echo "  5. docker compose up -d"
echo "  6. Configure /etc/caddy/Caddyfile (see deploy-guide.txt)"
echo "======================================================"
