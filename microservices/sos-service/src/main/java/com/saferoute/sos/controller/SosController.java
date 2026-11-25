package com.saferoute.sos.controller;

import com.saferoute.sos.dto.CreateSosRequest;
import com.saferoute.sos.dto.PatchSosRequest;
import com.saferoute.sos.dto.UpdateLocationRequest;
import com.saferoute.sos.entity.SosAlert;
import com.saferoute.sos.service.SosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/sos")
@Tag(name = "MS4 - SoS Service", description = "Gestion des alertes d'urgence SOS (Safe Route)")
public class SosController {

    private final SosService sosService;

    public SosController(SosService sosService) {
        this.sosService = sosService;
    }

    @PostMapping
    @Operation(summary = "Déclencher une alerte SOS", description = "Crée une nouvelle alerte SOS avec la géolocalisation actuelle de l'utilisateur, récupère les contacts d'urgence (Feign) et publie un événement RabbitMQ.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Alerte SOS déclenchée avec succès",
                    headers = @Header(name = "Location", description = "URI de la ressource créée", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Données d'entrée invalides (Problem Details RFC 9457)")
    })
    public ResponseEntity<SosAlert> triggerAlert(
            @Valid @RequestBody CreateSosRequest request,
            @Parameter(description = "Identifiant utilisateur facultatif dans l'en-tête (ex: injecté par la Gateway ou Keycloak)")
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        SosAlert alert = sosService.triggerAlert(request, headerUserId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(alert.getId())
                .toUri();
        return ResponseEntity.created(location).body(alert);
    }

    @GetMapping("/me")
    @Operation(summary = "Consulter mes alertes SOS", description = "Retourne la liste des alertes de l'utilisateur connecté.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historique des alertes retourné")
    })
    public ResponseEntity<List<SosAlert>> getMyAlerts(
            @Parameter(description = "Identifiant utilisateur dans l'en-tête")
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "Identifiant utilisateur en paramètre de requête (pratique pour tester dans Swagger UI)")
            @RequestParam(value = "userId", required = false, defaultValue = "1") Long paramUserId) {
        Long userId = (headerUserId != null) ? headerUserId : paramUserId;
        return ResponseEntity.ok(sosService.getUserAlerts(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une alerte par ID", description = "Récupère les détails d'une alerte SOS spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerte trouvée"),
            @ApiResponse(responseCode = "400", description = "Identifiant invalide (Problem Details)"),
            @ApiResponse(responseCode = "404", description = "Alerte introuvable (Problem Details)")
    })
    public ResponseEntity<SosAlert> getAlertById(
            @Parameter(description = "ID de l'alerte") @PathVariable Long id) {
        return ResponseEntity.ok(sosService.getAlertById(id));
    }

    @PutMapping("/{id}/location")
    @Operation(summary = "Mettre à jour la géolocalisation", description = "Met à jour la position GPS en temps réel pour une alerte SOS ouverte.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Position mise à jour"),
            @ApiResponse(responseCode = "400", description = "Coordonnées GPS invalides"),
            @ApiResponse(responseCode = "404", description = "Alerte introuvable"),
            @ApiResponse(responseCode = "409", description = "Alerte déjà clôturée (RESOLUE ou ANNULEE)")
    })
    public ResponseEntity<SosAlert> updateLocation(
            @Parameter(description = "ID de l'alerte") @PathVariable Long id,
            @Valid @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(sosService.updateLocation(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modifier partiellement une alerte SOS", description = "Met à jour uniquement les champs fournis (position, message, statut) d'une alerte ouverte.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerte mise à jour partiellement"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Alerte introuvable"),
            @ApiResponse(responseCode = "409", description = "Alerte déjà clôturée")
    })
    public ResponseEntity<SosAlert> patchAlert(
            @Parameter(description = "ID de l'alerte") @PathVariable Long id,
            @Valid @RequestBody PatchSosRequest request) {
        return ResponseEntity.ok(sosService.patchAlert(id, request));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Annuler une alerte SOS", description = "L'utilisateur annule son alerte en cas de fausse alerte ou si le danger est écarté.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerte marquée comme ANNULEE"),
            @ApiResponse(responseCode = "404", description = "Alerte introuvable"),
            @ApiResponse(responseCode = "409", description = "Alerte déjà clôturée")
    })
    public ResponseEntity<SosAlert> cancelAlert(
            @Parameter(description = "ID de l'alerte") @PathVariable Long id) {
        return ResponseEntity.ok(sosService.cancelAlert(id));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Résoudre une alerte SOS (ADMIN)", description = "Marque l'alerte comme RESOLUE une fois l'intervention des secours terminée.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerte marquée comme RESOLUE"),
            @ApiResponse(responseCode = "403", description = "Accès réservé au rôle ADMIN (en mode sécurisé Keycloak)"),
            @ApiResponse(responseCode = "404", description = "Alerte introuvable"),
            @ApiResponse(responseCode = "409", description = "Alerte déjà clôturée")
    })
    public ResponseEntity<SosAlert> resolveAlert(
            @Parameter(description = "ID de l'alerte") @PathVariable Long id) {
        return ResponseEntity.ok(sosService.resolveAlert(id));
    }

    @GetMapping("/active")
    @Operation(summary = "Lister les alertes actives (ADMIN)", description = "Retourne toutes les alertes ayant le statut ACTIVE ou EN_COURS pour supervision.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des alertes actives"),
            @ApiResponse(responseCode = "403", description = "Accès réservé au rôle ADMIN (en mode sécurisé Keycloak)")
    })
    public ResponseEntity<List<SosAlert>> getActiveAlerts() {
        return ResponseEntity.ok(sosService.getActiveAlerts());
    }
}
