package com.saferoute.sos.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sos_alerts")
public class SosAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SosStatus statut = SosStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime date;

    public SosAlert() {
        this.date = LocalDateTime.now();
    }

    public SosAlert(Long userId, Double latitude, Double longitude, String message) {
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.message = message;
        this.statut = SosStatus.ACTIVE;
        this.date = LocalDateTime.now();
    }

    public SosAlert(Long id, Long userId, Double latitude, Double longitude, String message, SosStatus statut, LocalDateTime date) {
        this.id = id;
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.message = message;
        this.statut = statut;
        this.date = date;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SosStatus getStatut() {
        return statut;
    }

    public void setStatut(SosStatus statut) {
        this.statut = statut;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
