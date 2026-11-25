package com.saferoute.sos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateSosRequest {

    @Schema(description = "Identifiant de l'utilisateur qui déclenche le SOS", example = "1")
    private Long userId;

    @NotNull(message = "La latitude est obligatoire")
    @DecimalMin(value = "-90.0", message = "La latitude doit être comprise entre -90 et 90")
    @DecimalMax(value = "90.0", message = "La latitude doit être comprise entre -90 et 90")
    @Schema(description = "Latitude GPS actuelle", example = "36.8065")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    @DecimalMin(value = "-180.0", message = "La longitude doit être comprise entre -180 et 180")
    @DecimalMax(value = "180.0", message = "La longitude doit être comprise entre -180 et 180")
    @Schema(description = "Longitude GPS actuelle", example = "10.1815")
    private Double longitude;

    @Size(max = 1000, message = "Le message ne doit pas dépasser 1000 caractères")
    @Schema(description = "Message optionnel d'alerte / détails", example = "Je me sens suivi dans la ruelle sombre")
    private String message;

    public CreateSosRequest() {
    }

    public CreateSosRequest(Long userId, Double latitude, Double longitude, String message) {
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.message = message;
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
}
