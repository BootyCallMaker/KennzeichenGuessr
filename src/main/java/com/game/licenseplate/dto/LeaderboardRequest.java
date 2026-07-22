package com.game.licenseplate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeaderboardRequest(
    @NotBlank(message = "Player name is required")
    @Size(min = 2, max = 20, message = "Name must be between 2 and 20 characters")
    String playerName,

    @Min(value = 0, message = "Score cannot be negative")
    int score
) {}
