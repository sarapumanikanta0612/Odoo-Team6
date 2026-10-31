package com.wams.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the SecurityConfig URL rules end-to-end: real filter chain, real
 * JwtAuthenticationFilter, real exception entry points. Each test either runs
 * anonymously (no {@code @WithMockUser}) or with an explicit role.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    private static final String VALID_ENQUIRY = """
            {
              "contactName": "Priya Sharma",
              "email": "priya.sharma@example.com",
              "phone": "+91 98765 43210",
              "message": "Please call after 6pm"
            }
            """;

    @Autowired MockMvc mvc;

    // --- 1. Anonymous POST /api/public/enquiries reaches the controller ---

    @Test
    void anonymous_postPublicEnquiry_valid_returns201_withEnquiryBody() throws Exception {
        mvc.perform(post("/api/public/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ENQUIRY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("priya.sharma@example.com"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.source").value("WEBSITE"));
    }

    @Test
    void anonymous_postPublicEnquiry_invalidBody_returns400_notAuthRejection() throws Exception {
        // Missing required fields: validation should kick in. The important bit
        // is that the response is 400 Validation and NOT 401 Unauthorized or
        // 403 Forbidden — i.e. security really did let the request through.
        mvc.perform(post("/api/public/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- 2. Other /api/public/** stays protected ---

    @Test
    void anonymous_getPublicEnquiries_isStillUnauthorized() throws Exception {
        // Same path, GET method. Not covered by the new permitAll matcher
        // (which is scoped to POST). Falls through to .anyRequest().authenticated().
        mvc.perform(get("/api/public/enquiries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_otherPublicSubpath_isStillUnauthorized() throws Exception {
        // A future /api/public/* route should NOT accidentally become public.
        mvc.perform(get("/api/public/some-future-route"))
                .andExpect(status().isUnauthorized());
    }

    // --- 3. Existing authentication and role-based rules unchanged ---

    @Test
    void anonymous_getUsers_returns401() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_postEmployee_returns401() throws Exception {
        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_postProject_returns401() throws Exception {
        mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeRole_cannotAccessUsersEndpoint_returns403() throws Exception {
        // Role-based rules must still work: EMPLOYEE is authenticated but
        // /api/users/** requires ROLE_ADMIN.
        mvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void vendorRole_cannotAccessClientsEndpoint_returns403() throws Exception {
        // /api/clients/** requires ADMIN or LEAD_PLANNER.
        mvc.perform(get("/api/clients"))
                .andExpect(status().isForbidden());
    }

    // --- Health endpoint sanity ---

    @Test
    void anonymous_health_returns200() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
