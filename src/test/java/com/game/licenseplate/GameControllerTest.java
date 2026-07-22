package com.game.licenseplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.licenseplate.entity.CityData;
import com.game.licenseplate.repository.CityDataRepository;
import com.game.licenseplate.repository.GameRoundRepository;
import com.game.licenseplate.repository.GameSessionRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CityDataRepository cityRepository;

    @Autowired
    private GameRoundRepository roundRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NominatimClient nominatimClient;

    @BeforeEach
    public void setup() {
        roundRepository.deleteAll();
        sessionRepository.deleteAll();
        cityRepository.deleteAll();

        cityRepository.save(new CityData("Berlin", "B", 52.5200, 13.4049));

        Mockito.when(nominatimClient.getCoordinates(Mockito.anyString()))
                .thenReturn(new NominatimClient.Coordinate(52.5200, 13.4049));
    }

    @Test
    public void testGameRoundLifecycle() throws Exception {
        // 1. Create a new game round - should return roundId, licensePlate (B), sessionId and NO coordinates
        String responseContent = mockMvc.perform(get("/api/game/new-round"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundId", notNullValue()))
                .andExpect(jsonPath("$.licensePlate", is("B")))
                .andExpect(jsonPath("$.sessionId", notNullValue()))
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse using Jackson
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String roundId = jsonNode.get("roundId").asText();
        String sessionId = jsonNode.get("sessionId").asText();

        // 2. Submit a correct pin guess (Berlin coordinates: 52.5200, 13.4049)
        String guessJson = String.format("{\"roundId\":\"%s\",\"latitude\":52.5200,\"longitude\":13.4049}", roundId);
        mockMvc.perform(post("/api/game/guess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guessJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct", is(true)))
                .andExpect(jsonPath("$.score", is(1000)))
                .andExpect(jsonPath("$.sessionScore", is(1000)))
                .andExpect(jsonPath("$.cityName", is("Berlin")))
                .andExpect(jsonPath("$.licensePlate", is("B")))
                .andExpect(jsonPath("$.actualLatitude", closeTo(52.5200, 0.01)))
                .andExpect(jsonPath("$.actualLongitude", closeTo(13.4049, 0.01)));

        // 3. Submit guess on already resolved round (should fail / error)
        mockMvc.perform(post("/api/game/guess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guessJson))
                .andExpect(status().isBadRequest());

        // 4. Play a second round reusing the same sessionId and verify score accumulation (1000 + 1000 = 2000)
        String round2Content = mockMvc.perform(get("/api/game/new-round?sessionId=" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundId", notNullValue()))
                .andExpect(jsonPath("$.sessionId", is(sessionId)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode round2Node = objectMapper.readTree(round2Content);
        String roundId2 = round2Node.get("roundId").asText();

        String guessJson2 = String.format("{\"roundId\":\"%s\",\"latitude\":52.5200,\"longitude\":13.4049}", roundId2);
        mockMvc.perform(post("/api/game/guess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guessJson2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct", is(true)))
                .andExpect(jsonPath("$.score", is(1000)))
                .andExpect(jsonPath("$.sessionScore", is(2000))); // Cumulative score 2000
    }
}
