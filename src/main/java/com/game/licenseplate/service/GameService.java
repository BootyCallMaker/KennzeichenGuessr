package com.game.licenseplate.service;

import com.game.licenseplate.entity.CityData;
import com.game.licenseplate.entity.GameRound;
import com.game.licenseplate.entity.GameSession;
import com.game.licenseplate.repository.CityDataRepository;
import com.game.licenseplate.repository.GameRoundRepository;
import com.game.licenseplate.repository.GameSessionRepository;
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

    private final CityDataRepository cityRepository;
    private final GameRoundRepository roundRepository;
    private final GameSessionRepository sessionRepository;
    private final NominatimClient nominatimClient;

    public GameService(CityDataRepository cityRepository, GameRoundRepository roundRepository,
                        GameSessionRepository sessionRepository, NominatimClient nominatimClient) {
        this.cityRepository = cityRepository;
        this.roundRepository = roundRepository;
        this.sessionRepository = sessionRepository;
        this.nominatimClient = nominatimClient;
    }

    /**
     * Starts a new round. If sessionId is null or unknown, a brand new session
     * (running score starting at 0) is created and used instead.
     */
    @Transactional
    public GameRound startNewRound(UUID sessionId) {
        GameSession session = (sessionId == null)
                ? null
                : sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            session = sessionRepository.save(new GameSession(UUID.randomUUID()));
        }

        long count = cityRepository.count();
        if (count == 0) {
            throw new IllegalStateException("Database contains no cities to guess.");
        }

        // Fetch a random city. An explicit sort is required: without one, the
        // database is free to return rows in any order for a given page/offset.
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
            NominatimClient.Coordinate coordinate = nominatimClient.getCoordinates(city.getName());
            actualLat = coordinate.getLatitude();
            actualLon = coordinate.getLongitude();

            // Cache coordinates in DB
            city.setLatitude(actualLat);
            city.setLongitude(actualLon);
            cityRepository.save(city);
        }

        // Create and save new round
        GameRound round = new GameRound(city, actualLat, actualLon, session);
        return roundRepository.save(round);
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

        CityData city = round.getCity();
        double actualLat = round.getLatitude();
        double actualLon = round.getLongitude();

        // Calculate distance using the Haversine formula
        double distanceKm = calculateDistanceInKm(guessLat, guessLon, actualLat, actualLon);

        // Deem correct if the guess is within a 50 km radius of the city center
        boolean correct = distanceKm <= 50.0;

        // GeoGuessr-style scoring: max 1000 points, penalizing 2 points per km away
        int score = Math.max(0, 1000 - (int) (distanceKm * 2));

        round.setActive(false);
        round.setScore(score);
        try {
            // saveAndFlush (not save) so the @Version check runs now, inside this
            // try/catch, instead of being deferred to transaction commit where it
            // could no longer be caught here.
            roundRepository.saveAndFlush(round);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Another concurrent request resolved this exact round first.
            throw new IllegalStateException("This round has already been resolved.");
        }

        GameSession session = round.getSession();
        session.setTotalScore(session.getTotalScore() + score);
        try {
            sessionRepository.saveAndFlush(session);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Two rounds in the same session were resolved at the same instant;
            // the round itself is still resolved above, caller can simply retry.
            throw new IllegalStateException("Score update conflict, please try again.");
        }

        return new GuessResult(
                correct,
                distanceKm,
                score,
                city.getLicensePlate(),
                city.getName(),
                actualLat,
                actualLon,
                session.getTotalScore()
        );
    }

    /**
     * Returns a session's server-tracked running score, for the leaderboard to submit.
     * The client can prompt this repeatedly (e.g. after every resolved round) to keep
     * the leaderboard live, but it can never submit a number it made up itself - the
     * total always comes from rounds the server itself scored (see #submitGuess).
     */
    @Transactional(readOnly = true)
    public int getSessionScore(UUID sessionId) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid session ID."));
        return session.getTotalScore();
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
        private final String correctPlate;
        private final String cityName;
        private final double actualLatitude;
        private final double actualLongitude;
        private final int sessionScore;

        public GuessResult(boolean correct, double distanceKm, int score, String correctPlate, String cityName,
                            double actualLatitude, double actualLongitude, int sessionScore) {
            this.correct = correct;
            this.distanceKm = distanceKm;
            this.score = score;
            this.correctPlate = correctPlate;
            this.cityName = cityName;
            this.actualLatitude = actualLatitude;
            this.actualLongitude = actualLongitude;
            this.sessionScore = sessionScore;
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

        public int getSessionScore() {
            return sessionScore;
        }
    }
}
