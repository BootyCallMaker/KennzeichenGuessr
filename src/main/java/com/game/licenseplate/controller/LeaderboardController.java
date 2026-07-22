package com.game.licenseplate.controller;

import com.game.licenseplate.dto.LeaderboardRequest;
import com.game.licenseplate.entity.LeaderboardEntry;
import com.game.licenseplate.service.GameService;
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
    private final GameService gameService;

    public LeaderboardController(LeaderboardService leaderboardService, GameService gameService) {
        this.leaderboardService = leaderboardService;
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<LeaderboardEntry> submitScore(@Valid @RequestBody LeaderboardRequest request) {
        // The score is never taken from the client directly - it's read from the
        // server-tracked session total, so a submission can't exceed what was
        // actually earned in-game (see GameService#getSessionScore).
        int score = gameService.getSessionScore(request.sessionId());
        LeaderboardEntry entry = leaderboardService.submitScore(request.playerName(), score);
        return ResponseEntity.ok(entry);
    }

    @GetMapping
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        List<LeaderboardEntry> top10 = leaderboardService.getTop10();
        return ResponseEntity.ok(top10);
    }
}
