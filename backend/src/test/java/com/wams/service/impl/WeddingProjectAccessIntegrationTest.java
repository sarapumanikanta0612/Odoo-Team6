package com.wams.service.impl;

import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.entity.WeddingProject;
import com.wams.enums.ProjectStatus;
import com.wams.enums.Role;
import com.wams.repository.ClientRepository;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.UserRepository;
import com.wams.repository.WeddingProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end verification of the role-based scope for GET /api/projects and
 * GET /api/projects/{id}. Uses the real security chain, real JPA/H2, real
 * controller, service, and repositories. Only the authenticated principal is
 * synthetic (via {@code @WithMockUser} with the seeded user's email).
 *
 * <p>Seed data:
 * <pre>
 *   admin        (ADMIN)
 *   planner-a    (LEAD_PLANNER) -> Employee empA -> Project A
 *   planner-b    (LEAD_PLANNER) -> Employee empB -> Project B
 *   client-a     (PRIMARY_CLIENT) <- Project A
 *   client-b     (PRIMARY_CLIENT) <- Project B
 *   partner-a    (PARTNER_CLIENT) <- Project A (partnerClient)
 *   plain-emp    (EMPLOYEE) - no FK path to any project
 *   vendor-1     (VENDOR)   - no FK path to any project
 * </pre>
 *
 * <p>This class uses a dedicated in-memory H2 database. Its repository-wide
 * cleanup therefore cannot delete data owned by another cached Spring test
 * context that uses the default {@code wams_test} database.
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:wams_project_access_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class WeddingProjectAccessIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired WeddingProjectRepository projectRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Long projectAId;
    private Long projectBId;
    private Long employeeAId;
    private Long employeeBId;

    @BeforeEach
    void seed() {
        // Clean this test class's isolated H2 database before each test case.
        projectRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();

        newUser("admin@wams.test", Role.ADMIN);

        User plannerA = newUser("planner-a@wams.test", Role.LEAD_PLANNER);
        User plannerB = newUser("planner-b@wams.test", Role.LEAD_PLANNER);
        Employee empA = newEmployee(plannerA, "EMP-A");
        Employee empB = newEmployee(plannerB, "EMP-B");
        employeeAId = empA.getId();
        employeeBId = empB.getId();

        User userCA = newUser("client-a@wams.test", Role.PRIMARY_CLIENT);
        User userCB = newUser("client-b@wams.test", Role.PRIMARY_CLIENT);
        Client clientA = newClient(userCA);
        Client clientB = newClient(userCB);

        User userPA = newUser("partner-a@wams.test", Role.PARTNER_CLIENT);
        Client partnerA = newClient(userPA);

        newUser("plain-emp@wams.test", Role.EMPLOYEE);
        newUser("vendor-1@wams.test", Role.VENDOR);

        projectAId = projectRepository.save(WeddingProject.builder()
                .projectName("Alice & Bob")
                .primaryClient(clientA)
                .partnerClient(partnerA)
                .leadPlanner(empA)
                .weddingDate(LocalDate.now().plusDays(60))
                .venue("Taj Palace, Jaipur")
                .projectStatus(ProjectStatus.PLANNING)
                .agreementSigned(true)
                .bookingDepositPaid(true)
                .build()).getId();

        projectBId = projectRepository.save(WeddingProject.builder()
                .projectName("Carol & Dan")
                .primaryClient(clientB)
                .leadPlanner(empB)
                .weddingDate(LocalDate.now().plusDays(90))
                .venue("Oberoi, Udaipur")
                .projectStatus(ProjectStatus.PLANNING)
                .agreementSigned(true)
                .bookingDepositPaid(true)
                .build()).getId();
    }

    // ---------- LIST ----------

    @Test
    @WithMockUser(username = "admin@wams.test", roles = "ADMIN")
    void admin_list_seesAllProjects() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "planner-a@wams.test", roles = "LEAD_PLANNER")
    void leadPlannerA_list_seesOnlyProjectsAssignedToThem() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].projectName").value("Alice & Bob"));
    }

    @Test
    @WithMockUser(username = "client-a@wams.test", roles = "PRIMARY_CLIENT")
    void primaryClientA_list_seesOnlyTheirOwnProject() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].projectName").value("Alice & Bob"));
    }

    @Test
    @WithMockUser(username = "partner-a@wams.test", roles = "PARTNER_CLIENT")
    void partnerClientA_list_seesOnlyTheirPartneredProject() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].projectName").value("Alice & Bob"));
    }

    @Test
    @WithMockUser(username = "plain-emp@wams.test", roles = "EMPLOYEE")
    void employee_list_isForbidden() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "vendor-1@wams.test", roles = "VENDOR")
    void vendor_list_isForbidden() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isForbidden());
    }

    // ---------- GET BY ID ----------

    @Test
    @WithMockUser(username = "admin@wams.test", roles = "ADMIN")
    void admin_getById_anyProject_returns200() throws Exception {
        mvc.perform(get("/api/projects/" + projectAId)).andExpect(status().isOk());
        mvc.perform(get("/api/projects/" + projectBId)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "planner-a@wams.test", roles = "LEAD_PLANNER")
    void leadPlannerA_getById_ownProject_returns200() throws Exception {
        mvc.perform(get("/api/projects/" + projectAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectAId));
    }

    @Test
    @WithMockUser(username = "planner-a@wams.test", roles = "LEAD_PLANNER")
    void leadPlannerA_getById_othersProject_returns404_notLeakingExistence() throws Exception {
        mvc.perform(get("/api/projects/" + projectBId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "client-a@wams.test", roles = "PRIMARY_CLIENT")
    void primaryClientA_getById_otherClientsProject_returns404() throws Exception {
        mvc.perform(get("/api/projects/" + projectBId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "partner-a@wams.test", roles = "PARTNER_CLIENT")
    void partnerClientA_getById_otherProjectWithoutPartner_returns404() throws Exception {
        mvc.perform(get("/api/projects/" + projectBId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "vendor-1@wams.test", roles = "VENDOR")
    void vendor_getById_anyProject_returns403() throws Exception {
        mvc.perform(get("/api/projects/" + projectAId)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "plain-emp@wams.test", roles = "EMPLOYEE")
    void employee_getById_anyProject_returns403() throws Exception {
        mvc.perform(get("/api/projects/" + projectAId)).andExpect(status().isForbidden());
    }

    // ---------- UPDATE (PUT) ----------
    //
    // Same ownership rules as GET /api/projects/{id}:
    //   ADMIN -> any; LEAD_PLANNER -> only own (404 on cross-planner);
    //   clients/employees/vendors -> forbidden (403 - at URL layer, since
    //   SecurityConfig already restricts PUT to ADMIN/LEAD_PLANNER).

    private static final String VENUE_PATCH =
            "{\"venue\":\"Updated by test\"}";

    @Test
    void anonymous_putProject_returns401() throws Exception {
        mvc.perform(put("/api/projects/" + projectAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENUE_PATCH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@wams.test", roles = "ADMIN")
    void admin_update_anyProject_returns200() throws Exception {
        mvc.perform(put("/api/projects/" + projectBId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENUE_PATCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue").value("Updated by test"));
    }

    @Test
    @WithMockUser(username = "admin@wams.test", roles = "ADMIN")
    void admin_reassignProjectToDifferentLeadPlanner_returns200() throws Exception {
        mvc.perform(put("/api/projects/" + projectAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leadPlannerId\":" + employeeBId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadPlannerId").value(employeeBId));

        WeddingProject updated = projectRepository.findById(projectAId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getLeadPlanner().getId())
                .isEqualTo(employeeBId);
    }

    @Test
    @WithMockUser(username = "planner-a@wams.test", roles = "LEAD_PLANNER")
    void leadPlannerA_reassignOwnProjectToDifferentPlanner_returns403_withoutMutation() throws Exception {
        mvc.perform(put("/api/projects/" + projectAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"venue\":\"Must Not Apply\",\"leadPlannerId\":"
                                + employeeBId + "}"))
                .andExpect(status().isForbidden());

        WeddingProject unchanged = projectRepository.findById(projectAId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(unchanged.getLeadPlanner().getId())
                .isEqualTo(employeeAId);
        org.assertj.core.api.Assertions.assertThat(unchanged.getVenue())
                .isEqualTo("Taj Palace, Jaipur");
    }

    @Test
    @WithMockUser(username = "planner-a@wams.test", roles = "LEAD_PLANNER")
    void leadPlannerA_submitCurrentPlannerId_allowsOtherPermittedUpdates() throws Exception {
        mvc.perform(put("/api/projects/" + projectAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"venue\":\"Planner Updated Venue\",\"leadPlannerId\":"
                                + employeeAId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue").value("Planner Updated Venue"))
                .andExpect(jsonPath("$.leadPlannerId").value(employeeAId));
    }

    @Test
    @WithMockUser(username = "planner-a@wams.test", roles = "LEAD_PLANNER")
    void leadPlannerA_update_ownProject_returns200() throws Exception {
        mvc.perform(put("/api/projects/" + projectAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENUE_PATCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue").value("Updated by test"));
    }

    @Test
    @WithMockUser(username = "planner-a@wams.test", roles = "LEAD_PLANNER")
    void leadPlannerA_update_othersProject_returns404_notLeakingExistence() throws Exception {
        mvc.perform(put("/api/projects/" + projectBId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENUE_PATCH))
                .andExpect(status().isNotFound());

        // Confirm the project was NOT mutated by the rejected request.
        WeddingProject fresh = projectRepository.findById(projectBId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(fresh.getVenue())
                .isEqualTo("Oberoi, Udaipur");
    }

    @Test
    @WithMockUser(username = "client-a@wams.test", roles = "PRIMARY_CLIENT")
    void primaryClient_update_isForbidden() throws Exception {
        mvc.perform(put("/api/projects/" + projectAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENUE_PATCH))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "partner-a@wams.test", roles = "PARTNER_CLIENT")
    void partnerClient_update_isForbidden() throws Exception {
        mvc.perform(put("/api/projects/" + projectAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENUE_PATCH))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "plain-emp@wams.test", roles = "EMPLOYEE")
    void employee_update_isForbidden() throws Exception {
        mvc.perform(put("/api/projects/" + projectAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENUE_PATCH))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "vendor-1@wams.test", roles = "VENDOR")
    void vendor_update_isForbidden() throws Exception {
        mvc.perform(put("/api/projects/" + projectAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENUE_PATCH))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private User newUser(String email, Role role) {
        return userRepository.save(User.builder()
                .fullName(email)
                .email(email)
                .passwordHash(passwordEncoder.encode("s3cret-password"))
                .role(role)
                .active(true)
                .build());
    }

    private Employee newEmployee(User user, String code) {
        return employeeRepository.save(Employee.builder()
                .user(user)
                .employeeCode(code)
                .active(true)
                .build());
    }

    private Client newClient(User user) {
        return clientRepository.save(Client.builder()
                .user(user)
                .build());
    }
}
