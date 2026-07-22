package com.game.licenseplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.licenseplate.entity.CityData;
import com.game.licenseplate.repository.CityDataRepository;
import com.game.licenseplate.repository.GameRoundRepository;
import com.game.licenseplate.repository.GameSessionRepository;
import com.game.licenseplate.repository.LeaderboardRepository;
import com.game.licenseplate.service.NominatimClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CityDataRepository cityRepository;

    @Autowired
    private GameRoundRepository roundRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NominatimClient nominatimClient;

    @BeforeEach
    public void setup() {
        leaderboardRepository.deleteAll();
        roundRepository.deleteAll();
        sessionRepository.deleteAll();
        cityRepository.deleteAll();
        
        cityRepository.save(new CityData("Berlin", "B", 52.5200, 13.4049));

        Mockito.when(nominatimClient.getCoordinates(Mockito.anyString()))
                .thenReturn(new NominatimClient.Coordinate(52.5200, 13.4049));
    }

    /**
     * Helper to run a game round and submit a guess.
     * Returns the sessionId generated or verified.
     */
    private String playRound(String sessionId, double lat, double lon) throws Exception {
        String url = (sessionId == null) ? "/api/game/new-round" : "/api/game/new-round?sessionId=" + sessionId;
        String response = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        String roundId = node.get("roundId").asText();
        String activeSessionId = node.get("sessionId").asText();

        String guessJson = String.format(java.util.Locale.US, "{\"roundId\":\"%s\",\"latitude\":%f,\"longitude\":%f}", roundId, lat, lon);
        mockMvc.perform(post("/api/game/guess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guessJson))
                .andExpect(status().isOk());

        return activeSessionId;
    }

    @Test
    public void testLeaderboardOperationsWithSessions() throws Exception {
        // a. A player earns a score in one session (1 exact round = 1000 pts) and submits it
        String sessionA = playRound(null, 52.5200, 13.4049);
        
        String submitJsonA = String.format("{\"playerName\":\"Player1\",\"sessionId\":\"%s\"}", sessionA);
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJsonA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerName", is("Player1")))
                .andExpect(jsonPath("$.score", is(1000)));

        // Verify leaderboard reflects that score
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].playerName", is("Player1")))
                .andExpect(jsonPath("$[0].score", is(1000)));

        // b. A second, unrelated session for Player1 with a wrong guess (score 0) does NOT drag down high score
        String sessionB = playRound(null, 0.0, 0.0);
        String submitJsonB = String.format("{\"playerName\":\"Player1\",\"sessionId\":\"%s\"}", sessionB);
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJsonB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(1000))); // Remains 1000

        // c. A third session where Player1 earns a higher total (2 exact rounds = 2000 pts) correctly raises the high score
        String sessionC = playRound(null, 52.5200, 13.4049);
        sessionC = playRound(sessionC, 52.5200, 13.4049); // Play second round in same session
        
        String submitJsonC = String.format("{\"playerName\":\"Player1\",\"sessionId\":\"%s\"}", sessionC);
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJsonC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(2000))); // Raised to 2000

        // d. A second player's score is tracked independently and ranked correctly
        String sessionD = playRound(null, 52.5200, 13.4049); // Score 1000
        String submitJsonD = String.format("{\"playerName\":\"Player2\",\"sessionId\":\"%s\"}", sessionD);
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJsonD))
                .andExpect(status().isOk());

        // Verify leaderboard size and order
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].playerName", is("Player1")))
                .andExpect(jsonPath("$[0].score", is(2000)))
                .andExpect(jsonPath("$[1].playerName", is("Player2")))
                .andExpect(jsonPath("$[1].score", is(1000)));

        // e. Submitting with an unknown/made-up sessionId is rejected with 400
        String submitJsonE = String.format("{\"playerName\":\"Player3\",\"sessionId\":\"%s\"}", UUID.randomUUID());
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJsonE))
                .andExpect(status().isBadRequest());
    }
}
