package com.game.licenseplate.entity;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Tracks the running score for one browser play session server-side, so that
 * leaderboard submissions can be validated against a total the server itself
 * accumulated (see GameService#submitGuess / #getSessionScore) instead of
 * trusting a score value sent by the client.
 */
@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    private UUID id;

    @Column(nullable = false, name = "total_score")
    private int totalScore;

    @Version
    private Long version;

    public GameSession() {}

    public GameSession(UUID id) {
        this.id = id;
        this.totalScore = 0;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }
}
