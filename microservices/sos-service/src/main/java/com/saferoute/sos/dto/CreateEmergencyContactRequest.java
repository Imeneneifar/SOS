package com.saferoute.sos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateEmergencyContactRequest {

    @Schema(description = "Identifiant de l'utilisateur propriétaire (optionnel si header X-User-Id)", example = "1")
    private Long userId;

    @NotBlank(message = "Le nom du contact est obligatoire")
    @Size(max = 120, message = "Le nom ne doit pas dépasser 120 caractères")
    @Schema(description = "Nom complet du contact d'urgence", example = "Ahmed Trabelsi")
    private String nom;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^[+0-9\\s\\-()]{6,25}$", message = "Le numéro de téléphone a un format invalide")
    @Schema(description = "Numéro de téléphone du contact", example = "+216 98 123 456")
    private String telephone;

    @Size(max = 60, message = "La relation ne doit pas dépasser 60 caractères")
    @Schema(description = "Lien de parenté ou relation", example = "Père")
    private String relation;

    public CreateEmergencyContactRequest() {
    }

    public CreateEmergencyContactRequest(Long userId, String nom, String telephone, String relation) {
        this.userId = userId;
        this.nom = nom;
        this.telephone = telephone;
        this.relation = relation;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }
}
