package com.game.licenseplate.service;

import com.game.licenseplate.entity.CityData;
import com.game.licenseplate.entity.GameRound;
import com.game.licenseplate.entity.GameSession;
import com.game.licenseplate.repository.CityDataRepository;
import com.game.licenseplate.repository.GameRoundRepository;
import com.game.licenseplate.repository.GameSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final CityDataRepository cityRepository;
    private final GameRoundRepository roundRepository;
    private final GameSessionRepository sessionRepository;
    private final NominatimClient nominatimClient;

    public GameService(CityDataRepository cityRepository, 
                       GameRoundRepository roundRepository, 
                       GameSessionRepository sessionRepository,
                       NominatimClient nominatimClient) {
        this.cityRepository = cityRepository;
        this.roundRepository = roundRepository;
        this.sessionRepository = sessionRepository;
        this.nominatimClient = nominatimClient;
    }

    @Transactional
    public GameRound startNewRound(UUID sessionId) {
        GameSession session = null;
        if (sessionId != null) {
            session = sessionRepository.findById(sessionId).orElse(null);
        }
        if (session == null) {
            session = new GameSession(UUID.randomUUID());
            session = sessionRepository.saveAndFlush(session);
        }

        long count = cityRepository.count();
        if (count == 0) {
            throw new IllegalStateException("Database contains no cities to guess.");
        }

        // Fetch a random city with explicit sorting to satisfy SQL offset pagination
        int randomIndex = (int) (Math.random() * count);
        Page<CityData> cityPage = cityRepository.findAll(PageRequest.of(randomIndex, 1, Sort.by("id")));
        if (cityPage.isEmpty()) {
            throw new IllegalStateException("Failed to retrieve a random city.");
        }
        CityData city = cityPage.getContent().get(0);

        double actualLat;
        double actualLon;

        // Use cached coordinates if available, otherwise fetch dynamically
        if (city.getLatitude() != null && city.getLongitude() != null) {
            actualLat = city.getLatitude();
            actualLon = city.getLongitude();
        } else {
            try {
                NominatimClient.Coordinate coordinate = nominatimClient.getCoordinates(city.getName());
                actualLat = coordinate.getLatitude();
                actualLon = coordinate.getLongitude();
                
                // Cache coordinates in DB
                city.setLatitude(actualLat);
                city.setLongitude(actualLon);
                cityRepository.save(city);
            } catch (Exception e) {
                log.warn("Failed to fetch coordinates for city '{}': {}", city.getName(), e.getMessage());
                actualLat = 51.1657;
                actualLon = 10.4515;
            }
        }

        // Create and save new round tied to the session
        GameRound round = new GameRound(city, actualLat, actualLon, session);
        return roundRepository.saveAndFlush(round);
    }

    @Transactional
    public GuessResult submitGuess(UUID roundId, double guessLat, double guessLon) {
        Optional<GameRound> roundOpt = roundRepository.findById(roundId);
        if (roundOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid round ID.");
        }

        GameRound round = roundOpt.get();
        if (!round.isActive()) {
            throw new IllegalStateException("This round has already been resolved.");
        }

        // Calculate distance using the Haversine formula
        double actualLat = round.getLatitude();
        double actualLon = round.getLongitude();
        double distanceKm = calculateDistanceInKm(guessLat, guessLon, actualLat, actualLon);

        // Deem correct if the guess is within a 50 km radius of the city center
        boolean correct = distanceKm <= 50.0;

        // GeoGuessr-style scoring: max 1000 points, penalizing 2 points per km away
        int score = Math.max(0, 1000 - (int) (distanceKm * 2));

        // Persist round state and check for concurrent submissions via optimistic lock versioning
        round.setActive(false);
        round.setScore(score);
        try {
            roundRepository.saveAndFlush(round);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("This round has already been resolved.", e);
        }

        // Persist session score and check for optimistic locking conflicts
        GameSession session = round.getSession();
        session.setTotalScore(session.getTotalScore() + score);
        try {
            sessionRepository.saveAndFlush(session);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("Score update conflict, please try again.", e);
        }

        CityData city = round.getCity();
        return new GuessResult(
                correct,
                distanceKm,
                score,
                session.getTotalScore(),
                city.getLicensePlate(),
                city.getName(),
                actualLat,
                actualLon
        );
    }

    @Transactional(readOnly = true)
    public int getSessionScore(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid session ID."))
                .getTotalScore();
    }

    /**
     * Calculates geographical distance between two points using the Haversine formula.
     */
    private double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Radius of the Earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
                
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }

    public static class GuessResult {
        private final boolean correct;
        private final double distanceKm;
        private final int score;
        private final int sessionScore;
        private final String correctPlate;
        private final String cityName;
        private final double actualLatitude;
        private final double actualLongitude;

        public GuessResult(boolean correct, double distanceKm, int score, int sessionScore, String correctPlate, String cityName, double actualLatitude, double actualLongitude) {
            this.correct = correct;
            this.distanceKm = distanceKm;
            this.score = score;
            this.sessionScore = sessionScore;
            this.correctPlate = correctPlate;
            this.cityName = cityName;
            this.actualLatitude = actualLatitude;
            this.actualLongitude = actualLongitude;
        }

        public boolean isCorrect() {
            return correct;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public int getScore() {
            return score;
        }

        public int getSessionScore() {
            return sessionScore;
        }

        public String getCorrectPlate() {
            return correctPlate;
        }

        public String getCityName() {
            return cityName;
        }

        public double getActualLatitude() {
            return actualLatitude;
        }

        public double getActualLongitude() {
            return actualLongitude;
        }
    }
}
