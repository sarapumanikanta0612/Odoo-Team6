package com.wams.qualification;

import com.wams.enquiry.Enquiry;
import com.wams.enquiry.EnquiryFollowUpRepository;
import com.wams.enquiry.EnquiryRepository;
import com.wams.enquiry.EnquirySource;
import com.wams.enquiry.EnquiryStatus;
import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.entity.WeddingProject;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:wams_p2_conversion_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class EnquiryConversionIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AgreementRepository agreementRepository;
    @Autowired BookingDepositRepository depositRepository;
    @Autowired WeddingProjectRepository projectRepository;
    @Autowired EnquiryFollowUpRepository followUpRepository;
    @Autowired EnquiryRepository enquiryRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired UserRepository userRepository;

    private User admin;
    private Employee planner;
    private Employee otherPlanner;
    private Client client;
    private Enquiry enquiry;

    @BeforeEach
    void seed() {
        projectRepository.deleteAll();
        agreementRepository.deleteAll();
        depositRepository.deleteAll();
        followUpRepository.deleteAll();
        enquiryRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();

        admin = newUser("admin@conversion.test", Role.ADMIN);
        planner = newEmployee(newUser("planner@conversion.test", Role.LEAD_PLANNER), "CONV-PLAN");
        otherPlanner = newEmployee(newUser("other@conversion.test", Role.LEAD_PLANNER), "CONV-OTHER");
        client = clientRepository.save(Client.builder()
                .user(newUser("client@conversion.test", Role.PRIMARY_CLIENT)).build());

        enquiry = new Enquiry();
        enquiry.setContactName("Conversion Couple");
        enquiry.setEmail("client@conversion.test");
        enquiry.setPhone("+919876543210");
        enquiry.setWeddingStartDate(LocalDate.now().plusMonths(4));
        enquiry.setDestination("Madurai");
        enquiry.setAssignedLeadPlanner(planner);
        enquiry.setStatus(EnquiryStatus.PROPOSAL);
        enquiry.setSource(EnquirySource.WEBSITE);
        enquiry = enquiryRepository.save(enquiry);
    }

    @Test
    @WithMockUser(username = "admin@conversion.test", roles = "ADMIN")
    void agreementCanBeRecordedRetrievedSignedAndVoided_withTransitionValidation() throws Exception {
        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/agreement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreementReference\":\"agr-pending\",\"status\":\"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.agreementReference").value("AGR-PENDING"));

        mvc.perform(get("/api/enquiries/" + enquiry.getId() + "/agreement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordedByUserId").value(admin.getId()));

        String signedAt = Instant.now().minusSeconds(60).toString();
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/agreement/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agreementReference":"AGR-PENDING","signedByName":"Conversion Couple","signedAt":"%s"}
                                """.formatted(signedAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SIGNED"));

        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/agreement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreementReference\":\"AGR-PENDING\",\"status\":\"PENDING\"}"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/agreement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreementReference\":\"AGR-PENDING\",\"status\":\"VOIDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOIDED"));
    }

    @Test
    @WithMockUser(username = "admin@conversion.test", roles = "ADMIN")
    void depositCanBeRecordedRetrievedPaidAndRefunded_withoutDuplicateOrAmountDrift() throws Exception {
        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/booking-deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50000,\"status\":\"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(get("/api/enquiries/" + enquiry.getId() + "/booking-deposit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(50000));

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/booking-deposit/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paidDepositJson(50000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"));

        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/booking-deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":40000,\"status\":\"REFUNDED\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Booking deposit amount cannot change while refunding"));

        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/booking-deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50000,\"status\":\"REFUNDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.paymentReference").value("PAY-CONVERSION"));

        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/booking-deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50000,\"status\":\"PENDING\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(depositRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin@conversion.test", roles = "ADMIN")
    void conversionBlockedWhenAgreementUnsigned() throws Exception {
        pendingAgreement();
        paidDeposit();

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON).content(convertJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("agreementSigned=false")));
        assertThat(projectRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "admin@conversion.test", roles = "ADMIN")
    void conversionBlockedWhenDepositUnpaid() throws Exception {
        signedAgreement();
        pendingDeposit();

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON).content(convertJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("bookingDepositPaid=false")));
        assertThat(projectRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "admin@conversion.test", roles = "ADMIN")
    void explicitConversionSucceedsOnce_andPreservesCommercialAndDepositSnapshots() throws Exception {
        mvc.perform(patch("/api/enquiries/" + enquiry.getId() + "/commercial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedPackage":"Temple Luxe","packageDescription":"Planning and decor","agreedAmount":900000}
                                """))
                .andExpect(status().isOk());
        signedAgreement();
        paidDeposit();

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON).content(convertJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enquiryId").value(enquiry.getId()))
                .andExpect(jsonPath("$.leadPlannerId").value(planner.getId()))
                .andExpect(jsonPath("$.primaryClientId").value(client.getId()))
                .andExpect(jsonPath("$.selectedPackage").value("Temple Luxe"))
                .andExpect(jsonPath("$.agreedAmount").value(900000))
                .andExpect(jsonPath("$.bookingDepositAmount").value(50000))
                .andExpect(jsonPath("$.bookingDepositReference").value("PAY-CONVERSION"));

        WeddingProject project = projectRepository.findByEnquiryId(enquiry.getId()).orElseThrow();
        assertThat(project.getAgreedAmount()).isEqualByComparingTo("900000");
        assertThat(project.getBookingDepositAmount()).isEqualByComparingTo("50000");
        assertThat(project.getLeadPlanner().getId()).isEqualTo(planner.getId());
        assertThat(enquiryRepository.findById(enquiry.getId()).orElseThrow().getStatus())
                .isEqualTo(EnquiryStatus.CONVERTED);

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON).content(convertJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("already converted")));
        assertThat(projectRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "other@conversion.test", roles = "LEAD_PLANNER")
    void otherPlannerCannotReadQualificationOrConvertAssignedEnquiry() throws Exception {
        mvc.perform(get("/api/enquiries/" + enquiry.getId() + "/agreement"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON).content(convertJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "client@conversion.test", roles = "PRIMARY_CLIENT")
    void clientCannotAccessQualificationActions() throws Exception {
        mvc.perform(get("/api/enquiries/" + enquiry.getId() + "/agreement"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/booking-deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50000,\"status\":\"PENDING\"}"))
                .andExpect(status().isForbidden());
    }

    private void pendingAgreement() throws Exception {
        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/agreement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreementReference\":\"AGR-PENDING-GATE\",\"status\":\"PENDING\"}"))
                .andExpect(status().isOk());
    }

    private void signedAgreement() throws Exception {
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/agreement/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agreementReference":"AGR-CONVERSION","signedByName":"Conversion Couple","signedAt":"%s"}
                                """.formatted(Instant.now().minusSeconds(60))))
                .andExpect(status().isCreated());
    }

    private void pendingDeposit() throws Exception {
        mvc.perform(put("/api/enquiries/" + enquiry.getId() + "/booking-deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50000,\"status\":\"PENDING\"}"))
                .andExpect(status().isOk());
    }

    private void paidDeposit() throws Exception {
        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/booking-deposit/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paidDepositJson(50000)))
                .andExpect(status().isCreated());
    }

    private String paidDepositJson(int amount) {
        return """
                {"amount":%d,"paidOn":"%s","paymentMethod":"UPI","paymentReference":"PAY-CONVERSION"}
                """.formatted(amount, LocalDate.now());
    }

    private String convertJson() {
        return """
                {
                  "projectName":"P2 Converted Wedding",
                  "weddingDate":"%s",
                  "venue":"Madurai"
                }
                """.formatted(LocalDate.now().plusMonths(4));
    }

    private User newUser(String email, Role role) {
        return userRepository.save(User.builder()
                .fullName(email).email(email).passwordHash("not-used").role(role).active(true).build());
    }

    private Employee newEmployee(User user, String code) {
        return employeeRepository.save(Employee.builder().user(user).employeeCode(code).active(true).build());
    }
}
