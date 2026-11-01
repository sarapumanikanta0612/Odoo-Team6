package com.wams.quotation;

import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.entity.WeddingProject;
import com.wams.enums.ProjectStatus;
import com.wams.enums.Role;
import com.wams.operations.WeddingFunction;
import com.wams.operations.WeddingFunctionRepository;
import com.wams.repository.ClientRepository;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.UserRepository;
import com.wams.repository.WeddingProjectRepository;
import com.wams.security.JwtService;
import com.wams.vendor.Vendor;
import com.wams.vendor.VendorCategory;
import com.wams.vendor.VendorRepository;
import com.wams.vendor.VendorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:wams_quotation_workflow_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class QuotationWorkflowIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired QuotationRepository quotations;
    @Autowired WeddingFunctionRepository functions;
    @Autowired WeddingProjectRepository projects;
    @Autowired VendorRepository vendors;
    @Autowired ClientRepository clients;
    @Autowired EmployeeRepository employees;
    @Autowired UserRepository users;

    private User plannerUser;
    private User unrelatedPlannerUser;
    private User primaryClientUser;
    private User unrelatedClientUser;
    private User vendorAUser;
    private User vendorBUser;
    private WeddingProject project;
    private WeddingFunction function;
    private Vendor vendorA;
    private Vendor vendorB;
    private LocalDate deadline;

    @BeforeEach
    void cleanAndSeed() {
        quotations.deleteAll();
        functions.deleteAll();
        projects.deleteAll();
        vendors.deleteAll();
        clients.deleteAll();
        employees.deleteAll();
        users.deleteAll();

        plannerUser = newUser("planner@quotation.test", Role.LEAD_PLANNER);
        Employee planner = newEmployee(plannerUser, "QUOTE-PLAN");
        unrelatedPlannerUser = newUser("other-planner@quotation.test", Role.LEAD_PLANNER);
        newEmployee(unrelatedPlannerUser, "QUOTE-OTHER");

        primaryClientUser = newUser("client@quotation.test", Role.PRIMARY_CLIENT);
        Client primaryClient = newClient(primaryClientUser);
        unrelatedClientUser = newUser("other-client@quotation.test", Role.PRIMARY_CLIENT);
        newClient(unrelatedClientUser);

        vendorAUser = newUser("vendor-a@quotation.test", Role.VENDOR);
        vendorBUser = newUser("vendor-b@quotation.test", Role.VENDOR);
        vendorA = newVendor("Alpha Caterers", "alpha-contact@quotation.test", vendorAUser);
        vendorB = newVendor("Beta Caterers", "beta-contact@quotation.test", vendorBUser);

        project = projects.save(WeddingProject.builder()
                .projectName("Quotation Wedding")
                .primaryClient(primaryClient)
                .leadPlanner(planner)
                .weddingDate(LocalDate.now().plusMonths(2))
                .projectStatus(ProjectStatus.PLANNING)
                .agreementSigned(true)
                .bookingDepositPaid(true)
                .build());
        function = functions.save(WeddingFunction.builder()
                .project(project)
                .name("Wedding Ceremony")
                .functionDate(LocalDate.now().plusMonths(2))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .venue("Temple Hall")
                .build());
        deadline = LocalDate.now().plusDays(7);
    }

    @Test
    void persistedWorkflowEnforcesIsolationAndClosesTheCompetingQuotation() throws Exception {
        mvc.perform(post("/api/projects/" + project.getId() + "/quotation-requests")
                        .header("Authorization", bearer(unrelatedPlannerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(vendorA)))
                .andExpect(status().isForbidden());

        Long quotationAId = createRequest(vendorA);
        Long quotationBId = createRequest(vendorB);

        mvc.perform(get("/api/projects/" + project.getId() + "/quotation-requests")
                        .header("Authorization", bearer(unrelatedPlannerUser)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/quotation-requests/mine")
                        .header("Authorization", bearer(vendorAUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(quotationAId))
                .andExpect(jsonPath("$[0].vendorId").value(vendorA.getId()))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));

        mvc.perform(put("/api/quotation-requests/" + quotationBId + "/submission")
                        .header("Authorization", bearer(vendorAUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson("125000.00", "Attempt on another vendor request")))
                .andExpect(status().isForbidden());
        assertThat(quotations.findById(quotationBId).orElseThrow().getStatus())
                .isEqualTo(QuotationStatus.REQUESTED);

        submit(quotationAId, vendorAUser, "125000.00", "Alpha full-service proposal");

        mvc.perform(get("/api/quotation-requests/mine")
                        .header("Authorization", bearer(vendorBUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(quotationBId))
                .andExpect(jsonPath("$[0].vendorId").value(vendorB.getId()))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));
        submit(quotationBId, vendorBUser, "135000.00", "Beta full-service proposal");

        mvc.perform(get("/api/projects/" + project.getId() + "/quotation-requests")
                        .header("Authorization", bearer(plannerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(quotationAId.intValue(), quotationBId.intValue())))
                .andExpect(jsonPath("$[*].status").value(containsInAnyOrder("SUBMITTED", "SUBMITTED")));

        mvc.perform(post("/api/quotation-requests/" + quotationAId + "/decision")
                        .header("Authorization", bearer(unrelatedClientUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/quotation-requests/" + quotationAId + "/decision")
                        .header("Authorization", bearer(primaryClientUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quotationAId))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty());

        QuotationRequest winner = quotations.findById(quotationAId).orElseThrow();
        QuotationRequest competitor = quotations.findById(quotationBId).orElseThrow();
        assertThat(winner.getStatus()).isEqualTo(QuotationStatus.APPROVED);
        assertThat(winner.getDecidedBy().getId()).isEqualTo(primaryClientUser.getId());
        assertThat(winner.getDecidedAt()).isNotNull();
        assertThat(competitor.getStatus()).isEqualTo(QuotationStatus.CLOSED);

        mvc.perform(get("/api/projects/" + project.getId() + "/quotation-requests")
                        .header("Authorization", bearer(plannerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status").value(containsInAnyOrder("APPROVED", "CLOSED")));
    }

    private Long createRequest(Vendor vendor) throws Exception {
        String response = mvc.perform(post("/api/projects/" + project.getId() + "/quotation-requests")
                        .header("Authorization", bearer(plannerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(vendor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.functionId").value(function.getId()))
                .andExpect(jsonPath("$.vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$.service").value("Catering"))
                .andExpect(jsonPath("$.deadline").value(deadline.toString()))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).get("id").asLong();
    }

    private void submit(Long quotationId, User vendorUser, String amount, String description) throws Exception {
        mvc.perform(put("/api/quotation-requests/" + quotationId + "/submission")
                        .header("Authorization", bearer(vendorUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(amount, description)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quotationId))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());
    }

    private String createJson(Vendor vendor) {
        return "{\"vendorId\":" + vendor.getId()
                + ",\"functionId\":" + function.getId()
                + ",\"service\":\"Catering\",\"deadline\":\"" + deadline + "\"}";
    }

    private String submissionJson(String amount, String description) {
        return "{\"amount\":" + amount + ",\"description\":\"" + description + "\"}";
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private User newUser(String email, Role role) {
        return users.save(User.builder()
                .fullName(email)
                .email(email)
                .passwordHash("not-used")
                .role(role)
                .active(true)
                .build());
    }

    private Client newClient(User user) {
        return clients.save(Client.builder().user(user).build());
    }

    private Employee newEmployee(User user, String code) {
        return employees.save(Employee.builder().user(user).employeeCode(code).active(true).build());
    }

    private Vendor newVendor(String name, String email, User user) {
        Vendor vendor = new Vendor();
        vendor.setUser(user);
        vendor.setBusinessName(name);
        vendor.setCategory(VendorCategory.CATERING);
        vendor.setEmail(email);
        vendor.setPhone("+919876543210");
        vendor.setStatus(VendorStatus.APPROVED);
        return vendors.save(vendor);
    }
}
