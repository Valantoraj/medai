/* ============================================================
   MedAI — hospitals.js
   Leaflet map + Overpass hospital finder + OSRM routing
   ============================================================ */

let map = null;
let userMarker = null;
let hospitalMarkers = [];
let routingControl = null;
let userLat = null;
let userLon = null;
let allHospitals = [];

document.addEventListener('DOMContentLoaded', () => {
  // Check for urgency params from chatbot redirect
  const params = new URLSearchParams(window.location.search);
  const urgent = params.get('urgent');
  const specialty = params.get('specialty');
  if (urgent === 'true') {
    const banner = document.getElementById('urgency-banner');
    if (banner) {
      banner.style.display = 'flex';
      if (specialty) {
        document.getElementById('urgency-text').textContent =
          `Based on your symptoms, we recommend visiting a ${specialty} department immediately. Here are the nearest hospitals:`;
      }
    }
  }

  // Search input filter
  const searchInput = document.getElementById('hospital-search');
  if (searchInput) {
    searchInput.addEventListener('input', () => filterHospitals(searchInput.value));
  }

  getLocation();
});

// ── Geolocation ────────────────────────────────────────────
function getLocation() {
  if (!navigator.geolocation) {
    showError('Geolocation is not supported by your browser.');
    initMapFallback(12.9716, 77.5946); // Default: Bangalore
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      userLat = pos.coords.latitude;
      userLon = pos.coords.longitude;
      initMap(userLat, userLon);
      loadHospitals(userLat, userLon);
    },
    (err) => {
      console.warn('Geolocation error:', err.message);
      showToast('Could not detect location. Using default (Bangalore).', 'warning');
      initMapFallback(12.9716, 77.5946);
    },
    { timeout: 10000, maximumAge: 60000 }
  );
}

function reloadHospitals() {
  if (!userLat || !userLon) { showToast('Location not available yet.', 'warning'); return; }
  loadHospitals(userLat, userLon);
}

// ── Map init ───────────────────────────────────────────────
function initMap(lat, lon) {
  document.getElementById('map-loading').style.display = 'none';

  map = L.map('map').setView([lat, lon], 14);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    maxZoom: 19
  }).addTo(map);

  // User location marker
  const userIcon = L.divIcon({
    html: '<div style="background:#3b82f6;width:16px;height:16px;border-radius:50%;border:3px solid #fff;box-shadow:0 0 0 3px rgba(59,130,246,0.4);"></div>',
    iconSize: [16, 16],
    iconAnchor: [8, 8],
    className: ''
  });

  userMarker = L.marker([lat, lon], { icon: userIcon })
    .addTo(map)
    .bindPopup(`<strong>${icon('map-pin', 13)} Your Location</strong>`);
}

function initMapFallback(lat, lon) {
  userLat = lat;
  userLon = lon;
  initMap(lat, lon);
  loadHospitals(lat, lon);
}

// ── Load hospitals ─────────────────────────────────────────
async function loadHospitals(lat, lon) {
  const radius = parseInt(document.getElementById('radius-select')?.value || '5000');
  document.getElementById('hospital-list').innerHTML = `
    <div style="padding:20px;text-align:center;color:var(--text-muted);font-size:13px;">
      <div class="map-spinner" style="margin:0 auto 12px;"></div>
      Searching for hospitals…
    </div>`;

  const res = await api('GET', `/api/hospitals/nearby?lat=${lat}&lon=${lon}&radius=${radius}`);

  if (!res || !res.ok) {
    document.getElementById('hospital-list').innerHTML =
      '<div style="padding:16px;font-size:13px;color:var(--danger);">Failed to load hospitals. Check your connection.</div>';
    return;
  }

  allHospitals = res.data;
  renderHospitalList(allHospitals);
  renderHospitalMarkers(allHospitals);

  const countEl = document.getElementById('hospital-count');
  if (countEl) countEl.textContent = `${allHospitals.length} found`;
}

// ── Render list ────────────────────────────────────────────
function renderHospitalList(hospitals) {
  const el = document.getElementById('hospital-list');
  if (!hospitals.length) {
    el.innerHTML = '<div style="padding:16px;font-size:13px;color:var(--text-muted);">No hospitals found in this area. Try increasing the radius.</div>';
    return;
  }

  el.innerHTML = hospitals.map((h, i) => `
    <div class="hospital-card" id="hcard-${i}" onclick="selectHospital(${i})">
      <div class="hospital-name">${h.name}</div>
      <div class="hospital-meta">
        <span class="hospital-distance" style="display:inline-flex;align-items:center;gap:3px;">${icon('map-pin', 11)} ${h.distanceKm} km</span>
        ${h.emergency ? `<span class="hospital-emergency" style="display:inline-flex;align-items:center;gap:3px;">${icon('alert-octagon', 11)} Emergency</span>` : ''}
        ${h.specialty ? `<span style="font-size:10px;color:var(--medical);">${h.specialty}</span>` : ''}
      </div>
      ${h.phone ? `<div class="hospital-phone" style="display:flex;align-items:center;gap:4px;">${icon('phone', 11)} ${h.phone}</div>` : ''}
      ${h.address ? `<div style="font-size:11px;color:var(--text-muted);margin-top:3px;display:flex;align-items:center;gap:4px;">${icon('pin', 11)} ${h.address}</div>` : ''}
      <button class="hospital-navigate-btn" style="display:inline-flex;align-items:center;gap:5px;" onclick="event.stopPropagation();navigateTo(${i})">
        ${icon('navigation', 12)} Get Directions
      </button>
    </div>`).join('');
}

