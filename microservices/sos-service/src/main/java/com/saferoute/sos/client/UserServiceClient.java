package com.saferoute.sos.client;

import com.saferoute.sos.client.dto.UserEmergencyContactDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * Client OpenFeign (Communication Synchrone - S6) vers MS1 (User Service - Foued).
 * Résolution dynamique via Eureka (service ID: "user-service").
 * Aligne le contrat MS1 : GET /users/{id} et GET /users/{id}/emergency-contacts.
 */
@FeignClient(
        name = "user-service",
        url = "${saferoute.clients.user-service.url:}",
        path = "/users"
)
public interface UserServiceClient {

    @GetMapping("/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id);

    @GetMapping("/{id}/emergency-contacts")
    List<UserEmergencyContactDto> getEmergencyContactsByUserId(@PathVariable("id") Long id);
}
