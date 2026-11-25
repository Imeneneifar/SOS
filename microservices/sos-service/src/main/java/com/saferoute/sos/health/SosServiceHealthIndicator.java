package com.saferoute.sos.health;

import com.saferoute.sos.entity.SosStatus;
import com.saferoute.sos.repository.SosAlertRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HealthIndicator personnalisé (Chapitre 4 — Observabilité avec Spring Boot Actuator).
 * Vérifie l'état métier du moteur d'urgences SOS (accès aux alertes, alertes actives,
 * mode de communication) et remonte la vraie santé à Eureka via eureka.client.healthcheck.enabled=true.
 */
@Component("sosEmergencyEngine")
public class SosServiceHealthIndicator implements HealthIndicator {

    private final SosAlertRepository sosAlertRepository;
    private final boolean rabbitmqEnabled;
    private final boolean feignUserServiceEnabled;

    public SosServiceHealthIndicator(
            SosAlertRepository sosAlertRepository,
            @Value("${saferoute.messaging.rabbitmq.enabled:false}") boolean rabbitmqEnabled,
            @Value("${saferoute.clients.user-service.enabled:false}") boolean feignUserServiceEnabled) {
        this.sosAlertRepository = sosAlertRepository;
        this.rabbitmqEnabled = rabbitmqEnabled;
        this.feignUserServiceEnabled = feignUserServiceEnabled;
    }

    @Override
    public Health health() {
        try {
            long totalAlerts = sosAlertRepository.count();
            long activeAlerts = sosAlertRepository.findByStatutInOrderByDateDesc(
                    List.of(SosStatus.ACTIVE, SosStatus.EN_COURS)
            ).size();

            return Health.up()
                    .withDetail("service", "MS4 - SoS Service")
                    .withDetail("totalAlerts", totalAlerts)
                    .withDetail("activeAlerts", activeAlerts)
                    .withDetail("messagingMode", rabbitmqEnabled ? "RabbitMQ" : "LocalSimulated")
                    .withDetail("feignUserIntegration", feignUserServiceEnabled ? "Enabled (with local fallback)" : "LocalFallbackOnly")
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("service", "MS4 - SoS Service")
                    .withDetail("reason", "Impossible d'accéder au référentiel des alertes SOS")
                    .build();
        }
    }
}