function filterHospitals(query) {
  const q = query.toLowerCase();
  const filtered = allHospitals.filter(h =>
    h.name.toLowerCase().includes(q) ||
    (h.address && h.address.toLowerCase().includes(q)) ||
    (h.specialty && h.specialty.toLowerCase().includes(q))
  );
  renderHospitalList(filtered);
}

// ── Map markers ────────────────────────────────────────────
function renderHospitalMarkers(hospitals) {
  // Clear old markers
  hospitalMarkers.forEach(m => m.remove());
  hospitalMarkers = [];

  hospitals.forEach((h, i) => {
    const isEmergency = h.emergency;
    const color = isEmergency ? '#f87171' : '#ef4444';

    const icon_html = L.divIcon({
      html: `<div style="background:${color};color:#fff;width:28px;height:28px;border-radius:50%;border:2px solid #fff;display:flex;align-items:center;justify-content:center;box-shadow:0 2px 8px rgba(0,0,0,0.4);cursor:pointer;"><svg xmlns='http://www.w3.org/2000/svg' width='13' height='13' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M12 6v4'/><path d='M14 14h-4'/><path d='M14 18h-4'/><path d='M14 8h-4'/><path d='M18 12h2a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2h2'/><path d='M18 22V4a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v18'/></svg></div>`,
      iconSize: [28, 28],
      iconAnchor: [14, 14],
      className: ''
    });

    const marker = L.marker([h.lat, h.lon], { icon: icon_html })
      .addTo(map)
      .bindPopup(`
        <div class="popup-hospital-name">${h.name}</div>
        ${h.address ? `<div class="popup-hospital-info" style="display:flex;align-items:center;gap:4px;"><svg xmlns='http://www.w3.org/2000/svg' width='11' height='11' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M12 17v5'/><path d='M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V7h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1z'/></svg> ${h.address}</div>` : ''}
        ${h.phone ? `<div class="popup-hospital-info" style="display:flex;align-items:center;gap:4px;"><svg xmlns='http://www.w3.org/2000/svg' width='11' height='11' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 13a19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 3.62 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z'/></svg> ${h.phone}</div>` : ''}
        <div class="popup-hospital-info" style="display:flex;align-items:center;gap:4px;"><svg xmlns='http://www.w3.org/2000/svg' width='11' height='11' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z'/><circle cx='12' cy='10' r='3'/></svg> ${h.distanceKm} km away</div>
        ${h.emergency ? `<div style="color:#f87171;font-size:11px;font-weight:700;margin-top:4px;display:flex;align-items:center;gap:4px;"><svg xmlns='http://www.w3.org/2000/svg' width='11' height='11' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M12 2 2 7l-1.27 8.77L12 22l11.27-6.23L22 7Z'/><path d='M12 8v4'/><path d='M12 16h.01'/></svg> Emergency services available</div>` : ''}
        <button class="popup-navigate" style="display:inline-flex;align-items:center;gap:5px;margin-top:6px;" onclick="navigateTo(${i})"><svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><polygon points='3 11 22 2 13 21 11 13 3 11'/></svg> Get Directions</button>
      `);

    marker.on('click', () => {
      document.querySelectorAll('.hospital-card').forEach(c => c.classList.remove('selected'));
      const card = document.getElementById(`hcard-${i}`);
      if (card) {
        card.classList.add('selected');
        card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    });

    hospitalMarkers.push(marker);
  });
}

function selectHospital(i) {
  const h = allHospitals[i];
  if (!h || !map) return;
  map.setView([h.lat, h.lon], 16, { animate: true });
  hospitalMarkers[i]?.openPopup();
  document.querySelectorAll('.hospital-card').forEach(c => c.classList.remove('selected'));
  document.getElementById(`hcard-${i}`)?.classList.add('selected');
}

// ── Routing ────────────────────────────────────────────────
function navigateTo(i) {
  const h = allHospitals[i];
  if (!h) return;

  // Draw a straight line on the map between user and hospital (no routing server needed)
  if (routingControl) {
    routingControl.remove();
    routingControl = null;
  }

  if (map && userLat && userLon) {
    routingControl = L.polyline(
      [[userLat, userLon], [h.lat, h.lon]],
      { color: '#3b82f6', weight: 4, opacity: 0.8, dashArray: '8 6' }
    ).addTo(map);
    map.fitBounds(routingControl.getBounds(), { padding: [40, 40] });
  }

  // Open turn-by-turn directions in Google Maps in a new tab
  const gmUrl = `https://www.google.com/maps/dir/?api=1` +
    `&origin=${userLat ?? ''},${userLon ?? ''}` +
    `&destination=${h.lat},${h.lon}` +
    `&travelmode=driving`;
  window.open(gmUrl, '_blank', 'noopener');

  showToast(`Opening directions to ${h.name} in Google Maps…`, 'info', 2500);
}

function showError(msg) {
  document.getElementById('map-loading').style.display = 'none';
  document.getElementById('hospital-list').innerHTML =
    `<div style="padding:16px;font-size:13px;color:var(--danger);">${msg}</div>`;
}
