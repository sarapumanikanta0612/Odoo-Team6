package com.wams.enquiry;

import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.qualification.Agreement;
import com.wams.qualification.AgreementRepository;
import com.wams.qualification.AgreementStatus;
import com.wams.qualification.BookingDepositRepository;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.UserRepository;
import com.wams.repository.WeddingProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:wams_internal_enquiry_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class InternalEnquiryIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired EnquiryRepository enquiryRepository;
    @Autowired EnquiryFollowUpRepository followUpRepository;
    @Autowired AgreementRepository agreementRepository;
    @Autowired BookingDepositRepository depositRepository;
    @Autowired WeddingProjectRepository projectRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired UserRepository userRepository;

    private User admin;
    private Employee plannerA;
    private Employee plannerB;
    private Enquiry enquiryA;
    private Enquiry enquiryB;
    private Enquiry unassigned;

    @BeforeEach
    void seed() {
        projectRepository.deleteAll();
        agreementRepository.deleteAll();
        depositRepository.deleteAll();
        followUpRepository.deleteAll();
        enquiryRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();

        admin = newUser("admin@enquiry.test", Role.ADMIN);
        plannerA = newEmployee(newUser("planner-a@enquiry.test", Role.LEAD_PLANNER), "ENQ-PLAN-A");
        plannerB = newEmployee(newUser("planner-b@enquiry.test", Role.LEAD_PLANNER), "ENQ-PLAN-B");
        newUser("client@enquiry.test", Role.PRIMARY_CLIENT);

        enquiryA = newEnquiry("Alpha Couple", "alpha@example.com", EnquiryStatus.NEW, plannerA);
        enquiryB = newEnquiry("Beta Couple", "beta@example.com", EnquiryStatus.CONTACTED, plannerB);
        unassigned = newEnquiry("Gamma Couple", "gamma@example.com", EnquiryStatus.NEW, null);
    }

    @Test
    @WithMockUser(username = "admin@enquiry.test", roles = "ADMIN")
    void adminListsAll_withPaginationSearchAndStatusFilter() throws Exception {
        mvc.perform(get("/api/enquiries").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3));

        mvc.perform(get("/api/enquiries").param("search", "beta").param("status", "CONTACTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(enquiryB.getId()));
    }

    @Test
    @WithMockUser(username = "planner-a@enquiry.test", roles = "LEAD_PLANNER")
    void leadPlannerListsAndReadsOnlyAssignedEnquiries() throws Exception {
        mvc.perform(get("/api/enquiries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(enquiryA.getId()));

        mvc.perform(get("/api/enquiries/" + enquiryA.getId()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/enquiries/" + enquiryB.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/enquiries/" + unassigned.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@enquiry.test", roles = "ADMIN")
    void adminCanAssignActiveLeadPlanner() throws Exception {
        mvc.perform(patch("/api/enquiries/" + unassigned.getId() + "/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedLeadPlannerId\":" + plannerB.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedLeadPlannerId").value(plannerB.getId()))
                .andExpect(jsonPath("$.assignedLeadPlannerName").value("planner-b@enquiry.test"));

        assertThat(enquiryRepository.findById(unassigned.getId()).orElseThrow()
                .getAssignedLeadPlanner().getId()).isEqualTo(plannerB.getId());
    }

    @Test
    @WithMockUser(username = "planner-a@enquiry.test", roles = "LEAD_PLANNER")
    void leadPlannerCannotAssignEnquiry() throws Exception {
        mvc.perform(patch("/api/enquiries/" + enquiryA.getId() + "/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedLeadPlannerId\":" + plannerB.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "planner-a@enquiry.test", roles = "LEAD_PLANNER")
    void statusTransitionsAreForwardOnly_andConvertedCannotBeSetManually() throws Exception {
        mvc.perform(patch("/api/enquiries/" + enquiryA.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"));

        mvc.perform(patch("/api/enquiries/" + enquiryA.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NEW\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(patch("/api/enquiries/" + enquiryA.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONVERTED\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = "planner-a@enquiry.test", roles = "LEAD_PLANNER")
    void assignedPlannerCanRecordAndRetrieveFollowUp() throws Exception {
        mvc.perform(post("/api/enquiries/" + enquiryA.getId() + "/follow-ups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "occurredAt": "%s",
                                  "notes": "Discussed the ceremony requirements",
                                  "nextFollowUpDate": "%s"
                                }
                                """.formatted(Instant.now().minusSeconds(60), LocalDate.now().plusDays(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordedByName").value("planner-a@enquiry.test"));

        mvc.perform(get("/api/enquiries/" + enquiryA.getId() + "/follow-ups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].notes").value("Discussed the ceremony requirements"));
    }

    @Test
    @WithMockUser(username = "planner-a@enquiry.test", roles = "LEAD_PLANNER")
    void commercialTermsPersist_andFreezeAfterAgreementSigned() throws Exception {
        mvc.perform(patch("/api/enquiries/" + enquiryA.getId() + "/commercial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedPackage":"Temple Luxe","packageDescription":"Planning and decor","agreedAmount":750000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedPackage").value("Temple Luxe"))
                .andExpect(jsonPath("$.agreedAmount").value(750000));

        agreementRepository.save(Agreement.builder()
                .enquiry(enquiryA)
                .status(AgreementStatus.SIGNED)
                .agreementReference("ENQ-SIGNED")
                .signedByName("Alpha Couple")
                .signedAt(Instant.now().minusSeconds(60))
                .recordedBy(admin)
                .build());

        mvc.perform(patch("/api/enquiries/" + enquiryA.getId() + "/commercial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedPackage":"Changed","agreedAmount":1}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Commercial terms cannot change after the agreement is signed"));
    }

    @Test
    @WithMockUser(username = "client@enquiry.test", roles = "PRIMARY_CLIENT")
    void clientCannotAccessAnyInternalEnquiryEndpoint() throws Exception {
        mvc.perform(get("/api/enquiries")).andExpect(status().isForbidden());
        mvc.perform(get("/api/enquiries/" + enquiryA.getId())).andExpect(status().isForbidden());
        mvc.perform(get("/api/enquiries/" + enquiryA.getId() + "/follow-ups")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotAccessInternalEnquiries() throws Exception {
        mvc.perform(get("/api/enquiries")).andExpect(status().isUnauthorized());
    }

    private User newUser(String email, Role role) {
        return userRepository.save(User.builder()
                .fullName(email)
                .email(email)
                .passwordHash("not-used")
                .role(role)
                .active(true)
                .build());
    }

    private Employee newEmployee(User user, String code) {
        return employeeRepository.save(Employee.builder()
                .user(user).employeeCode(code).active(true).build());
    }

    private Enquiry newEnquiry(String name, String email, EnquiryStatus status, Employee planner) {
        Enquiry enquiry = new Enquiry();
        enquiry.setContactName(name);
        enquiry.setEmail(email);
        enquiry.setPhone("+919876543210");
        enquiry.setWeddingStartDate(LocalDate.now().plusMonths(3));
        enquiry.setDestination("Chennai");
        enquiry.setStatus(status);
        enquiry.setSource(EnquirySource.WEBSITE);
        enquiry.setAssignedLeadPlanner(planner);
        return enquiryRepository.save(enquiry);
    }
}
