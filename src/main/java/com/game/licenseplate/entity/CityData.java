package com.game.licenseplate.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "city_data")
public class CityData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, name = "license_plate")
    private String licensePlate;

    private Double latitude;
    private Double longitude;

    // Constructors
    public CityData() {}

    public CityData(String name, String licensePlate) {
        this.name = name;
        this.licensePlate = licensePlate;
    }

    public CityData(String name, String licensePlate, Double latitude, Double longitude) {
        this.name = name;
        this.licensePlate = licensePlate;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
