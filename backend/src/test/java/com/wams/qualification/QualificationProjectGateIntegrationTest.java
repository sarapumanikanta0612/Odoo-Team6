package com.wams.qualification;

import com.wams.enquiry.Enquiry;
import com.wams.enquiry.EnquiryRepository;
import com.wams.enquiry.EnquirySource;
import com.wams.enquiry.EnquiryStatus;
import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.User;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:wams_qualification_gate_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class QualificationProjectGateIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AgreementRepository agreementRepository;
    @Autowired BookingDepositRepository bookingDepositRepository;
    @Autowired WeddingProjectRepository projectRepository;
    @Autowired EnquiryRepository enquiryRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired UserRepository userRepository;

    private User admin;
    private Client primaryClient;
    private Employee leadPlanner;
    private Enquiry enquiry;

    @BeforeEach
    void seed() {
        projectRepository.deleteAll();
        agreementRepository.deleteAll();
        bookingDepositRepository.deleteAll();
        enquiryRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();

        admin = newUser("admin@qualification.test", Role.ADMIN);
        User plannerUser = newUser("planner@qualification.test", Role.LEAD_PLANNER);
        leadPlanner = employeeRepository.save(Employee.builder()
                .user(plannerUser)
                .employeeCode("QUAL-PLANNER")
                .active(true)
                .build());
        User clientUser = newUser("client@qualification.test", Role.PRIMARY_CLIENT);
        primaryClient = clientRepository.save(Client.builder().user(clientUser).build());

        enquiry = new Enquiry();
        enquiry.setContactName("Qualified Couple");
        enquiry.setEmail("couple@example.com");
        enquiry.setPhone("+919876543210");
        enquiry.setWeddingStartDate(LocalDate.now().plusMonths(3));
        enquiry.setStatus(EnquiryStatus.NEW);
        enquiry.setSource(EnquirySource.WEBSITE);
        enquiry.setPrimaryClient(primaryClient);
        enquiry = enquiryRepository.save(enquiry);
    }

    @Test
    @WithMockUser(username = "admin@qualification.test", roles = "ADMIN")
    void legacyTrueBooleans_withoutPersistedRecords_cannotCreateProject() throws Exception {
        mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson(true)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("persisted signed agreement")));

        assertThat(projectRepository.count()).isZero();
        assertThat(enquiryRepository.findById(enquiry.getId()).orElseThrow().getStatus())
                .isEqualTo(EnquiryStatus.NEW);
    }

    @Test
    @WithMockUser(username = "admin@qualification.test", roles = "ADMIN")
    void signedAgreementAndPaidDeposit_createProjectAndConvertEnquiry_atomically() throws Exception {
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/agreement/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreementReference": "agr-qual-001",
                                  "signedByName": "Qualified Couple",
                                  "signedAt": "%s"
                                }
                                """.formatted(Instant.now().minusSeconds(60))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SIGNED"))
                .andExpect(jsonPath("$.recordedByUserId").value(admin.getId()));

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/booking-deposit/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 50000.00,
                                  "paidOn": "%s",
                                  "paymentMethod": "UPI",
                                  "paymentReference": "upi-qual-001"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.recordedByUserId").value(admin.getId()));

        mvc.perform(get("/api/enquiries/" + enquiry.getId() + "/conversion-readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.agreementStatus").value("SIGNED"))
                .andExpect(jsonPath("$.bookingDepositStatus").value("PAID"));

        mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson(false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enquiryId").value(enquiry.getId()))
                .andExpect(jsonPath("$.agreementSigned").value(true))
                .andExpect(jsonPath("$.bookingDepositPaid").value(true));

        assertThat(enquiryRepository.findById(enquiry.getId()).orElseThrow().getStatus())
                .isEqualTo(EnquiryStatus.CONVERTED);
        assertThat(projectRepository.findByEnquiryId(enquiry.getId()))
                .get()
                .satisfies(project -> assertThat(project.getEnquiry().getId()).isEqualTo(enquiry.getId()));

        mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson(false)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("already converted")));
        assertThat(projectRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin@qualification.test", roles = "ADMIN")
    void duplicateAgreementAndDepositForSameEnquiry_areRejected() throws Exception {
        String agreementBody = """
                {
                  "agreementReference": "AGR-DUPLICATE",
                  "signedByName": "Qualified Couple",
                  "signedAt": "%s"
                }
                """.formatted(Instant.now().minusSeconds(60));
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/agreement/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agreementBody))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/agreement/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agreementBody))
                .andExpect(status().isConflict());

        String depositBody = """
                {
                  "amount": 10000,
                  "paidOn": "%s",
                  "paymentMethod": "BANK_TRANSFER",
                  "paymentReference": "PAY-DUPLICATE"
                }
                """.formatted(LocalDate.now());
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/booking-deposit/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/booking-deposit/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody))
                .andExpect(status().isConflict());

        assertThat(agreementRepository.count()).isEqualTo(1);
        assertThat(bookingDepositRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "employee@qualification.test", roles = "EMPLOYEE")
    void employeeCannotRecordQualification() throws Exception {
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/agreement/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreementReference": "AGR-DENIED",
                                  "signedByName": "Nobody",
                                  "signedAt": "%s"
                                }
                                """.formatted(Instant.now().minusSeconds(60))))
                .andExpect(status().isForbidden());
        assertThat(agreementRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "admin@qualification.test", roles = "ADMIN")
    void depositValidation_rejectsZeroAmountAndFutureDate() throws Exception {
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/booking-deposit/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 0,
                                  "paidOn": "%s",
                                  "paymentMethod": "CASH",
                                  "paymentReference": "INVALID"
                                }
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
        assertThat(bookingDepositRepository.count()).isZero();
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

    private String projectJson(boolean includeLegacyBooleans) {
        String legacyFields = includeLegacyBooleans
                ? ",\"agreementSigned\":true,\"bookingDepositPaid\":true"
                : "";
        return """
                {
                  "projectName": "Qualified Wedding",
                  "primaryClientId": %d,
                  "leadPlannerId": %d,
                  "weddingDate": "%s",
                  "venue": "Chennai",
                  "enquiryId": %d%s
                }
                """.formatted(
                primaryClient.getId(),
                leadPlanner.getId(),
                LocalDate.now().plusMonths(3),
                enquiry.getId(),
                legacyFields);
    }
}
