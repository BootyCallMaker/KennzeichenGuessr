package com.game.licenseplate.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "game_rounds")
public class GameRound {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id", nullable = false)
    private CityData city;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private boolean active;

    // Constructors
    public GameRound() {}

    public GameRound(CityData city, double latitude, double longitude) {
        this.id = UUID.randomUUID();
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = true;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CityData getCity() {
        return city;
    }

    public void setCity(CityData city) {
        this.city = city;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
