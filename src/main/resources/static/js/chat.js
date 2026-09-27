/* ============================================================
   MedAI — chat.js
   Unified chat UI for mental health, common disease, complex disease bots
   Uses SSE streaming for real-time responses
   ============================================================ */

if (!isLoggedIn()) window.location.href = '/login.html';

const BOT_CONFIG = {
  'mental-health': {
    name: 'Mental Health Counselor', icon: '🧠', endpoint: '/api/chat/mental-health',
    streamEndpoint: '/api/chat/mental-health/stream',
    welcome: 'I\'m here to listen and support you. Tell me how you\'re feeling — I\'ll ask questions one at a time to better understand your situation.',
    botType: 'MENTAL_HEALTH'
  },
  'common-disease': {
    name: 'Common Disease Diagnoser', icon: '🩺', endpoint: '/api/chat/common-disease',
    streamEndpoint: '/api/chat/common-disease/stream',
    welcome: 'Hello! Describe your main symptom and I\'ll ask targeted questions to help identify what\'s going on.',
    botType: 'COMMON_DISEASE'
  },
  'complex-disease': {
    name: 'Complex Disease Specialist', icon: '🔬', endpoint: '/api/chat/complex-disease',
    streamEndpoint: '/api/chat/complex-disease/stream',
    welcome: 'I\'m Dr. MedAI, a senior consultant. Please describe your primary concern and I\'ll conduct a thorough clinical assessment.',
    botType: 'COMPLEX_DISEASE'
  }
};

let currentBot = 'mental-health';
let currentSessionId = null;
let isStreaming = false;

// ── Init ───────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  const botParam = params.get('bot');
  if (botParam && BOT_CONFIG[botParam]) {
    selectBot(botParam);
  } else {
    selectBot('mental-health');
  }
  loadSessionList();
  setupInput();
});

// ── Bot selection ──────────────────────────────────────────
function selectBot(bot) {
  currentBot = bot;
  currentSessionId = null;

  document.querySelectorAll('.bot-item').forEach(el => {
    el.classList.toggle('active', el.dataset.bot === bot);
  });

  const cfg = BOT_CONFIG[bot];
  document.getElementById('chat-bot-icon').textContent = cfg.icon;
  document.getElementById('chat-bot-label').textContent = cfg.name;
  document.getElementById('session-badge').textContent = '';
  document.getElementById('welcome-icon').textContent = cfg.icon;
  document.getElementById('welcome-title').textContent = cfg.name;
  document.getElementById('welcome-desc').textContent = cfg.welcome;

  clearMessages();
  clearConfidencePanel();
}

function newChat() {
  currentSessionId = null;
  clearMessages();
  clearConfidencePanel();
  document.getElementById('session-badge').textContent = '';
  showToast('New chat started', 'info', 1500);
}

// ── Input setup ────────────────────────────────────────────
function setupInput() {
  const input = document.getElementById('chat-input');
  const sendBtn = document.getElementById('send-btn');

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

  input.addEventListener('input', () => {
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 140) + 'px';
  });

  sendBtn.addEventListener('click', sendMessage);
}

// ── Send message ───────────────────────────────────────────
async function sendMessage() {
  if (isStreaming) return;
  const input = document.getElementById('chat-input');
  const msg = input.value.trim();
  if (!msg) return;

  input.value = '';
  input.style.height = 'auto';
  document.getElementById('send-btn').disabled = true;

  // Hide welcome message
  const welcome = document.getElementById('welcome-msg');
  if (welcome) welcome.style.display = 'none';

  // Add user bubble
  appendBubble('user', msg);

  // Show typing indicator
  const typingId = showTyping();

  isStreaming = true;

  try {
    const body = {
      message: msg,
      sessionId: currentSessionId,
      botType: BOT_CONFIG[currentBot].botType,
      stream: false
    };

    const res = await api('POST', BOT_CONFIG[currentBot].endpoint, body);

    removeTyping(typingId);

    if (res && res.ok) {
      const data = res.data;
      currentSessionId = data.sessionId;
      document.getElementById('session-badge').textContent = `Session #${data.sessionId}`;

      appendBubble('assistant', data.message, data.thresholdReached);

      // Update confidence sidebar
      if (data.confidenceScores && data.confidenceScores.length > 0) {
        renderConfidenceScores(data.confidenceScores);
      }
      if (data.thresholdReached) {
        showThresholdCard(data.topCondition, data.topConfidence);
      }
      if (data.triggerHospitalFinder) {
        showHospitalTrigger(data.hospitalSpecialtyFilter);
      }

      loadSessionList();
    } else {
      appendBubble('assistant', '⚠️ Sorry, I encountered an error. Please try again.');
    }
  } catch (err) {
    removeTyping(typingId);
    appendBubble('assistant', '⚠️ Connection error. Please check the server is running.');
    console.error(err);
  } finally {
    isStreaming = false;
    document.getElementById('send-btn').disabled = false;
    document.getElementById('chat-input').focus();
  }
}

