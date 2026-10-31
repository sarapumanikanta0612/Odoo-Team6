package com.wams.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that the GlobalExceptionHandler maps framework and domain
 * exceptions to the correct HTTP status without leaking internals.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN") // bypasses JWT so these tests focus on exception mapping
class GlobalExceptionHandlerIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void unknownUrl_returns404_withApiError() throws Exception {
        mvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void wrongMethod_returns405_withApiError() throws Exception {
        // /api/health is defined as GET only.
        mvc.perform(post("/api/health"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value(containsString("POST")));
    }

    @Test
    void responseStatusException_preservesStatus_doesNotEchoArbitraryReason() throws Exception {
        // The handler must preserve the HTTP status but must NOT reflect the
        // caller-supplied reason string back to the client — that field is
        // untrusted on arbitrary throw sites (framework internals, libraries).
        mvc.perform(get("/__test__/response-status/409")
                        .param("reason", "secret-internal-detail-/etc/passwd"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Conflict"))
                .andExpect(content().string(not(containsString("secret-internal-detail"))))
                .andExpect(content().string(not(containsString("/etc/passwd"))));
    }

    @Test
    void responseStatusException_418_preservesStatus_withGenericMessage() throws Exception {
        mvc.perform(get("/__test__/response-status/418").param("reason", "leaked-reason"))
                .andExpect(status().is(418))
                .andExpect(jsonPath("$.status").value(418))
                // Status-derived message, not the caller-supplied reason.
                .andExpect(jsonPath("$.message").value("I'm a teapot"))
                .andExpect(content().string(not(containsString("leaked-reason"))));
    }

    @Test
    void resourceNotFoundException_returns404() throws Exception {
        mvc.perform(get("/__test__/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("forced not found"));
    }

    @Test
    void duplicateResourceException_returns409() throws Exception {
        mvc.perform(get("/__test__/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("forced duplicate"));
    }

    @Test
    void businessRuleException_returns422() throws Exception {
        mvc.perform(get("/__test__/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void unexpectedException_returns500_withoutLeakingInternals() throws Exception {
        mvc.perform(get("/__test__/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                // The raw exception message must NOT appear anywhere in the body.
                .andExpect(content().string(not(containsString("internal detail"))));
    }

    @Test
    void validationError_returns400_withFieldDetails() throws Exception {
        // RegisterRequest requires non-blank fullName/email and 8-72 char password.
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
