package com.saferoute.sos.dto;

import com.saferoute.sos.entity.SosStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * DTO pour modification partielle (PATCH /sos/{id}).
 * Tous les champs sont optionnels : seuls les champs non nuls sont appliqués.
 */
public class PatchSosRequest {

    @DecimalMin(value = "-90.0", message = "La latitude doit être comprise entre -90 et 90")
    @DecimalMax(value = "90.0", message = "La latitude doit être comprise entre -90 et 90")
    @Schema(description = "Nouvelle latitude GPS (optionnelle)", example = "36.8090")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "La longitude doit être comprise entre -180 et 180")
    @DecimalMax(value = "180.0", message = "La longitude doit être comprise entre -180 et 180")
    @Schema(description = "Nouvelle longitude GPS (optionnelle)", example = "10.1840")
    private Double longitude;

    @Size(max = 1000, message = "Le message ne doit pas dépasser 1000 caractères")
    @Schema(description = "Message mis à jour (optionnel)", example = "Les secours sont en approche")
    private String message;

    @Schema(description = "Nouveau statut (optionnel)", example = "EN_COURS")
    private SosStatus statut;

    public PatchSosRequest() {
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
}
