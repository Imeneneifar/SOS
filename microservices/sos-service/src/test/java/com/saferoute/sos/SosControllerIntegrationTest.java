package com.saferoute.sos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "saferoute.security.enabled=false",
        "saferoute.messaging.rabbitmq.enabled=false",
        "saferoute.clients.user-service.enabled=false"
})
@AutoConfigureMockMvc
class SosControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullSosLifecycle_returnsProperCodesHeadersAndProblemDetails() throws Exception {
        // 1. POST /sos -> 201 Created + Location header
        String payload = """
                {
                  "userId": 42,
                  "latitude": 36.8065,
                  "longitude": 10.1815,
                  "message": "Urgence test intégration"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/sos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/sos/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.statut").value("ACTIVE"))
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String createdId = location.substring(location.lastIndexOf('/') + 1);

        // 2. GET /sos/{id} -> 200 OK
        mockMvc.perform(get("/sos/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Urgence test intégration"));

        // 3. PUT /sos/{id}/location -> 200 OK
        String locationUpdate = """
                {
                  "latitude": 36.8100,
                  "longitude": 10.1900
                }
                """;
        mockMvc.perform(put("/sos/{id}/location", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(36.8100))
                .andExpect(jsonPath("$.longitude").value(10.1900));

        // 4. PATCH /sos/{id} -> 200 OK (modification partielle)
        String patchBody = """
                {
                  "statut": "EN_COURS",
                  "message": "Secours en route"
                }
                """;
        mockMvc.perform(patch("/sos/{id}", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"))
                .andExpect(jsonPath("$.message").value("Secours en route"))
                .andExpect(jsonPath("$.latitude").value(36.8100));

        // 5. PUT /sos/{id}/resolve -> 200 OK
        mockMvc.perform(put("/sos/{id}/resolve", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RESOLUE"));

        // 6. Conflit métier (RFC 9457) : annuler une alerte déjà résolue -> 409 Conflict
        mockMvc.perform(put("/sos/{id}/cancel", createdId))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflit métier"))
                .andExpect(jsonPath("$.detail", containsString("RESOLUE")));

        // 7. Conflit métier : maj GPS sur alerte clôturée -> 409 Conflict
        mockMvc.perform(put("/sos/{id}/location", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationUpdate))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void validationErrors_return400ProblemDetailsWithFieldErrors() throws Exception {
        String invalidPayload = """
                {
                  "latitude": 120.5,
                  "longitude": null
                }
                """;

        mockMvc.perform(post("/sos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.latitude").exists())
                .andExpect(jsonPath("$.errors.longitude").exists());
    }

    @Test
    void malformedJsonAndInvalidIdAndNotFound_returnProblemDetails() throws Exception {
        // Malformed JSON -> 400
        mockMvc.perform(post("/sos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"latitude\": 36.8 "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        // Non-numeric ID -> 400
        mockMvc.perform(get("/sos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        // Unknown ID -> 404
        mockMvc.perform(get("/sos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void emergencyContactsCrudAndValidation() throws Exception {
        // Invalid contact -> 400 Problem Details
        mockMvc.perform(post("/emergency/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\": \"\", \"telephone\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.nom").exists())
                .andExpect(jsonPath("$.errors.telephone").exists());

        // Valid contact -> 201 Created + Location
        MvcResult result = mockMvc.perform(post("/emergency/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 7, \"nom\": \"Sami Ben Ali\", \"telephone\": \"+216 55 111 222\", \"relation\": \"Frère\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nom").value("Sami Ben Ali"))
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        String contactId = location.substring(location.lastIndexOf('/') + 1);

        // DELETE -> 204 No Content
        mockMvc.perform(delete("/emergency/contacts/{id}", contactId))
                .andExpect(status().isNoContent());

        // Second DELETE -> 404 Problem Details
        mockMvc.perform(delete("/emergency/contacts/{id}", contactId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
