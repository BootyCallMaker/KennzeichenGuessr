package com.game.licenseplate.controller;

import com.game.licenseplate.dto.LeaderboardRequest;
import com.game.licenseplate.entity.LeaderboardEntry;
import com.game.licenseplate.service.LeaderboardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @PostMapping
    public ResponseEntity<LeaderboardEntry> submitScore(@Valid @RequestBody LeaderboardRequest request) {
        LeaderboardEntry entry = leaderboardService.submitScore(request.playerName(), request.score());
        return ResponseEntity.ok(entry);
    }

    @GetMapping
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        List<LeaderboardEntry> top10 = leaderboardService.getTop10();
        return ResponseEntity.ok(top10);
    }
}
