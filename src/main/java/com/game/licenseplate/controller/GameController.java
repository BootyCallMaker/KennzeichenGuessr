package com.game.licenseplate.controller;

import com.game.licenseplate.dto.GameRoundResponse;
import com.game.licenseplate.dto.GuessRequest;
import com.game.licenseplate.dto.GuessResponse;
import com.game.licenseplate.entity.GameRound;
import com.game.licenseplate.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/new-round")
    public ResponseEntity<GameRoundResponse> newRound(@RequestParam(required = false) UUID sessionId) {
        GameRound round = gameService.startNewRound(sessionId);
        // Hide coordinates from user to prevent cheating
        return ResponseEntity.ok(new GameRoundResponse(
                round.getId(),
                round.getCity().getLicensePlate(),
                round.getSession().getId()
        ));
    }

    @PostMapping("/guess")
    public ResponseEntity<GuessResponse> guess(@Valid @RequestBody GuessRequest request) {
        GameService.GuessResult result = gameService.submitGuess(
                request.roundId(),
                request.latitude(),
                request.longitude()
        );
        return ResponseEntity.ok(new GuessResponse(
                result.isCorrect(),
                result.getDistanceKm(),
                result.getScore(),
                result.getCityName(),
                result.getCorrectPlate(),
                result.getActualLatitude(),
                result.getActualLongitude(),
                result.getSessionScore()
        ));
    }
}
