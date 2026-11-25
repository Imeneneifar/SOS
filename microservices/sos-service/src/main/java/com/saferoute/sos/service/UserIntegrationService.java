package com.saferoute.sos.service;

import com.saferoute.sos.client.UserServiceClient;
import com.saferoute.sos.client.dto.UserEmergencyContactDto;
import com.saferoute.sos.repository.EmergencyContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service d'intégration synchrone avec MS1 (User Service) via OpenFeign,
 * avec repli résilient (fallback) sur le référentiel local de contacts d'urgence
 * afin qu'une panne de MS1 ne bloque jamais le déclenchement d'une alerte SOS.
 */
@Service
public class UserIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(UserIntegrationService.class);

    private final UserServiceClient userServiceClient;
    private final EmergencyContactRepository emergencyContactRepository;
    private final boolean feignEnabled;

    public UserIntegrationService(
            UserServiceClient userServiceClient,
            EmergencyContactRepository emergencyContactRepository,
            @Value("${saferoute.clients.user-service.enabled:false}") boolean feignEnabled) {
        this.userServiceClient = userServiceClient;
        this.emergencyContactRepository = emergencyContactRepository;
        this.feignEnabled = feignEnabled;
    }

    public List<UserEmergencyContactDto> resolveEmergencyContacts(Long userId) {
        if (feignEnabled) {
            try {
                List<UserEmergencyContactDto> remoteContacts = userServiceClient.getEmergencyContactsByUserId(userId);
                if (remoteContacts != null && !remoteContacts.isEmpty()) {
                    log.info("[FEIGN -> MS1] {} contact(s) d'urgence récupéré(s) depuis user-service pour l'utilisateur #{}",
                            remoteContacts.size(), userId);
                    return remoteContacts;
                }
            } catch (Exception ex) {
                log.warn("[FEIGN FALLBACK] MS1 (user-service) indisponible pour l'utilisateur #{} ({}). "
                        + "Bascule automatique sur les contacts locaux.", userId, ex.getMessage());
            }
        }

        List<UserEmergencyContactDto> localContacts = emergencyContactRepository.findByUserId(userId)
                .stream()
                .map(c -> new UserEmergencyContactDto(c.getId(), c.getUserId(), c.getNom(), c.getTelephone(), c.getRelation()))
                .toList();

        log.info("[LOCAL CONTACTS] {} contact(s) d'urgence trouvé(s) localement pour l'utilisateur #{}",
                localContacts.size(), userId);
        return localContacts;
    }
}
