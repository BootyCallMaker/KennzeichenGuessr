package com.game.licenseplate.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    private UUID id;

    @Column(name = "total_score", nullable = false)
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
