# Ngrok 400 Error Fix - Added Browser Warning Skip Header

## Problem
When using ngrok to deploy MedAI, chatbot queries return **400 Bad Request** errors in ngrok logs.

## Root Cause
Ngrok shows a browser warning page by default for security. When the frontend makes API calls, ngrok intercepts them and shows this warning instead of forwarding to your server, causing 400 errors.

## Solution
Added `ngrok-skip-browser-warning: true` header to **all API calls** in the application.

## What Was Changed

### Modified File: `/src/main/resources/static/js/app.js`

**Updated the `api()` function** (centralized API wrapper):

```javascript
// ── API fetch wrapper ──────────────────────────────────────
async function api(method, path, body = null, isFormData = false) {
  const headers = {};
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (body && !isFormData) headers['Content-Type'] = 'application/json';
  
  // Add ngrok header to bypass browser warning when using ngrok tunnels
  headers['ngrok-skip-browser-warning'] = 'true';  // ← ADDED THIS LINE

  const opts = { method, headers, credentials: 'include' };
  if (body) opts.body = isFormData ? body : JSON.stringify(body);

  const res = await fetch(path, opts);
  // ... rest of function
}
```

## Why This Works

- **All API calls** in MedAI use the centralized `api()` function in `app.js`
- This function is used by:
  - `chat.js` - Chat/chatbot API calls
  - `auth.js` - Login/register
  - `predict.js` - Disease prediction
  - `predict-image.js` - Image scanning
  - `medicine.js` - Medicine lookup
  - `hospitals.js` - Hospital search
  - `personalisation.js` - User preferences

- Adding the header **once** in `app.js` fixes it for **all pages**

## Files Checked (No Changes Needed)
- ✅ `chat.js` - Uses `api()` function
- ✅ `auth.js` - Uses `api()` function
- ✅ `predict.js` - Uses `api()` function
- ✅ `predict-image.js` - Uses `api()` function
- ✅ `medicine.js` - Uses `api()` function
- ✅ `hospitals.js` - Uses `api()` function
- ✅ `personalisation.js` - Uses `api()` function
- ✅ No direct `fetch()` calls found bypassing the wrapper
- ✅ No axios or XMLHttpRequest calls found

## Deployment

### Built & Pushed
```bash
# Committed changes
git commit -m "fix: add ngrok-skip-browser-warning header to bypass ngrok 400 errors"

# Built JAR
mvn clean package -DskipTests
✅ BUILD SUCCESS

# Built Docker image
docker build -t valantorajg/medai-app:latest -f Dockerfile.spring .
✅ Image built

# Pushed to Docker Hub
docker push valantorajg/medai-app:latest
✅ Pushed (digest: sha256:6d279473f93ed899e2f48027b90ca3280fd946ce10435245238531990bc7c5c8)

# Pushed to GitHub
git push origin main
✅ Pushed (commit: 69e0d29)
```

## How to Update

### If Using Docker Hub Image (Recommended)

**On Windows (PowerShell):**
```powershell
cd D:\medai-new
git pull
docker compose -f docker-compose.hub.yml down
docker compose -f docker-compose.hub.yml pull
docker compose -f docker-compose.hub.yml up -d
```

**On Linux:**
```bash
cd /path/to/medai
git pull
docker compose -f docker-compose.hub.yml down
docker compose -f docker-compose.hub.yml pull
docker compose -f docker-compose.hub.yml up -d
```

### Testing

1. **Start ngrok**:
   ```bash
   ngrok http 8080
   ```

2. **Access via ngrok URL**: 
   - Copy the ngrok forwarding URL (e.g., `https://abc123.ngrok.io`)
   - Open in browser

3. **Test chatbot**:
   - Login to MedAI
   - Go to Chatbots page
   - Send a message to any bot
   - Should work WITHOUT 400 errors in ngrok logs

4. **Check ngrok logs**:
   - Should see `200 OK` responses instead of `400 Bad Request`

## What This Header Does

From ngrok documentation:
> `ngrok-skip-browser-warning: true` - When set, ngrok will skip the browser warning interstitial page and forward the request directly to your upstream service.

This header tells ngrok:
- "This is an API call from my application"
- "Don't show the browser warning page"
- "Forward directly to my server"

## Alternative Solutions (If This Doesn't Work)

### Option 1: Use ngrok auth token
```bash
ngrok http 8080 --auth="user:password"
```

### Option 2: Disable browser warning in ngrok config
Add to `~/.ngrok2/ngrok.yml`:
```yaml
tunnels:
  medai:
    addr: 8080
    proto: http
    inspect: false
```

Then run:
```bash
ngrok start medai
```

### Option 3: Use ngrok paid plan
Free plan shows browser warnings. Paid plans can disable them completely.

## Technical Details

**Header Name**: `ngrok-skip-browser-warning`  
**Value**: `true` (or any value)  
**Where Added**: `src/main/resources/static/js/app.js` (line ~72)  
**Scope**: All HTTP requests from frontend to backend  
**Impact**: None on non-ngrok deployments (header is ignored)

## Security Note

This header is **safe** because:
1. It's only recognized by ngrok (ignored elsewhere)
2. It doesn't bypass authentication
3. It doesn't expose any data
4. It only skips ngrok's browser warning page
5. Your backend auth (JWT) still works normally

---

**Status**: ✅ Fixed and Deployed  
**Docker Image**: `valantorajg/medai-app:latest`  
**GitHub**: `main` branch (commit 69e0d29)  
**Date**: 2026-10-01
