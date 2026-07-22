package com.game.licenseplate.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NominatimClientWireMockTest {

    private static WireMockServer wireMockServer;
    private NominatimClient nominatimClient;

    @BeforeAll
    public static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    public static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    public void setupClient() {
        wireMockServer.resetAll();
        nominatimClient = new NominatimClient(new RestTemplateBuilder(), wireMockServer.baseUrl() + "/search");
    }

    @Test
    @Order(1)
    public void testGetCoordinates_SuccessfulResponse() {
        // Mock a 200 OK HTTP response from Nominatim for Munich matching query param substring
        wireMockServer.stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", containing("Munich"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"lat\": \"48.1351\", \"lon\": \"11.5820\"}]")));

        NominatimClient.Coordinate coord = nominatimClient.getCoordinates("Munich");

        assertEquals(48.1351, coord.getLatitude(), 0.0001);
        assertEquals(11.5820, coord.getLongitude(), 0.0001);
    }

    @Test
    @Order(2)
    public void testGetCoordinates_ServerErrorFallback() {
        // Mock a 500 Server Error response from Nominatim
        wireMockServer.stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", containing("UnknownCity"))
                .willReturn(aResponse().withStatus(500)));

        NominatimClient.Coordinate coord = nominatimClient.getCoordinates("UnknownCity");

        // Verify it returns default fallback coordinates (51.1657, 10.4515)
        assertEquals(51.1657, coord.getLatitude(), 0.0001);
        assertEquals(10.4515, coord.getLongitude(), 0.0001);
    }

    @Test
    @Order(3)
    public void testGetCoordinates_TimeoutFallback() {
        // Mock a delayed HTTP response (> 5s read timeout)
        wireMockServer.stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", containing("TimeoutCity"))
                .willReturn(aResponse()
                        .withFixedDelay(6000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"lat\": \"48.1351\", \"lon\": \"11.5820\"}]")));

        NominatimClient.Coordinate coord = nominatimClient.getCoordinates("TimeoutCity");

        // Verify timeout triggers catch block and returns fallback coordinates
        assertEquals(51.1657, coord.getLatitude(), 0.0001);
        assertEquals(10.4515, coord.getLongitude(), 0.0001);
    }
}
