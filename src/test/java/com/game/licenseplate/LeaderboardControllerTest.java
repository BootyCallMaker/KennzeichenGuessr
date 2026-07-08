package com.game.licenseplate;

import com.game.licenseplate.entity.LeaderboardEntry;
import com.game.licenseplate.repository.LeaderboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    private LeaderboardRepository leaderboardRepository;

    @BeforeEach
    public void setup() {
        leaderboardRepository.deleteAll();
    }

    @Test
    public void testLeaderboardOperations() throws Exception {
        // 1. Submit a score for a new player "Alex"
        String submitJson = "{\"playerName\":\"Alex\",\"score\":850}";
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerName", is("Alex")))
                .andExpect(jsonPath("$.score", is(850)));

        // 2. Submit a lower score for "Alex" (score should remain 850)
        String lowerScoreJson = "{\"playerName\":\"Alex\",\"score\":500}";
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lowerScoreJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(850))); // High score unchanged

        // 3. Submit a higher score for "Alex" (score should update to 950)
        String higherScoreJson = "{\"playerName\":\"Alex\",\"score\":950}";
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(higherScoreJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(950))); // High score updated

        // 4. Submit a score for "Bob"
        String bobScoreJson = "{\"playerName\":\"Bob\",\"score\":900}";
        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bobScoreJson))
                .andExpect(status().isOk());

        // 5. Fetch leaderboard - should return ["Alex" (950), "Bob" (900)] in descending order
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].playerName", is("Alex")))
                .andExpect(jsonPath("$[0].score", is(950)))
                .andExpect(jsonPath("$[1].playerName", is("Bob")))
                .andExpect(jsonPath("$[1].score", is(900)));
    }
}
