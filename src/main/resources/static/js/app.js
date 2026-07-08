// app.js

// Game State
let map;
let guessMarker = null;
let actualMarker = null;
let connectionLine = null;
let currentRoundId = null;
let score = 0;
let streak = 0;
let roundActive = false;

// DOM Elements
const newRoundBtn = document.getElementById('new-round-btn');
const submitGuessBtn = document.getElementById('submit-guess-btn');
const loadingOverlay = document.getElementById('loading-overlay');
const plateTextDisplay = document.getElementById('plate-text-display');
const guessHint = document.getElementById('guess-hint');

const streakVal = document.getElementById('streak-val');
const scoreVal = document.getElementById('score-val');

// Guess Panel & Results Panel
const guessCard = document.getElementById('guess-card');
const resultsCard = document.getElementById('results-card');
const resStatusTitle = document.getElementById('res-status-title');
const resCity = document.getElementById('res-city');
const resDistance = document.getElementById('res-distance');
const resPoints = document.getElementById('res-points');
const nextGuessBtn = document.getElementById('next-guess-btn');

const CENTER_OF_GERMANY = [51.1657, 10.4515];

// Initialize Map
function initMap() {
    map = L.map('map', {
        zoomControl: true,
        maxZoom: 18,
        minZoom: 5
    }).setView(CENTER_OF_GERMANY, 6);

    // Apply CartoDB Dark Matter tiles for a high-end dark aesthetic
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
        subdomains: 'abcd',
        maxZoom: 20
    }).addTo(map);

    // Map Click Listener to Drop Pin
    map.on('click', (e) => {
        if (!roundActive) return;

        const { lat, lng } = e.latlng;

        if (guessMarker === null) {
            // Drop a new guess pin
            guessMarker = L.marker([lat, lng], {
                draggable: true
            }).addTo(map);
            
            // Re-validate if dragged
            guessMarker.on('dragend', () => {
                submitGuessBtn.disabled = false;
            });
        } else {
            // Move existing guess pin
            guessMarker.setLatLng([lat, lng]);
        }

        submitGuessBtn.disabled = false;
        guessHint.textContent = `Pin placed at: [${lat.toFixed(4)}, ${lng.toFixed(4)}]`;
    });
}

// Start a New Game Round
async function startNewRound() {
    showLoader(true);
    
    // Switch panels back to Guess mode
    guessCard.style.display = 'block';
    resultsCard.style.display = 'none';
    
    // Clear map markers and lines
    if (guessMarker) {
        map.removeLayer(guessMarker);
        guessMarker = null;
    }
    if (actualMarker) {
        map.removeLayer(actualMarker);
        actualMarker = null;
    }
    if (connectionLine) {
        map.removeLayer(connectionLine);
        connectionLine = null;
    }

    try {
        const response = await fetch('/api/game/new-round');
        if (!response.ok) {
            throw new Error('Failed to fetch new round.');
        }
        
        const data = await response.json();
        currentRoundId = data.roundId;

        // Display the license plate prefix
        plateTextDisplay.textContent = data.licensePlate;

        // Reset view back to full Germany map
        map.flyTo(CENTER_OF_GERMANY, 6, {
            duration: 1.2
        });

        // Setup control state
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
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                roundId: currentRoundId,
                latitude: latlng.lat,
                longitude: latlng.lng
            })
        });

        if (!response.ok) {
            throw new Error('Failed to submit guess coordinates.');
        }

        const data = await response.json();
        
        // Add actual location marker
        actualMarker = L.marker([data.actualLatitude, data.actualLongitude]).addTo(map);
        actualMarker.bindPopup(`<b>${data.cityName}</b> (${data.licensePlate})`).openPopup();
        guessMarker.bindPopup("Your Guess").openPopup();

        // Connect guess marker to actual city center with a dashed polyline
        connectionLine = L.polyline([guessMarker.getLatLng(), actualMarker.getLatLng()], {
            color: '#06b6d4',
            weight: 3,
            dashArray: '8, 8',
            opacity: 0.8
        }).addTo(map);

        // Adjust map boundaries to display both pins comfortably
        map.fitBounds(connectionLine.getBounds(), {
            padding: [80, 80],
            maxZoom: 12,
            animate: true,
            duration: 1.5
        });

        // Switch panels to Results mode
        guessCard.style.display = 'none';
        resultsCard.style.display = 'block';

        // Update Results elements
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

        // Update score
        score += data.score;
        updateStats();

    } catch (error) {
        console.error('Error submitting guess:', error);
        alert('Error processing guess coordinates. Please try again.');
        roundActive = true;
        submitGuessBtn.disabled = false;
    } finally {
        showLoader(false);
    }
}

// Update UI stats indicators (Header Bar)
function updateStats() {
    streakVal.textContent = streak;
    scoreVal.textContent = score;
}

// Show/Hide loader overlay
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

// Initialize on DOM load
window.addEventListener('DOMContentLoaded', initMap);
