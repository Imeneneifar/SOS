package com.saferoute.sos.client.dto;

import java.io.Serializable;

/**
 * Représentation d'un contact d'urgence retourné par MS1 (User Service)
 * ou depuis le stockage local de secours.
 */
public class UserEmergencyContactDto implements Serializable {

    private Long id;
    private Long userId;
    private String nom;
    private String telephone;
    private String relation;

    public UserEmergencyContactDto() {
    }

    public UserEmergencyContactDto(Long id, Long userId, String nom, String telephone, String relation) {
        this.id = id;
        this.userId = userId;
        this.nom = nom;
        this.telephone = telephone;
        this.relation = relation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
