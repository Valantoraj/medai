/* ============================================================
   MedAI — medicine.js
   Medicine information chatbot (conversational + single-ask)
   ============================================================ */

if (!isLoggedIn()) window.location.href = '/login.html';

let medicineMode = 'chat'; // 'chat' | 'single'
let medicineSessionId = null;
let isBusy = false;

document.addEventListener('DOMContentLoaded', () => {
  setupInput();
});

function setMode(mode) {
  medicineMode = mode;
  document.getElementById('mode-chat-btn').classList.toggle('active', mode === 'chat');
  document.getElementById('mode-single-btn').classList.toggle('active', mode === 'single');
  const placeholder = mode === 'chat'
    ? 'Ask about any medication… (Shift+Enter for new line)'
    : 'Ask a single question about a drug…';
  document.getElementById('chat-input').placeholder = placeholder;
}

function setupInput() {
  const input = document.getElementById('chat-input');
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
  });
  input.addEventListener('input', () => {
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 140) + 'px';
  });
  document.getElementById('send-btn').addEventListener('click', sendMessage);
}

function quickSearch(query) {
  document.getElementById('chat-input').value = query;
  sendMessage();
}

async function sendMessage() {
  if (isBusy) return;
  const input = document.getElementById('chat-input');
  const msg = input.value.trim();
  if (!msg) return;

  input.value = '';
  input.style.height = 'auto';
  document.getElementById('send-btn').disabled = true;
  isBusy = true;

  const welcome = document.querySelector('#chat-messages > div[style*="text-align:center"]');
  if (welcome) welcome.remove();

  appendBubble('user', msg);
  const typingId = showTyping();

  try {
    let answer;
    if (medicineMode === 'single') {
      const res = await api('POST', '/api/medicine/query', { question: msg });
      if (res?.ok) answer = res.data.answer;
      else answer = `${icon('alert-triangle', 14)} Could not get an answer. Please try again.`;
    } else {
      const res = await api('POST', '/api/medicine/chat', {
        message: msg,
        sessionId: medicineSessionId,
        botType: 'MEDICINE',
        stream: false
      });
      if (res?.ok) {
        answer = res.data.message;
        medicineSessionId = res.data.sessionId;
      } else {
        answer = `${icon('alert-triangle', 14)} Error contacting medicine assistant.`;
      }
    }

    removeTyping(typingId);
    appendBubble('assistant', answer);
  } catch (e) {
    removeTyping(typingId);
    appendBubble('assistant', `${icon('alert-triangle', 14)} Connection error. Is the server running?`);
  } finally {
    isBusy = false;
    document.getElementById('send-btn').disabled = false;
    input.focus();
  }
}

function appendBubble(role, content) {
  const container = document.getElementById('chat-messages');
  const row = document.createElement('div');
  row.className = `bubble-row ${role}`;

  const avatar = document.createElement('div');
  avatar.className = 'bubble-avatar';
  avatar.innerHTML = role === 'user' ? icon('user', 18) : icon('pill', 18);

  const bubble = document.createElement('div');
  bubble.className = `bubble bubble-${role}`;
  bubble.innerHTML = formatMedicineMessage(content);

  const ts = document.createElement('div');
  ts.className = 'bubble-ts';
  ts.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  const inner = document.createElement('div');
  inner.style.cssText = `display:flex;flex-direction:column;align-items:${role === 'user' ? 'flex-end' : 'flex-start'}`;
  inner.appendChild(bubble);
  inner.appendChild(ts);

  if (role === 'user') { row.appendChild(inner); row.appendChild(avatar); }
  else { row.appendChild(avatar); row.appendChild(inner); }

  container.appendChild(row);
  container.scrollTop = container.scrollHeight;
}

function showTyping() {
  const container = document.getElementById('chat-messages');
  const id = 'typing-' + Date.now();
  const row = document.createElement('div');
  row.className = 'bubble-row assistant';
  row.id = id;
  row.innerHTML = `<div class="bubble-avatar">${icon('pill', 18)}</div><div class="bubble-typing"><div class="typing-indicator"><span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span></div></div>`;
  container.appendChild(row);
  container.scrollTop = container.scrollHeight;
  return id;
}

function removeTyping(id) {
  const el = document.getElementById(id);
  if (el) el.remove();
}

function formatMedicineMessage(text) {
  return text
    .replace(/⚠️ WARNING(.*?)(\n|$)/g, `<div style="background:color-mix(in srgb,var(--warning) 12%,transparent);border-left:3px solid var(--warning);padding:6px 10px;margin:6px 0;border-radius:4px;font-size:12px;display:flex;align-items:flex-start;gap:6px;">${icon('alert-triangle', 13)} <span>WARNING$1</span></div>`)
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>');
}
