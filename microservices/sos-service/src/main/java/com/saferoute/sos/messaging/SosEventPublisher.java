package com.saferoute.sos.messaging;

import com.saferoute.sos.client.dto.UserEmergencyContactDto;
import com.saferoute.sos.entity.SosAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Producteur d'événements asynchrones SOS vers RabbitMQ.
 * Garantit le découplage temporel : une indisponibilité du broker ne bloque jamais
 * l'enregistrement d'une alerte SOS.
 */
@Component
public class SosEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SosEventPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final boolean rabbitEnabled;

    public SosEventPublisher(
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
            @Value("${saferoute.messaging.rabbitmq.enabled:false}") boolean rabbitEnabled) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.rabbitEnabled = rabbitEnabled;
    }

    public void publishAlertTriggered(SosAlert alert, List<UserEmergencyContactDto> contacts) {
        SosAlertEvent event = new SosAlertEvent(
                "SOS_TRIGGERED",
                alert.getId(),
                alert.getUserId(),
                alert.getLatitude(),
                alert.getLongitude(),
                alert.getMessage(),
                alert.getStatut(),
                contacts != null ? contacts : Collections.emptyList(),
                Instant.now()
        );
        send(RabbitMQConfig.ROUTING_KEY_TRIGGERED, event);
    }

    public void publishLocationUpdated(SosAlert alert) {
        SosAlertEvent event = new SosAlertEvent(
                "SOS_LOCATION_UPDATED",
                alert.getId(),
                alert.getUserId(),
                alert.getLatitude(),
                alert.getLongitude(),
                alert.getMessage(),
                alert.getStatut(),
                Collections.emptyList(),
                Instant.now()
        );
        send(RabbitMQConfig.ROUTING_KEY_LOCATION, event);
    }

    public void publishStatusChanged(SosAlert alert) {
        SosAlertEvent event = new SosAlertEvent(
                "SOS_STATUS_CHANGED",
                alert.getId(),
                alert.getUserId(),
                alert.getLatitude(),
                alert.getLongitude(),
                alert.getMessage(),
                alert.getStatut(),
                Collections.emptyList(),
                Instant.now()
        );
        send(RabbitMQConfig.ROUTING_KEY_STATUS, event);
    }

    private void send(String routingKey, SosAlertEvent event) {
        if (!rabbitEnabled) {
            log.info("[RABBITMQ MODE LOCAL - SIMULÉ] Événement '{}' (routingKey={}) pour alerte #{} (statut={}, contacts={})",
                    event.getEventType(), routingKey, event.getAlertId(), event.getStatut(),
                    event.getEmergencyContacts().size());
            return;
        }

        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.warn("[RABBITMQ] RabbitTemplate indisponible, événement '{}' non envoyé pour alerte #{}",
                    event.getEventType(), event.getAlertId());
            return;
        }

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.SOS_EXCHANGE, routingKey, event);
            log.info("[RABBITMQ] Événement '{}' publié sur exchange '{}' (routingKey={}) pour alerte #{}",
                    event.getEventType(), RabbitMQConfig.SOS_EXCHANGE, routingKey, event.getAlertId());
        } catch (Exception ex) {
            log.error("[RABBITMQ] Échec d'envoi de l'événement '{}' pour alerte #{} (l'alerte reste sauvegardée) : {}",
                    event.getEventType(), event.getAlertId(), ex.getMessage());
        }
    }
}
