// app.js

// Game State
let map;
let maskLayer = null;
let guessMarker = null;
let actualMarker = null;
let connectionLine = null;
let currentRoundId = null;
let currentSessionId = null;
let score = 0;
let streak = 0;
let roundActive = false;
let playerName = null;

// DOM Elements
const newRoundBtn = document.getElementById('new-round-btn');
const submitGuessBtn = document.getElementById('submit-guess-btn');
const loadingOverlay = document.getElementById('loading-overlay');
const plateTextDisplay = document.getElementById('plate-text-display');
const guessHint = document.getElementById('guess-hint');

const streakVal = document.getElementById('stats-streak').querySelector('span');
const scoreVal = document.getElementById('stats-score').querySelector('span');
const playerDisplay = document.getElementById('player-display');

// Screens & Landing Page elements
const landingPage = document.getElementById('landing-page');
const gameContent = document.getElementById('game-content');
const landingStartBtn = document.getElementById('landing-start-btn');
const landingNameInput = document.getElementById('landing-name-input');

// Panels inside Sidebar
const infoCard = document.getElementById('info-card');
const plateCard = document.getElementById('plate-card');
const guessCard = document.getElementById('guess-card');
const resultsCard = document.getElementById('results-card');
const leaderboardDisplay = document.getElementById('leaderboard-display');
const actionsCard = document.getElementById('actions-card');

const nextGuessBtn = document.getElementById('next-guess-btn');
const resStatusTitle = document.getElementById('res-status-title');
const resCity = document.getElementById('res-city');
const resDistance = document.getElementById('res-distance');
const resPoints = document.getElementById('res-points');

const CENTER_OF_GERMANY = [51.1657, 10.4515];

// Initialize Map
function initMap() {
    map = L.map('map', {
        zoomControl: true,
        maxZoom: 18,
        minZoom: 5,
        maxBounds: [[47.2, 5.8], [55.1, 15.1]],
        maxBoundsViscosity: 0.8
    }).setView(CENTER_OF_GERMANY, 6);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    // Fetch ultra-high resolution Germany state boundaries (1_sehr_hoch.geo.json) for 100% exact border precision
    fetch('https://raw.githubusercontent.com/isellsoap/deutschlandGeoJSON/main/2_bundeslaender/1_sehr_hoch.geo.json')
        .then(res => res.json())
        .then(geoJsonData => {
            const worldOuter = [
                [90, -180],
                [90, 180],
                [-90, 180],
                [-90, -180]
            ];

            let germanyRings = [];
            geoJsonData.features.forEach(feature => {
                const geom = feature.geometry;
                if (geom.type === 'Polygon') {
                    geom.coordinates.forEach(ring => {
                        germanyRings.push(ring.map(coord => [coord[1], coord[0]]));
                    });
                } else if (geom.type === 'MultiPolygon') {
                    geom.coordinates.forEach(poly => {
                        poly.forEach(ring => {
                            germanyRings.push(ring.map(coord => [coord[1], coord[0]]));
                        });
                    });
                }
            });

            maskLayer = L.polygon([worldOuter, ...germanyRings], {
                stroke: false,
                fillColor: '#080c14',
                fillOpacity: 0.35,
                className: 'map-mask-overlay',
                interactive: false
            }).addTo(map);
        })
        .catch(err => console.error('Failed to load ultra-high resolution Germany GeoJSON:', err));

    map.on('click', onMapClick);
}

// Handle Map Clicks to Drop Guess Pin
function onMapClick(e) {
    if (!roundActive) return;

    const lat = e.latlng.lat;
    const lng = e.latlng.lng;

    if (guessMarker) {
        guessMarker.setLatLng(e.latlng);
    } else {
        guessMarker = L.marker([lat, lng], { draggable: true }).addTo(map);
        guessMarker.on('dragend', () => {
            if (roundActive) submitGuessBtn.disabled = false;
        });
    }

    submitGuessBtn.disabled = false;
    guessHint.textContent = "Pin placed! Click 'Submit Guess' to evaluate.";
}

// Always show landing screen on page load (no name caching)
function checkPlayerState() {
    playerName = null;
    currentSessionId = null;
    score = 0;
    streak = 0;
    landingNameInput.value = '';
    
    landingPage.style.display = 'flex';
    gameContent.style.display = 'none';

    fetchLeaderboard();
}

// Landing Page: Set Player Name and Enter Game
function handleGameStart() {
    const inputName = landingNameInput.value.trim();
    if (!inputName) {
        alert('Please enter your name to start playing!');
        return;
    }
    
    if (inputName.length < 2 || inputName.length > 20) {
        alert('Player name must be between 2 and 20 characters.');
        return;
    }

    playerName = inputName;
    playerDisplay.textContent = playerName;
    
    currentSessionId = null;
    score = 0;
    streak = 0;
    updateStats();

    landingPage.style.display = 'none';
    gameContent.style.display = 'flex';
    
    infoCard.style.display = 'block';
    plateCard.style.display = 'flex';
    guessCard.style.display = 'block';
    actionsCard.style.display = 'block';
    
    if (map) {
        setTimeout(() => map.invalidateSize(), 50);
    }
    
    startNewRound();
}

