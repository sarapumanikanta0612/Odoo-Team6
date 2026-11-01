package com.wams.enquiry;

import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.entity.WeddingProject;
import com.wams.enums.ProjectStatus;
import com.wams.enums.Role;
import com.wams.qualification.Agreement;
import com.wams.qualification.AgreementRepository;
import com.wams.qualification.AgreementStatus;
import com.wams.qualification.BookingDeposit;
import com.wams.qualification.BookingDepositRepository;
import com.wams.qualification.BookingDepositStatus;
import com.wams.qualification.PaymentMethod;
import com.wams.repository.ClientRepository;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.UserRepository;
import com.wams.repository.WeddingProjectRepository;
import com.wams.security.JwtService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:wams_client_association_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ClientAssociationIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired EnquiryRepository enquiryRepository;
    @Autowired EnquiryFollowUpRepository followUpRepository;
    @Autowired AgreementRepository agreementRepository;
    @Autowired BookingDepositRepository depositRepository;
    @Autowired WeddingProjectRepository projectRepository;
    @Autowired JwtService jwtService;

    private User admin;
    private Employee planner;

    @BeforeEach
    void cleanAndSeed() {
        projectRepository.deleteAll();
        agreementRepository.deleteAll();
        depositRepository.deleteAll();
        followUpRepository.deleteAll();
        enquiryRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();

        admin = newUser("admin@association.test", Role.ADMIN);
        planner = newEmployee(newUser("planner@association.test", Role.LEAD_PLANNER), "ASSOC-PLAN");
    }

    @Test
    @WithMockUser(username = "admin@association.test", roles = "ADMIN")
    void conversionMatchesExistingClientByTrimmedCaseInsensitiveEmail() throws Exception {
        User user = newUser("matched.client@example.com", Role.PRIMARY_CLIENT);
        Client client = newClient(user);
        Enquiry enquiry = qualifiedEnquiry("  MATCHED.CLIENT@EXAMPLE.COM  ");

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON).content(convertJson(null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.primaryClientId").value(client.getId()));

        Enquiry saved = enquiryRepository.findById(enquiry.getId()).orElseThrow();
        assertThat(saved.getPrimaryClient().getId()).isEqualTo(client.getId());
        assertThat(saved.getClientLinkSource()).isEqualTo(ClientLinkSource.AUTO_EMAIL);
    }

    @Test
    @WithMockUser(username = "admin@association.test", roles = "ADMIN")
    void conversionRecoversMissingClientProfile_withoutCreatingDuplicate() throws Exception {
        User user = newUser("missing.profile@example.com", Role.PRIMARY_CLIENT);
        Enquiry enquiry = qualifiedEnquiry("Missing.Profile@Example.com");

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON).content(convertJson(null)))
                .andExpect(status().isCreated());

        Client recovered = clientRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(projectRepository.findByEnquiryId(enquiry.getId()).orElseThrow()
                .getPrimaryClient().getId()).isEqualTo(recovered.getId());
        assertThat(clientRepository.findAll().stream().filter(c -> c.getUser().getId().equals(user.getId())))
                .hasSize(1);
    }

    @Test
    @WithMockUser(username = "admin@association.test", roles = "ADMIN")
    void conversionRejectsNoMatchingAccount_withoutCreatingAccount() throws Exception {
        Enquiry enquiry = qualifiedEnquiry("nobody@example.com");
        long usersBefore = userRepository.count();

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON).content(convertJson(null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("admin must link")));

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(projectRepository.count()).isZero();
        assertThat(enquiryRepository.findById(enquiry.getId()).orElseThrow().getPrimaryClient()).isNull();
    }

    @Test
    @WithMockUser(username = "admin@association.test", roles = "ADMIN")
    void conversionRejectsMatchingAccountWithInvalidRole() throws Exception {
        newUser("wrong.role@example.com", Role.VENDOR);
        Enquiry enquiry = qualifiedEnquiry("wrong.role@example.com");

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON).content(convertJson(null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("PRIMARY_CLIENT role")));
        assertThat(projectRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "admin@association.test", roles = "ADMIN")
    void adminCanExplicitlyLinkExistingClient_andSameRequestIsIdempotent() throws Exception {
        Client client = newClient(newUser("explicit.client@example.com", Role.PRIMARY_CLIENT));
        Enquiry enquiry = basicEnquiry("different.enquiry@example.com");
        String body = "{\"primaryClientId\":" + client.getId() + "}";

        mvc.perform(patch("/api/enquiries/" + enquiry.getId() + "/primary-client")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryClientId").value(client.getId()))
                .andExpect(jsonPath("$.primaryClientName").value("explicit.client@example.com"))
                .andExpect(jsonPath("$.clientLinkSource").value("ADMIN"));
        mvc.perform(patch("/api/enquiries/" + enquiry.getId() + "/primary-client")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryClientId").value(client.getId()));

        assertThat(enquiryRepository.findById(enquiry.getId()).orElseThrow().getPrimaryClient().getId())
                .isEqualTo(client.getId());
    }

    @Test
    @WithMockUser(username = "admin@association.test", roles = "ADMIN")
    void explicitLinkRejectsInvalidRole_andBlindRelinkConflict() throws Exception {
        Client original = newClient(newUser("original@example.com", Role.PRIMARY_CLIENT));
        Client replacement = newClient(newUser("replacement@example.com", Role.PRIMARY_CLIENT));
        Client invalid = newClient(newUser("partner@example.com", Role.PARTNER_CLIENT));
        Enquiry enquiry = basicEnquiry("manual@example.com");
        enquiry.setPrimaryClient(original);
        enquiryRepository.save(enquiry);

        mvc.perform(patch("/api/enquiries/" + enquiry.getId() + "/primary-client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryClientId\":" + replacement.getId() + "}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/enquiries/" + enquiry.getId() + "/primary-client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryClientId\":" + invalid.getId()
                                + ",\"expectedCurrentPrimaryClientId\":" + original.getId() + "}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(enquiryRepository.findById(enquiry.getId()).orElseThrow().getPrimaryClient().getId())
                .isEqualTo(original.getId());
    }

    @Test
    @WithMockUser(username = "planner@association.test", roles = "LEAD_PLANNER")
    void leadPlannerCannotUseAdminClientLinkEndpoint() throws Exception {
        Client client = newClient(newUser("denied@example.com", Role.PRIMARY_CLIENT));
        Enquiry enquiry = basicEnquiry("denied@example.com");
        enquiry.setAssignedLeadPlanner(planner);
        enquiryRepository.save(enquiry);

        mvc.perform(patch("/api/enquiries/" + enquiry.getId() + "/primary-client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryClientId\":" + client.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@association.test", roles = "ADMIN")
    void recoveredProfileRollsBackWhenLaterProjectAssociationFails() throws Exception {
        User matchingUser = newUser("rollback@example.com", Role.PRIMARY_CLIENT);
        Client invalidPartner = newClient(newUser("not-a-partner@example.com", Role.PRIMARY_CLIENT));
        Enquiry enquiry = qualifiedEnquiry("rollback@example.com");

        mvc.perform(post("/api/enquiries/" + enquiry.getId() + "/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convertJson(invalidPartner.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("PARTNER_CLIENT role")));

        assertThat(clientRepository.findByUserId(matchingUser.getId())).isEmpty();
        assertThat(enquiryRepository.findById(enquiry.getId()).orElseThrow().getPrimaryClient()).isNull();
        assertThat(projectRepository.count()).isZero();
    }

    @Test
    void convertedRelinkSynchronizesProjectVisibility_andPreservesPartnerAccess() throws Exception {
        Client oldPrimary = newClient(newUser("old@example.com", Role.PRIMARY_CLIENT));
        Client newPrimary = newClient(newUser("new@example.com", Role.PRIMARY_CLIENT));
        Client partner = newClient(newUser("partner-access@example.com", Role.PARTNER_CLIENT));
        Enquiry enquiry = basicEnquiry("legacy@example.com");
        enquiry.setPrimaryClient(oldPrimary);
        enquiry.setStatus(EnquiryStatus.CONVERTED);
        enquiryRepository.save(enquiry);
        WeddingProject project = projectRepository.save(WeddingProject.builder()
                .projectName("Relinked Wedding")
                .enquiry(enquiry)
                .primaryClient(oldPrimary)
                .partnerClient(partner)
                .leadPlanner(planner)
                .weddingDate(LocalDate.now().plusMonths(2))
                .projectStatus(ProjectStatus.PLANNING)
                .agreementSigned(true)
                .bookingDepositPaid(true)
                .build());

        mvc.perform(patch("/api/enquiries/" + enquiry.getId() + "/primary-client")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryClientId\":" + newPrimary.getId()
                                + ",\"expectedCurrentPrimaryClientId\":" + oldPrimary.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryClientId").value(newPrimary.getId()));

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getPrimaryClient().getId())
                .isEqualTo(newPrimary.getId());
        assertThat(projectRepository.findById(project.getId()).orElseThrow().getPartnerClient().getId())
                .isEqualTo(partner.getId());

        mvc.perform(get("/api/projects").header("Authorization", bearer(oldPrimary.getUser())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/projects").header("Authorization", bearer(newPrimary.getUser())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/projects").header("Authorization", bearer(partner.getUser())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    private Enquiry qualifiedEnquiry(String email) {
        Enquiry enquiry = basicEnquiry(email);
        agreementRepository.save(Agreement.builder()
                .enquiry(enquiry).status(AgreementStatus.SIGNED)
                .agreementReference("AGR-" + enquiry.getId())
                .signedByName("Client").signedAt(Instant.now().minusSeconds(60)).recordedBy(admin).build());
        depositRepository.save(BookingDeposit.builder()
                .enquiry(enquiry).status(BookingDepositStatus.PAID)
                .amount(new BigDecimal("50000.00")).paidOn(LocalDate.now())
                .paymentMethod(PaymentMethod.UPI).paymentReference("PAY-" + enquiry.getId())
                .recordedBy(admin).build());
        return enquiry;
    }

    private Enquiry basicEnquiry(String email) {
        Enquiry enquiry = new Enquiry();
        enquiry.setContactName("Association Client");
        enquiry.setEmail(email);
        enquiry.setPhone("+919876543210");
        enquiry.setWeddingStartDate(LocalDate.now().plusMonths(2));
        enquiry.setAssignedLeadPlanner(planner);
        enquiry.setStatus(EnquiryStatus.PROPOSAL);
        enquiry.setSource(EnquirySource.WEBSITE);
        return enquiryRepository.save(enquiry);
    }

    private String convertJson(Long partnerClientId) {
        String partner = partnerClientId == null ? "" : ",\"partnerClientId\":" + partnerClientId;
        return "{\"projectName\":\"Associated Wedding\",\"weddingDate\":\""
                + LocalDate.now().plusMonths(2) + "\",\"venue\":\"Chennai\"" + partner + "}";
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private User newUser(String email, Role role) {
        return userRepository.save(User.builder()
                .fullName(email).email(email).passwordHash("not-used").role(role).active(true).build());
    }

    private Client newClient(User user) {
        return clientRepository.save(Client.builder().user(user).build());
    }

    private Employee newEmployee(User user, String code) {
        return employeeRepository.save(Employee.builder().user(user).employeeCode(code).active(true).build());
    }
}