// ── Bubble rendering ───────────────────────────────────────
function appendBubble(role, content, isEmergency = false) {
  const container = document.getElementById('chat-messages');
  const row = document.createElement('div');
  row.className = `bubble-row ${role}`;

  const avatar = document.createElement('div');
  avatar.className = 'bubble-avatar';
  avatar.textContent = role === 'user' ? '👤' : BOT_CONFIG[currentBot].icon;

  const bubble = document.createElement('div');
  bubble.className = `bubble bubble-${role}${isEmergency ? ' emergency' : ''}`;
  bubble.innerHTML = formatMessage(content);

  const ts = document.createElement('div');
  ts.className = 'bubble-ts';
  ts.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  const inner = document.createElement('div');
  inner.style.display = 'flex';
  inner.style.flexDirection = 'column';
  inner.style.alignItems = role === 'user' ? 'flex-end' : 'flex-start';
  inner.appendChild(bubble);
  inner.appendChild(ts);

  if (role === 'user') {
    row.appendChild(inner);
    row.appendChild(avatar);
  } else {
    row.appendChild(avatar);
    row.appendChild(inner);
  }

  container.appendChild(row);
  container.scrollTop = container.scrollHeight;
  return row;
}

function showTyping() {
  const container = document.getElementById('chat-messages');
  const id = 'typing-' + Date.now();
  const row = document.createElement('div');
  row.className = 'bubble-row assistant';
  row.id = id;
  row.innerHTML = `
    <div class="bubble-avatar">${BOT_CONFIG[currentBot].icon}</div>
    <div class="bubble-typing">
      <div class="typing-indicator">
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
      </div>
    </div>`;
  container.appendChild(row);
  container.scrollTop = container.scrollHeight;
  return id;
}

function removeTyping(id) {
  const el = document.getElementById(id);
  if (el) el.remove();
}

function clearMessages() {
  const container = document.getElementById('chat-messages');
  container.innerHTML = `
    <div id="welcome-msg" style="text-align:center;padding:40px 20px;">
      <div style="font-size:48px;margin-bottom:12px;" id="welcome-icon">${BOT_CONFIG[currentBot].icon}</div>
      <h3 style="font-size:16px;margin-bottom:8px;" id="welcome-title">${BOT_CONFIG[currentBot].name}</h3>
      <p style="font-size:13px;color:var(--text-muted);max-width:320px;margin:0 auto;" id="welcome-desc">${BOT_CONFIG[currentBot].welcome}</p>
    </div>`;
}

function clearConfidencePanel() {
  const list = document.getElementById('confidence-list');
  const card = document.getElementById('threshold-card');
  const trigger = document.getElementById('hospital-trigger');
  if (list) list.innerHTML = '';
  if (card) { card.style.display = 'none'; card.innerHTML = ''; }
  if (trigger) trigger.style.display = 'none';
}

function formatMessage(text) {
  // Basic markdown-like formatting
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
    .replace(/(\d+\.) /g, '<br>$1 ');
}

// ── Session list ───────────────────────────────────────────
async function loadSessionList() {
  const res = await api('GET', '/api/chat/sessions');
  if (!res || !res.ok) return;

  const sessions = res.data.filter(s => s.botType === BOT_CONFIG[currentBot].botType).slice(0, 8);
  const el = document.getElementById('session-list');
  if (!el) return;

  el.innerHTML = sessions.map(s => `
    <div style="padding:8px 12px;border-radius:6px;cursor:pointer;font-size:11px;color:var(--text-muted);border:1px solid transparent;transition:all 0.15s;"
         onclick="loadSession(${s.id})"
         onmouseover="this.style.borderColor='var(--border)';this.style.background='var(--bg-hover)'"
         onmouseout="this.style.borderColor='transparent';this.style.background='none'">
      <div style="color:var(--text-2);font-weight:600;">#${s.id}</div>
      <div>${formatDate(s.startedAt)}</div>
    </div>`).join('');
}

async function loadSession(sessionId) {
  const res = await api('GET', `/api/chat/sessions/${sessionId}`);
  if (!res || !res.ok) return;

  currentSessionId = sessionId;
  document.getElementById('session-badge').textContent = `Session #${sessionId}`;
  clearMessages();

  const welcome = document.getElementById('welcome-msg');
  if (welcome) welcome.style.display = 'none';

  res.data.forEach(msg => {
    if (msg.role !== 'SYSTEM') {
      appendBubble(msg.role.toLowerCase(), msg.content);
    }
  });

  // Load confidence for this session
  const confRes = await api('GET', `/api/chat/confidence/${sessionId}`);
  if (confRes?.ok && confRes.data.length > 0) {
    renderConfidenceScores(confRes.data.map(c => ({
      condition: c.conditionName,
      confidence: parseFloat(c.confidencePct),
      reasoning: c.reasoning
    })));
  }
}
