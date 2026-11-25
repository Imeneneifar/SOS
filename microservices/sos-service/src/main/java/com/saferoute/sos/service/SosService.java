package com.saferoute.sos.service;

import com.saferoute.sos.client.dto.UserEmergencyContactDto;
import com.saferoute.sos.dto.CreateSosRequest;
import com.saferoute.sos.dto.PatchSosRequest;
import com.saferoute.sos.dto.UpdateLocationRequest;
import com.saferoute.sos.entity.SosAlert;
import com.saferoute.sos.entity.SosStatus;
import com.saferoute.sos.exception.BusinessConflictException;
import com.saferoute.sos.exception.ResourceNotFoundException;
import com.saferoute.sos.messaging.SosEventPublisher;
import com.saferoute.sos.repository.SosAlertRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SosService {

    private static final Logger log = LoggerFactory.getLogger(SosService.class);

    private final SosAlertRepository sosAlertRepository;
    private final UserIntegrationService userIntegrationService;
    private final SosEventPublisher sosEventPublisher;
    private final Counter triggeredAlertsCounter;
    private final Counter resolvedAlertsCounter;
    private final Counter cancelledAlertsCounter;

    public SosService(
            SosAlertRepository sosAlertRepository,
            UserIntegrationService userIntegrationService,
            SosEventPublisher sosEventPublisher,
            MeterRegistry meterRegistry) {
        this.sosAlertRepository = sosAlertRepository;
        this.userIntegrationService = userIntegrationService;
        this.sosEventPublisher = sosEventPublisher;
        this.triggeredAlertsCounter = Counter.builder("sos.alerts.triggered.total")
                .description("Nombre total d'alertes SOS déclenchées")
                .register(meterRegistry);
        this.resolvedAlertsCounter = Counter.builder("sos.alerts.resolved.total")
                .description("Nombre total d'alertes SOS résolues")
                .register(meterRegistry);
        this.cancelledAlertsCounter = Counter.builder("sos.alerts.cancelled.total")
                .description("Nombre total d'alertes SOS annulées")
                .register(meterRegistry);
    }

    /**
     * POST /sos : Déclencher une alerte SOS
     */
    public SosAlert triggerAlert(CreateSosRequest request, Long authenticatedUserId) {
        Long targetUserId = (authenticatedUserId != null) ? authenticatedUserId :
                (request.getUserId() != null ? request.getUserId() : 1L);

        SosAlert alert = new SosAlert();
        alert.setUserId(targetUserId);
        alert.setLatitude(request.getLatitude());
        alert.setLongitude(request.getLongitude());
        alert.setMessage(request.getMessage() != null && !request.getMessage().isBlank()
                ? request.getMessage().trim()
                : "Alerte SOS d'urgence déclenchée !");
        alert.setStatut(SosStatus.ACTIVE);
        alert.setDate(LocalDateTime.now());

        SosAlert saved = sosAlertRepository.save(alert);
        triggeredAlertsCounter.increment();

        log.info("[SOS SERVICE] Alerte #{} déclenchée pour l'utilisateur #{} (lat={}, lng={}).",
                saved.getId(), targetUserId, saved.getLatitude(), saved.getLongitude());

        // S6 (Communication synchrone) : récupération des contacts d'urgence via OpenFeign (MS1) avec repli local
        List<UserEmergencyContactDto> contacts = userIntegrationService.resolveEmergencyContacts(targetUserId);

        // S7 (Communication asynchrone) : publication de l'événement métier sur RabbitMQ
        sosEventPublisher.publishAlertTriggered(saved, contacts);

        return saved;
    }

    /**
     * GET /sos/me : Liste des alertes de l'utilisateur
     */
    @Transactional(readOnly = true)
    public List<SosAlert> getUserAlerts(Long userId) {
        Long targetUserId = (userId != null) ? userId : 1L;
        return sosAlertRepository.findByUserIdOrderByDateDesc(targetUserId);
    }

    /**
     * GET /sos/{id} : Récupérer une alerte par son identifiant
     */
    @Transactional(readOnly = true)
    public SosAlert getAlertById(Long id) {
        return findAlertOrThrow(id);
    }

    /**
     * PUT /sos/{id}/location : Mettre à jour la géolocalisation en temps réel
     */
    public SosAlert updateLocation(Long id, UpdateLocationRequest request) {
        SosAlert alert = findAlertOrThrow(id);
        ensureAlertIsOpen(alert, "mettre à jour la position");

        alert.setLatitude(request.getLatitude());
        alert.setLongitude(request.getLongitude());
        SosAlert saved = sosAlertRepository.save(alert);

        log.info("[SOS SERVICE] Position mise à jour pour l'alerte #{}: lat={}, lng={}",
                id, request.getLatitude(), request.getLongitude());

        sosEventPublisher.publishLocationUpdated(saved);
        return saved;
    }

    /**
     * PATCH /sos/{id} : Modification partielle d'une alerte SOS (position, message, statut)
     */
    public SosAlert patchAlert(Long id, PatchSosRequest request) {
        SosAlert alert = findAlertOrThrow(id);
        ensureAlertIsOpen(alert, "modifier");

        boolean locationChanged = false;
        boolean statusChanged = false;

        if (request.getLatitude() != null) {
            alert.setLatitude(request.getLatitude());
            locationChanged = true;
        }
        if (request.getLongitude() != null) {
            alert.setLongitude(request.getLongitude());
            locationChanged = true;
        }
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            alert.setMessage(request.getMessage().trim());
        }
        if (request.getStatut() != null && request.getStatut() != alert.getStatut()) {
            alert.setStatut(request.getStatut());
            statusChanged = true;
        }

        SosAlert saved = sosAlertRepository.save(alert);

        if (locationChanged) {
            sosEventPublisher.publishLocationUpdated(saved);
        }
        if (statusChanged) {
            if (saved.getStatut() == SosStatus.RESOLUE) {
                resolvedAlertsCounter.increment();
            } else if (saved.getStatut() == SosStatus.ANNULEE) {
                cancelledAlertsCounter.increment();
            }
            sosEventPublisher.publishStatusChanged(saved);
        }

        return saved;
    }

    /**
     * PUT /sos/{id}/cancel : Annuler l'alerte
     */
    public SosAlert cancelAlert(Long id) {
        SosAlert alert = findAlertOrThrow(id);
        ensureAlertIsOpen(alert, "annuler");

        alert.setStatut(SosStatus.ANNULEE);
        SosAlert saved = sosAlertRepository.save(alert);
        cancelledAlertsCounter.increment();

        log.info("[SOS SERVICE] Alerte #{} annulée par l'utilisateur.", id);
        sosEventPublisher.publishStatusChanged(saved);
        return saved;
    }

    /**
     * PUT /sos/{id}/resolve : Résoudre l'alerte (ADMIN / Secours)
     */
    public SosAlert resolveAlert(Long id) {
        SosAlert alert = findAlertOrThrow(id);
        ensureAlertIsOpen(alert, "résoudre");

        alert.setStatut(SosStatus.RESOLUE);
        SosAlert saved = sosAlertRepository.save(alert);
        resolvedAlertsCounter.increment();

        log.info("[SOS SERVICE] Alerte #{} résolue par l'administrateur / secours.", id);
        sosEventPublisher.publishStatusChanged(saved);
        return saved;
    }

    /**
     * GET /sos/active : Liste des alertes actives ou en cours (ADMIN)
     */
    @Transactional(readOnly = true)
    public List<SosAlert> getActiveAlerts() {
        return sosAlertRepository.findByStatutInOrderByDateDesc(
                List.of(SosStatus.ACTIVE, SosStatus.EN_COURS)
        );
    }

    private SosAlert findAlertOrThrow(Long id) {
        return sosAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alerte SOS introuvable avec l'identifiant : " + id));
    }

    private void ensureAlertIsOpen(SosAlert alert, String action) {
        if (alert.getStatut() == SosStatus.RESOLUE || alert.getStatut() == SosStatus.ANNULEE) {
            throw new BusinessConflictException(String.format(
                    "Impossible de %s l'alerte #%d : elle est déjà clôturée avec le statut %s.",
                    action, alert.getId(), alert.getStatut()));
        }
    }
}
