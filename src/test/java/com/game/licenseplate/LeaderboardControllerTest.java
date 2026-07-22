package com.game.licenseplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.licenseplate.entity.CityData;
import com.game.licenseplate.repository.CityDataRepository;
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

    private static final String BERLIN_LAT = "52.5200";
    private static final String BERLIN_LON = "13.4049";
    // Far enough from Germany that the Haversine-based score is guaranteed to be 0.
    private static final String FAR_AWAY_LAT = "-33.8688";
    private static final String FAR_AWAY_LON = "151.2093";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private CityDataRepository cityRepository;

    @MockBean
    private NominatimClient nominatimClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        leaderboardRepository.deleteAll();
        cityRepository.deleteAll();
        cityRepository.save(new CityData("Berlin", "B", 52.5200, 13.4049));

        Mockito.when(nominatimClient.getCoordinates(Mockito.anyString()))
                .thenReturn(new NominatimClient.Coordinate(52.5200, 13.4049));
    }

    /**
     * Plays through `rounds` rounds of a single game session, guessing the exact
     * city location each time, and returns the sessionId - a stand-in for a real
     * player earning a legitimate score before submitting it to the leaderboard.
     */
    private String playSessionWithExactGuesses(int rounds) throws Exception {
        String sessionId = null;
        for (int i = 0; i < rounds; i++) {
            sessionId = startRound(sessionId);
            submitGuess(BERLIN_LAT, BERLIN_LON);
        }
        return sessionId;
    }

    private String lastRoundId;

    private String startRound(String sessionId) throws Exception {
        String url = sessionId == null ? "/api/game/new-round" : "/api/game/new-round?sessionId=" + sessionId;
        String response = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var node = objectMapper.readTree(response);
        this.lastRoundId = node.get("roundId").asText();
        return node.get("sessionId").asText();
    }

    private void submitGuess(String lat, String lon) throws Exception {
        String guessJson = String.format(
                "{\"roundId\":\"%s\",\"latitude\":%s,\"longitude\":%s}", lastRoundId, lat, lon);
        mockMvc.perform(post("/api/game/guess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guessJson))
                .andExpect(status().isOk());
    }

    private void submitToLeaderboard(String playerName, String sessionId, int expectedScore) throws Exception {
        String submitJson = String.format("{\"playerName\":\"%s\",\"sessionId\":\"%s\"}", playerName, sessionId);
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerName", is(playerName)))
                .andExpect(jsonPath("$.score", is(expectedScore)));
    }

    @Test
    public void testLeaderboardOperations() throws Exception {
        // 1. Alex earns 1000 points in one exact-guess round, submits to the leaderboard
        String alexSession1 = playSessionWithExactGuesses(1);
        submitToLeaderboard("Alex", alexSession1, 1000);

        // 2. A second, unrelated session for Alex with a wildly wrong guess (score 0)
        //    must NOT be able to drag the leaderboard entry down.
        String alexSession2 = startRound(null);
        submitGuess(FAR_AWAY_LAT, FAR_AWAY_LON);
        submitToLeaderboard("Alex", alexSession2, 1000); // unchanged

        // 3. A third session where Alex plays two exact-guess rounds (2000 total)
        //    legitimately beats the previous high score and should update it.
        String alexSession3 = playSessionWithExactGuesses(2);
        submitToLeaderboard("Alex", alexSession3, 2000);

        // 4. Bob earns 1000 points and submits his own score.
        String bobSession = playSessionWithExactGuesses(1);
        submitToLeaderboard("Bob", bobSession, 1000);

        // 5. Fetch leaderboard - should return ["Alex" (2000), "Bob" (1000)] in descending order
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].playerName", is("Alex")))
                .andExpect(jsonPath("$[0].score", is(2000)))
                .andExpect(jsonPath("$[1].playerName", is("Bob")))
                .andExpect(jsonPath("$[1].score", is(1000)));
    }

    @Test
    public void testSubmittingWithUnknownSessionIdIsRejected() throws Exception {
        String submitJson = String.format("{\"playerName\":\"Ghost\",\"sessionId\":\"%s\"}", UUID.randomUUID());
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isBadRequest());
    }
}
