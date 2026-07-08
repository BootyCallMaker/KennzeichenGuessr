package com.game.licenseplate.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GuessRequest(
    @NotNull(message = "Round ID is required") UUID roundId,
    @NotNull(message = "Latitude is required") Double latitude,
    @NotNull(message = "Longitude is required") Double longitude
) {}
