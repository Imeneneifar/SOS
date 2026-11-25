package com.saferoute.sos.controller;

import com.saferoute.sos.dto.CreateEmergencyContactRequest;
import com.saferoute.sos.dto.CreateSosRequest;
import com.saferoute.sos.entity.EmergencyContact;
import com.saferoute.sos.entity.SosAlert;
import com.saferoute.sos.service.EmergencyContactService;
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
@RequestMapping("/emergency")
@Tag(name = "Module Emergency (Contacts & Alertes)", description = "Endpoints pour les contacts d'urgence et alias de déclenchement SOS")
public class EmergencyController {

    private final SosService sosService;
    private final EmergencyContactService emergencyContactService;

    public EmergencyController(SosService sosService, EmergencyContactService emergencyContactService) {
        this.sosService = sosService;
        this.emergencyContactService = emergencyContactService;
    }

    @PostMapping("/sos")
    @Operation(summary = "Permettre à un citoyen d'envoyer une alerte SOS", description = "Alias direct vers le déclenchement d'alerte SOS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Alerte SOS créée",
                    headers = @Header(name = "Location", description = "URI de l'alerte créée", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Données invalides (Problem Details)")
    })
    public ResponseEntity<SosAlert> sendSos(
            @Valid @RequestBody CreateSosRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        SosAlert alert = sosService.triggerAlert(request, headerUserId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/sos/{id}")
                .buildAndExpand(alert.getId())
                .toUri();
        return ResponseEntity.created(location).body(alert);
    }

    @GetMapping("/contacts")
    @Operation(summary = "Lister les contacts d'urgence de l'utilisateur", description = "Récupère les contacts d'urgence configurés")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des contacts retournée")
    })
    public ResponseEntity<List<EmergencyContact>> getContacts(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "Identifiant utilisateur (défaut = 1)")
            @RequestParam(value = "userId", required = false, defaultValue = "1") Long paramUserId) {
        Long userId = (headerUserId != null) ? headerUserId : paramUserId;
        return ResponseEntity.ok(emergencyContactService.getContactsByUserId(userId));
    }

    @PostMapping("/contacts")
    @Operation(summary = "Ajouter un contact d'urgence", description = "Ajoute un contact à prévenir en cas de danger")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contact d'urgence créé",
                    headers = @Header(name = "Location", description = "URI du contact créé", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Données de contact invalides (Problem Details)")
    })
    public ResponseEntity<EmergencyContact> addContact(
            @Valid @RequestBody CreateEmergencyContactRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        EmergencyContact saved = emergencyContactService.addContact(request, headerUserId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @DeleteMapping("/contacts/{id}")
    @Operation(summary = "Supprimer un contact d'urgence", description = "Supprime un contact par son identifiant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Contact supprimé"),
            @ApiResponse(responseCode = "404", description = "Contact introuvable (Problem Details)")
    })
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        emergencyContactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}
