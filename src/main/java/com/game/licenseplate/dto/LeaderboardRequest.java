package com.game.licenseplate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// Note: the score itself is intentionally NOT part of this request. It is looked up
// server-side from the session (see GameService#getSessionScore) so a client can't
// submit an arbitrary, unearned score to the leaderboard.
public record LeaderboardRequest(
    @NotBlank(message = "Player name is required")
    @Size(min = 2, max = 20, message = "Name must be between 2 and 20 characters")
    String playerName,

    @NotNull(message = "Session ID is required")
    UUID sessionId
) {}
