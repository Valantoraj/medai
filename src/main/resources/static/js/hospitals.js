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
    .bindPopup('<strong>📍 Your Location</strong>');
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
        <span class="hospital-distance">📍 ${h.distanceKm} km</span>
        ${h.emergency ? '<span class="hospital-emergency">🚨 Emergency</span>' : ''}
        ${h.specialty ? `<span style="font-size:10px;color:var(--medical);">${h.specialty}</span>` : ''}
      </div>
      ${h.phone ? `<div class="hospital-phone">📞 ${h.phone}</div>` : ''}
      ${h.address ? `<div style="font-size:11px;color:var(--text-muted);margin-top:3px;">📌 ${h.address}</div>` : ''}
      <button class="hospital-navigate-btn" onclick="event.stopPropagation();navigateTo(${i})">
        🗺️ Get Directions
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

    const icon = L.divIcon({
      html: `<div style="background:${color};color:#fff;width:28px;height:28px;border-radius:50%;border:2px solid #fff;display:flex;align-items:center;justify-content:center;font-size:13px;box-shadow:0 2px 8px rgba(0,0,0,0.4);cursor:pointer;">🏥</div>`,
      iconSize: [28, 28],
      iconAnchor: [14, 14],
      className: ''
    });

    const marker = L.marker([h.lat, h.lon], { icon })
      .addTo(map)
      .bindPopup(`
        <div class="popup-hospital-name">${h.name}</div>
        ${h.address ? `<div class="popup-hospital-info">📌 ${h.address}</div>` : ''}
        ${h.phone ? `<div class="popup-hospital-info">📞 ${h.phone}</div>` : ''}
        <div class="popup-hospital-info">📍 ${h.distanceKm} km away</div>
        ${h.emergency ? '<div style="color:#f87171;font-size:11px;font-weight:700;margin-top:4px;">🚨 Emergency services available</div>' : ''}
        <button class="popup-navigate" onclick="navigateTo(${i})">🗺️ Get Directions</button>
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
  if (!h || !userLat || !userLon || !map) return;

  // Remove existing route
  if (routingControl) {
    routingControl.remove();
    routingControl = null;
  }

  routingControl = L.Routing.control({
    waypoints: [
      L.latLng(userLat, userLon),
      L.latLng(h.lat, h.lon)
    ],
    routeWhileDragging: false,
    addWaypoints: false,
    fitSelectedRoutes: true,
    lineOptions: {
      styles: [{ color: '#3b82f6', weight: 5, opacity: 0.8 }]
    },
    createMarker: () => null, // suppress default markers
    router: L.Routing.osrmv1({ serviceUrl: 'https://router.project-osrm.org/route/v1' })
  }).addTo(map);

  showToast(`Getting directions to ${h.name}…`, 'info', 2000);
}

function showError(msg) {
  document.getElementById('map-loading').style.display = 'none';
  document.getElementById('hospital-list').innerHTML =
    `<div style="padding:16px;font-size:13px;color:var(--danger);">${msg}</div>`;
}