// Start a New Game Round
async function startNewRound() {
    showLoader(true);
    
    guessCard.style.display = 'block';
    resultsCard.style.display = 'none';
    
    if (guessMarker) { map.removeLayer(guessMarker); guessMarker = null; }
    if (actualMarker) { map.removeLayer(actualMarker); actualMarker = null; }
    if (connectionLine) { map.removeLayer(connectionLine); connectionLine = null; }

    try {
        const url = currentSessionId ? `/api/game/new-round?sessionId=${currentSessionId}` : '/api/game/new-round';
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Failed to fetch new round.');
        }
        
        const data = await response.json();
        currentRoundId = data.roundId;
        currentSessionId = data.sessionId;
        plateTextDisplay.textContent = data.licensePlate;

        map.flyTo(CENTER_OF_GERMANY, 6, { duration: 1.2 });

        submitGuessBtn.disabled = true;
        guessHint.textContent = "Click on the map of Germany to place your guess pin.";
        roundActive = true;
        newRoundBtn.textContent = 'Reset Round';
        
    } catch (error) {
        console.error('Error starting round:', error);
        alert('Could not start game round. Please check if your Spring Boot backend is running.');
    } finally {
        showLoader(false);
    }
}

// Submit Map Pin Guess
async function submitGuess() {
    if (guessMarker === null || !roundActive) return;

    showLoader(true);
    roundActive = false;
    submitGuessBtn.disabled = true;

    const latlng = guessMarker.getLatLng();

    try {
        const response = await fetch('/api/game/guess', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                roundId: currentRoundId,
                latitude: latlng.lat,
                longitude: latlng.lng
            })
        });

        if (!response.ok) throw new Error('Failed to submit guess.');

        const data = await response.json();
        
        actualMarker = L.marker([data.actualLatitude, data.actualLongitude]).addTo(map);
        actualMarker.bindPopup(`<b>${data.cityName}</b> (${data.licensePlate})`).openPopup();
        guessMarker.bindPopup("Your Guess").openPopup();

        connectionLine = L.polyline([guessMarker.getLatLng(), actualMarker.getLatLng()], {
            color: '#06b6d4',
            weight: 3,
            dashArray: '8, 8',
            opacity: 0.8
        }).addTo(map);

        map.fitBounds(connectionLine.getBounds(), {
            padding: [80, 80],
            maxZoom: 12,
            animate: true,
            duration: 1.5
        });

        guessCard.style.display = 'none';
        resultsCard.style.display = 'block';

        if (data.correct) {
            resStatusTitle.textContent = "Correct!";
            resStatusTitle.style.color = "var(--success-color)";
            streak++;
        } else {
            resStatusTitle.textContent = "Too Far!";
            resStatusTitle.style.color = "var(--error-color)";
            streak = 0;
        }

        resCity.textContent = `${data.cityName} (${data.licensePlate})`;
        resDistance.textContent = `${data.distanceKm.toFixed(1)} km`;
        resPoints.textContent = `+${data.score} pts`;

        score = data.sessionScore;
        updateStats();

        await submitScoreToLeaderboard();

    } catch (error) {
        console.error('Error submitting guess:', error);
        alert('Error processing guess coordinates. Please try again.');
        roundActive = true;
        submitGuessBtn.disabled = false;
    } finally {
        showLoader(false);
    }
}

async function submitScoreToLeaderboard() {
    try {
        await fetch('/api/leaderboard', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                playerName: playerName,
                sessionId: currentSessionId
            })
        });
        fetchLeaderboard();
    } catch (error) {
        console.error('Failed to submit score to leaderboard:', error);
    }
}

async function fetchLeaderboard() {
    try {
        const response = await fetch('/api/leaderboard');
        if (!response.ok) throw new Error('Failed to fetch leaderboard');
        
        const data = await response.json();
        leaderboardDisplay.innerHTML = '';
        
        if (data.length === 0) {
            leaderboardDisplay.innerHTML = '<div style="font-size: 0.85rem; color: var(--text-secondary); text-align: center;">No high scores yet. Be the first!</div>';
            return;
        }
        
        data.forEach((entry, index) => {
            const rank = index + 1;
            let rankClass = 'rank-normal';
            if (rank === 1) rankClass = 'rank-1';
            else if (rank === 2) rankClass = 'rank-2';
            else if (rank === 3) rankClass = 'rank-3';
            
            const row = document.createElement('div');
            row.className = 'leaderboard-row';
            row.innerHTML = `
                <div class="leaderboard-player">
                    <span class="rank-badge ${rankClass}">${rank}</span>
                    <span>${entry.playerName}</span>
                </div>
                <span class="leaderboard-score">${entry.score} pts</span>
            `;
            leaderboardDisplay.appendChild(row);
        });
    } catch (error) {
        console.error('Error loading leaderboard:', error);
        leaderboardDisplay.innerHTML = '<div style="font-size: 0.85rem; color: var(--error-color); text-align: center;">Failed to load leaderboard.</div>';
    }
}

function updateStats() {
    streakVal.textContent = streak;
    scoreVal.textContent = score;
}

function showLoader(show) {
    if (show) {
        loadingOverlay.classList.add('active');
    } else {
        loadingOverlay.classList.remove('active');
    }
}

// Event Listeners
newRoundBtn.addEventListener('click', startNewRound);
nextGuessBtn.addEventListener('click', startNewRound);
submitGuessBtn.addEventListener('click', submitGuess);

// Landing page hooks
landingStartBtn.addEventListener('click', handleGameStart);
landingNameInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') handleGameStart();
});

// Initialize on DOM load
window.addEventListener('DOMContentLoaded', () => {
    initMap();
    checkPlayerState();
});