package com.game.licenseplate.service;

import com.game.licenseplate.entity.CityData;
import com.game.licenseplate.entity.GameRound;
import com.game.licenseplate.repository.CityDataRepository;
import com.game.licenseplate.repository.GameRoundRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class GameService {

    private final CityDataRepository cityRepository;
    private final GameRoundRepository roundRepository;
    private final NominatimClient nominatimClient;

    public GameService(CityDataRepository cityRepository, GameRoundRepository roundRepository, NominatimClient nominatimClient) {
        this.cityRepository = cityRepository;
        this.roundRepository = roundRepository;
        this.nominatimClient = nominatimClient;
    }

    @Transactional
    public GameRound startNewRound() {
        long count = cityRepository.count();
        if (count == 0) {
            throw new IllegalStateException("Database contains no cities to guess.");
        }

        // Fetch a random city
        int randomIndex = (int) (Math.random() * count);
        Page<CityData> cityPage = cityRepository.findAll(PageRequest.of(randomIndex, 1));
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
        GameRound round = new GameRound(city, actualLat, actualLon);
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

        // Deactivate the round after guessing
        round.setActive(false);
        roundRepository.save(round);

        CityData city = round.getCity();
        double actualLat = round.getLatitude();
        double actualLon = round.getLongitude();

        // Calculate distance using the Haversine formula
        double distanceKm = calculateDistanceInKm(guessLat, guessLon, actualLat, actualLon);

        // Deem correct if the guess is within a 50 km radius of the city center
        boolean correct = distanceKm <= 50.0;

        // GeoGuessr-style scoring: max 1000 points, penalizing 2 points per km away
        int score = Math.max(0, 1000 - (int) (distanceKm * 2));

        return new GuessResult(
                correct,
                distanceKm,
                score,
                city.getLicensePlate(),
                city.getName(),
                actualLat,
                actualLon
        );
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

        public GuessResult(boolean correct, double distanceKm, int score, String correctPlate, String cityName, double actualLatitude, double actualLongitude) {
            this.correct = correct;
            this.distanceKm = distanceKm;
            this.score = score;
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
