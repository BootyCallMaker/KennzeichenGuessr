package com.game.licenseplate.dto;

public record GuessResponse(
    boolean correct,
    double distanceKm,
    int score,
    String cityName,
    String licensePlate,
    double actualLatitude,
    double actualLongitude
) {}
