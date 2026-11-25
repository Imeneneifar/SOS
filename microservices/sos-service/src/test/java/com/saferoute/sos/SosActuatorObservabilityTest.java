package com.saferoute.sos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'observabilité Spring Boot Actuator (Chapitre 4) :
 * /actuator/health (avec HealthIndicator personnalisé sosEmergencyEngine),
 * sondes liveness/readiness, /actuator/info, /actuator/metrics et /actuator/loggers.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "saferoute.security.enabled=false",
        "saferoute.messaging.rabbitmq.enabled=false",
        "saferoute.clients.user-service.enabled=false"
})
@AutoConfigureMockMvc
class SosActuatorObservabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealth_exposesCustomHealthIndicatorAndDbDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.sosEmergencyEngine.status").value("UP"))
                .andExpect(jsonPath("$.components.sosEmergencyEngine.details.service").value("MS4 - SoS Service"))
                .andExpect(jsonPath("$.components.sosEmergencyEngine.details.totalAlerts").isNumber());

        // Sondes d'orchestrateur (Chapitre 4)
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorInfo_exposesApplicationMetadataForEurekaDashboard() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("sos-service"))
                .andExpect(jsonPath("$.app.version").value("1.0.0"))
                .andExpect(jsonPath("$.app.owner").value("Imen"))
                .andExpect(jsonPath("$.app.team").value("Safe Route"));
    }

    @Test
    void actuatorMetrics_tracksCustomSosCountersAndJvmMetrics() throws Exception {
        // Déclencher une alerte pour incrémenter le compteur métier
        mockMvc.perform(post("/sos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 99, \"latitude\": 36.8, \"longitude\": 10.1, \"message\": \"Test métrique\"}"))
                .andExpect(status().isCreated());

        // Vérifier que les métriques JVM et métier sont exposées
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItems("jvm.memory.used", "sos.alerts.triggered.total")));

        mockMvc.perform(get("/actuator/metrics/sos.alerts.triggered.total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(1.0)));
    }

    @Test
    void actuatorLoggers_allowsDynamicLogLevelUpdateWithoutRestart() throws Exception {
        // Modifier le niveau de log à chaud -> DEBUG
        mockMvc.perform(post("/actuator/loggers/com.saferoute.sos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configuredLevel\": \"DEBUG\"}"))
                .andExpect(status().isNoContent());

        // Vérifier que le niveau a bien changé sans redémarrer
        mockMvc.perform(get("/actuator/loggers/com.saferoute.sos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredLevel").value("DEBUG"))
                .andExpect(jsonPath("$.effectiveLevel").value("DEBUG"));

        // Remettre à INFO proprement
        mockMvc.perform(post("/actuator/loggers/com.saferoute.sos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configuredLevel\": \"INFO\"}"))
                .andExpect(status().isNoContent());
    }
}
