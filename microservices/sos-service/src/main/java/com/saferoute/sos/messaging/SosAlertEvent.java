package com.saferoute.sos.messaging;

import com.saferoute.sos.client.dto.UserEmergencyContactDto;
import com.saferoute.sos.entity.SosStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Événement métier publié sur RabbitMQ (Communication Asynchrone - S7)
 * consommé par le service de Notification et le tableau de bord temps réel.
 */
public class SosAlertEvent implements Serializable {

    private String eventType;
    private Long alertId;
    private Long userId;
    private Double latitude;
    private Double longitude;
    private String message;
    private SosStatus statut;
    private List<UserEmergencyContactDto> emergencyContacts;
    private Instant occurredAt;

    public SosAlertEvent() {
    }

    public SosAlertEvent(
            String eventType,
            Long alertId,
            Long userId,
            Double latitude,
            Double longitude,
            String message,
            SosStatus statut,
            List<UserEmergencyContactDto> emergencyContacts,
            Instant occurredAt) {
        this.eventType = eventType;
        this.alertId = alertId;
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.message = message;
        this.statut = statut;
        this.emergencyContacts = emergencyContacts;
        this.occurredAt = occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
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

    public List<UserEmergencyContactDto> getEmergencyContacts() {
        return emergencyContacts;
    }

    public void setEmergencyContacts(List<UserEmergencyContactDto> emergencyContacts) {
        this.emergencyContacts = emergencyContacts;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
