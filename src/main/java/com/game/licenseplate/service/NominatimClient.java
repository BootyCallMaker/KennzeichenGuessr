package com.game.licenseplate.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class NominatimClient {

    private final RestTemplate restTemplate;

    public NominatimClient() {
        this.restTemplate = new RestTemplate();
    }

    @SuppressWarnings("unchecked")
    public Coordinate getCoordinates(String cityName) {
        String url = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("q", cityName + ", Germany")
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        // Nominatim requires a valid and descriptive User-Agent header
        headers.set("User-Agent", "LicensePlateGame/1.0 (contact@example.com)");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<?> body = response.getBody();
            if (body != null && !body.isEmpty()) {
                Map<String, Object> firstResult = (Map<String, Object>) body.get(0);
                double lat = Double.parseDouble((String) firstResult.get("lat"));
                double lon = Double.parseDouble((String) firstResult.get("lon"));
                return new Coordinate(lat, lon);
            }
        } catch (Exception e) {
            System.err.println("Error calling Nominatim API: " + e.getMessage());
        }
        // Fallback coordinates (approximate center of Germany) to keep game functional if rate limited
        return new Coordinate(51.1657, 10.4515);
    }

    public static class Coordinate {
        private final double latitude;
        private final double longitude;

        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}
