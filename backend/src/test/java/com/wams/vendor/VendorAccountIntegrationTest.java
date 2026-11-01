package com.wams.vendor;

import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:wams_vendor_account_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class VendorAccountIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired VendorRepository vendorRepository;

    @BeforeEach
    void clean() {
        vendorRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreate_canLinkVendorUser_andReturnsUserId() throws Exception {
        User vendorUser = newUser("linked-vendor@wams.test", Role.VENDOR);

        mvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendorJson("Linked Caterers", "linked-caterers@wams.test", vendorUser.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(vendorUser.getId()))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(vendorRepository.findByUserId(vendorUser.getId())).isPresent();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreate_rejectsLinkToNonVendorUser() throws Exception {
        User employee = newUser("employee@wams.test", Role.EMPLOYEE);

        mvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendorJson("Wrong Link", "wrong-link@wams.test", employee.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Linked user must have VENDOR role"));

        assertThat(vendorRepository.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreate_rejectsVendorUserAlreadyLinkedElsewhere() throws Exception {
        User vendorUser = newUser("one-account@wams.test", Role.VENDOR);
        newVendor("First Vendor", "first@wams.test", vendorUser);

        mvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendorJson("Second Vendor", "second@wams.test", vendorUser.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vendor user is already linked to another vendor"));

        assertThat(vendorRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "mine@wams.test", roles = "VENDOR")
    void vendorGetMine_returnsOnlyLinkedProfile() throws Exception {
        User mine = newUser("mine@wams.test", Role.VENDOR);
        User other = newUser("other@wams.test", Role.VENDOR);
        Vendor myVendor = newVendor("My Caterers", "mine-contact@wams.test", mine);
        Vendor otherVendor = newVendor("Other Decor", "other-contact@wams.test", other);

        mvc.perform(get("/api/vendors/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(myVendor.getId()))
                .andExpect(jsonPath("$.userId").value(mine.getId()))
                .andExpect(jsonPath("$.businessName").value("My Caterers"));

        mvc.perform(get("/api/vendors/" + otherVendor.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "unlinked@wams.test", roles = "VENDOR")
    void vendorGetMine_withoutLinkedProfile_returns404() throws Exception {
        newUser("unlinked@wams.test", Role.VENDOR);

        mvc.perform(get("/api/vendors/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vendor profile not found for current account"));
    }

    @Test
    @WithMockUser(username = "admin@wams.test", roles = "ADMIN")
    void adminCannotUseVendorSelfEndpoint() throws Exception {
        newUser("admin@wams.test", Role.ADMIN);

        mvc.perform(get("/api/vendors/me"))
                .andExpect(status().isForbidden());
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

    private Vendor newVendor(String name, String email, User user) {
        Vendor vendor = new Vendor();
        vendor.setUser(user);
        vendor.setBusinessName(name);
        vendor.setCategory(VendorCategory.CATERING);
        vendor.setEmail(email);
        vendor.setPhone("+919876543210");
        vendor.setStatus(VendorStatus.APPROVED);
        return vendorRepository.save(vendor);
    }

    private String vendorJson(String name, String email, Long userId) {
        return """
                {
                  "businessName": "%s",
                  "category": "CATERING",
                  "contactPerson": "Contact",
                  "email": "%s",
                  "phone": "+919876543210",
                  "city": "Chennai",
                  "serviceDescription": "Catering",
                  "userId": %d
                }
                """.formatted(name, email, userId);
    }
}
