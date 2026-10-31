package com.wams.service.impl;

import com.wams.dto.request.CreateProjectRequest;
import com.wams.dto.response.ProjectResponse;
import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.entity.WeddingProject;
import com.wams.enums.ProjectStatus;
import com.wams.enums.Role;
import com.wams.exception.BusinessRuleException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.repository.ClientRepository;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.WeddingProjectRepository;
import com.wams.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the booking-gate truth table defined in the SRS:
 *
 *   signed | paid | result
 *   -------+------+----------
 *   true   | true | ALLOWED
 *   true   | false| REJECTED
 *   false  | true | REJECTED
 *   false  | false| REJECTED
 */
@ExtendWith(MockitoExtension.class)
class WeddingProjectServiceImplTest {

    @Mock WeddingProjectRepository projectRepository;
    @Mock ClientRepository clientRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock AuthenticatedUserProvider authenticatedUserProvider;
    @InjectMocks WeddingProjectServiceImpl service;

    private Client primaryClient;
    private Employee leadPlanner;

    @BeforeEach
    void init() {
        User clientUser = User.builder().id(10L).role(Role.PRIMARY_CLIENT).active(true).build();
        primaryClient = Client.builder().id(100L).user(clientUser).build();

        User plannerUser = User.builder().id(20L).role(Role.LEAD_PLANNER).active(true).build();
        leadPlanner = Employee.builder().id(200L).user(plannerUser).active(true).build();
    }

    // ---------- GATE TRUTH TABLE ----------

    @Test
    void create_allowed_whenAgreementSigned_andDepositPaid() {
        when(clientRepository.findById(100L)).thenReturn(Optional.of(primaryClient));
        when(employeeRepository.findById(200L)).thenReturn(Optional.of(leadPlanner));
        when(projectRepository.save(any(WeddingProject.class))).thenAnswer(inv -> {
            WeddingProject p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProjectResponse resp = service.create(req(true, true));

        assertThat(resp.getId()).isEqualTo(1L);
        ArgumentCaptor<WeddingProject> captor = ArgumentCaptor.forClass(WeddingProject.class);
        verify(projectRepository).save(captor.capture());
        WeddingProject saved = captor.getValue();
        assertThat(saved.isAgreementSigned()).isTrue();
        assertThat(saved.isBookingDepositPaid()).isTrue();
        assertThat(saved.getProjectStatus()).isEqualTo(ProjectStatus.PLANNING);
    }

    @Test
    void create_rejected_whenAgreementSignedButDepositUnpaid() {
        assertThatThrownBy(() -> service.create(req(true, false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bookingDepositPaid=false");
        verify(projectRepository, never()).save(any());
        verify(clientRepository, never()).findById(any());
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    void create_rejected_whenAgreementUnsignedButDepositPaid() {
        assertThatThrownBy(() -> service.create(req(false, true)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("agreementSigned=false");
        verify(projectRepository, never()).save(any());
    }

    @Test
    void create_rejected_whenBothFalse() {
        assertThatThrownBy(() -> service.create(req(false, false)))
                .isInstanceOf(BusinessRuleException.class);
        verify(projectRepository, never()).save(any());
    }

    // ---------- ASSOCIATION VALIDATION ----------

    @Test
    void create_rejected_whenPrimaryClientMissing() {
        when(clientRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(req(true, true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Primary client");
    }

    @Test
    void create_rejected_whenLeadPlannerMissing() {
        when(clientRepository.findById(100L)).thenReturn(Optional.of(primaryClient));
        when(employeeRepository.findById(200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req(true, true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lead planner");
    }

    @Test
    void create_rejected_whenPrimaryClientUserHasWrongRole() {
        primaryClient.getUser().setRole(Role.VENDOR); // not PRIMARY_CLIENT
        when(clientRepository.findById(100L)).thenReturn(Optional.of(primaryClient));

        assertThatThrownBy(() -> service.create(req(true, true)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PRIMARY_CLIENT role");
    }

    @Test
    void create_rejected_whenLeadPlannerUserHasWrongRole() {
        leadPlanner.getUser().setRole(Role.EMPLOYEE); // not LEAD_PLANNER/ADMIN
        when(clientRepository.findById(100L)).thenReturn(Optional.of(primaryClient));
        when(employeeRepository.findById(200L)).thenReturn(Optional.of(leadPlanner));

        assertThatThrownBy(() -> service.create(req(true, true)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("LEAD_PLANNER");
    }

    @Test
    void create_rejected_whenLeadPlannerInactive() {
        leadPlanner.setActive(false);
        when(clientRepository.findById(100L)).thenReturn(Optional.of(primaryClient));
        when(employeeRepository.findById(200L)).thenReturn(Optional.of(leadPlanner));

        assertThatThrownBy(() -> service.create(req(true, true)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
    }

    // ---------- READ SCOPE: list() ----------
    //
    // The service resolves identity through AuthenticatedUserProvider, so every
    // test here stubs that provider to return a User with the desired role.
    // Each test also verifies that the correct repository finder was called —
    // or, for denied roles, that no finder was called at all.

    @Test
    void list_admin_returnsAllProjects_viaFindAll() {
        User adminUser = User.builder().id(1L).role(Role.ADMIN).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(adminUser);
        when(projectRepository.findAll()).thenReturn(List.of(sampleProject(500L)));

        List<ProjectResponse> out = service.list();

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getId()).isEqualTo(500L);
        verify(projectRepository).findAll();
        verify(projectRepository, never()).findAllByLeadPlannerId(any());
        verify(projectRepository, never()).findAllByPrimaryClientId(any());
        verify(projectRepository, never()).findAllByPartnerClientId(any());
    }

    @Test
    void list_leadPlanner_returnsOnlyProjectsAssignedToTheirEmployeeId() {
        User plannerUser = User.builder().id(20L).role(Role.LEAD_PLANNER).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        when(employeeRepository.findByUserId(20L)).thenReturn(Optional.of(leadPlanner));
        when(projectRepository.findAllByLeadPlannerId(200L))
                .thenReturn(List.of(sampleProject(501L)));

        List<ProjectResponse> out = service.list();

        assertThat(out).hasSize(1);
        verify(projectRepository).findAllByLeadPlannerId(200L);
        verify(projectRepository, never()).findAll();
    }

    @Test
    void list_leadPlanner_whenNoEmployeeProfile_returns403() {
        User plannerUser = User.builder().id(20L).role(Role.LEAD_PLANNER).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        when(employeeRepository.findByUserId(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list()).isInstanceOf(AccessDeniedException.class);
        verify(projectRepository, never()).findAll();
    }

    @Test
    void list_primaryClient_returnsOnlyTheirOwnProjects() {
        User clientUser = User.builder().id(10L).role(Role.PRIMARY_CLIENT).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(clientUser);
        when(clientRepository.findByUserId(10L)).thenReturn(Optional.of(primaryClient));
        when(projectRepository.findAllByPrimaryClientId(100L))
                .thenReturn(List.of(sampleProject(502L)));

        List<ProjectResponse> out = service.list();

        assertThat(out).hasSize(1);
        verify(projectRepository).findAllByPrimaryClientId(100L);
        verify(projectRepository, never()).findAllByPartnerClientId(any());
    }

    @Test
    void list_partnerClient_usesPartnerFinder() {
        Client partnerClient = Client.builder().id(150L)
                .user(User.builder().id(15L).role(Role.PARTNER_CLIENT).active(true).build())
                .build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(partnerClient.getUser());
        when(clientRepository.findByUserId(15L)).thenReturn(Optional.of(partnerClient));
        when(projectRepository.findAllByPartnerClientId(150L))
                .thenReturn(List.of(sampleProject(503L)));

        List<ProjectResponse> out = service.list();

        assertThat(out).hasSize(1);
        verify(projectRepository).findAllByPartnerClientId(150L);
        verify(projectRepository, never()).findAllByPrimaryClientId(any());
    }

    @Test
    void list_employee_denied_returns403_withoutTouchingProjectRepo() {
        User employeeUser = User.builder().id(30L).role(Role.EMPLOYEE).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(employeeUser);

        assertThatThrownBy(() -> service.list()).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(projectRepository);
    }

    @Test
    void list_vendor_denied_returns403_withoutTouchingProjectRepo() {
        User vendorUser = User.builder().id(40L).role(Role.VENDOR).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(vendorUser);

        assertThatThrownBy(() -> service.list()).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(projectRepository);
    }

    // ---------- READ SCOPE: getById() ----------
    //
    // Out-of-scope projects must return 404 (ResourceNotFoundException), not 403,
    // so a caller cannot probe existence by scanning ids.

    @Test
    void getById_admin_returnsAnyProject() {
        User adminUser = User.builder().id(1L).role(Role.ADMIN).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(adminUser);
        when(projectRepository.findById(999L)).thenReturn(Optional.of(sampleProject(999L)));

        ProjectResponse out = service.getById(999L);

        assertThat(out.getId()).isEqualTo(999L);
    }

    @Test
    void getById_leadPlanner_ownProject_returnsIt() {
        User plannerUser = leadPlanner.getUser();
        WeddingProject owned = sampleProject(600L); // leadPlanner is set in sampleProject
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        when(projectRepository.findById(600L)).thenReturn(Optional.of(owned));
        when(employeeRepository.findByUserId(plannerUser.getId())).thenReturn(Optional.of(leadPlanner));

        ProjectResponse out = service.getById(600L);

        assertThat(out.getId()).isEqualTo(600L);
    }

    @Test
    void getById_leadPlanner_othersProject_returns404_notLeakingExistence() {
        User plannerUser = leadPlanner.getUser();
        Employee someoneElse = Employee.builder().id(999L)
                .user(User.builder().id(998L).role(Role.LEAD_PLANNER).active(true).build())
                .active(true).build();
        WeddingProject notOwned = sampleProjectWithPlanner(601L, someoneElse);
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        when(projectRepository.findById(601L)).thenReturn(Optional.of(notOwned));
        when(employeeRepository.findByUserId(plannerUser.getId())).thenReturn(Optional.of(leadPlanner));

        assertThatThrownBy(() -> service.getById(601L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_primaryClient_ownProject_returnsIt() {
        User clientUser = primaryClient.getUser();
        WeddingProject owned = sampleProject(602L);
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(clientUser);
        when(projectRepository.findById(602L)).thenReturn(Optional.of(owned));
        when(clientRepository.findByUserId(clientUser.getId())).thenReturn(Optional.of(primaryClient));

        ProjectResponse out = service.getById(602L);
        assertThat(out.getId()).isEqualTo(602L);
    }

    @Test
    void getById_primaryClient_othersProject_returns404() {
        User clientUser = primaryClient.getUser();
        Client otherClient = Client.builder().id(777L)
                .user(User.builder().id(776L).role(Role.PRIMARY_CLIENT).active(true).build()).build();
        WeddingProject notOwned = sampleProjectWithPrimary(603L, otherClient);
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(clientUser);
        when(projectRepository.findById(603L)).thenReturn(Optional.of(notOwned));
        when(clientRepository.findByUserId(clientUser.getId())).thenReturn(Optional.of(primaryClient));

        assertThatThrownBy(() -> service.getById(603L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_partnerClient_whenPartnerFieldIsNull_returns404() {
        Client partnerClient = Client.builder().id(150L)
                .user(User.builder().id(15L).role(Role.PARTNER_CLIENT).active(true).build()).build();
        WeddingProject noPartner = sampleProject(604L); // partnerClient = null
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(partnerClient.getUser());
        when(projectRepository.findById(604L)).thenReturn(Optional.of(noPartner));
        when(clientRepository.findByUserId(15L)).thenReturn(Optional.of(partnerClient));

        assertThatThrownBy(() -> service.getById(604L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_vendor_denied_returns403() {
        User vendorUser = User.builder().id(40L).role(Role.VENDOR).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(vendorUser);

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void getById_employee_denied_returns403() {
        User employeeUser = User.builder().id(30L).role(Role.EMPLOYEE).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(employeeUser);

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(projectRepository, never()).findById(any());
    }

    // ---------- UPDATE SCOPE ----------
    //
    // Same ownership rules as getById. ADMIN unrestricted; LEAD_PLANNER scoped
    // to their own projects with 404 on cross-planner attempts (anti-enumeration);
    // every other role denied with 403.

    @Test
    void update_admin_anyProject_succeeds() {
        User adminUser = User.builder().id(1L).role(Role.ADMIN).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(adminUser);
        WeddingProject existing = sampleProject(700L);
        when(projectRepository.findById(700L)).thenReturn(Optional.of(existing));

        ProjectResponse resp = service.update(700L,
                new com.wams.dto.request.UpdateProjectRequest(
                        "Renamed", null, "New Venue", null, null, null));

        assertThat(resp.getProjectName()).isEqualTo("Renamed");
        assertThat(resp.getVenue()).isEqualTo("New Venue");
        // No employee/client lookup needed for the ADMIN path.
        verify(employeeRepository, never()).findByUserId(any());
        verify(clientRepository, never()).findByUserId(any());
    }

    @Test
    void update_admin_canReassignProjectToDifferentLeadPlanner() {
        User adminUser = User.builder().id(1L).role(Role.ADMIN).active(true).build();
        Employee replacement = Employee.builder().id(300L)
                .user(User.builder().id(30L).role(Role.LEAD_PLANNER).active(true).build())
                .active(true)
                .build();
        WeddingProject existing = sampleProject(705L);
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(adminUser);
        when(projectRepository.findById(705L)).thenReturn(Optional.of(existing));
        when(employeeRepository.findById(300L)).thenReturn(Optional.of(replacement));

        ProjectResponse response = service.update(705L,
                new com.wams.dto.request.UpdateProjectRequest(
                        null, null, null, null, null, 300L));

        assertThat(response.getLeadPlannerId()).isEqualTo(300L);
        assertThat(existing.getLeadPlanner()).isSameAs(replacement);
        verify(employeeRepository).findById(300L);
    }

    @Test
    void update_leadPlanner_differentLeadPlannerId_returns403_beforeAnyMutation() {
        User plannerUser = leadPlanner.getUser();
        WeddingProject owned = sampleProject(706L);
        owned.setVenue("Original Venue");
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        when(projectRepository.findById(706L)).thenReturn(Optional.of(owned));
        when(employeeRepository.findByUserId(plannerUser.getId())).thenReturn(Optional.of(leadPlanner));

        assertThatThrownBy(() -> service.update(706L,
                new com.wams.dto.request.UpdateProjectRequest(
                        null, null, "Must Not Apply", null, null, 300L)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only ADMIN may reassign a wedding project");

        assertThat(owned.getLeadPlanner()).isSameAs(leadPlanner);
        assertThat(owned.getVenue()).isEqualTo("Original Venue");
        verify(employeeRepository, never()).findById(300L);
    }

    @Test
    void update_leadPlanner_sameLeadPlannerId_isAllowedAsNoOp() {
        User plannerUser = leadPlanner.getUser();
        WeddingProject owned = sampleProject(707L);
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        when(projectRepository.findById(707L)).thenReturn(Optional.of(owned));
        when(employeeRepository.findByUserId(plannerUser.getId())).thenReturn(Optional.of(leadPlanner));
        when(employeeRepository.findById(leadPlanner.getId())).thenReturn(Optional.of(leadPlanner));

        ProjectResponse response = service.update(707L,
                new com.wams.dto.request.UpdateProjectRequest(
                        null, null, "Planner Updated Venue", null, null, leadPlanner.getId()));

        assertThat(response.getLeadPlannerId()).isEqualTo(leadPlanner.getId());
        assertThat(response.getVenue()).isEqualTo("Planner Updated Venue");
    }

    @Test
    void update_leadPlanner_ownProject_succeeds() {
        User plannerUser = leadPlanner.getUser();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        WeddingProject owned = sampleProject(701L); // leadPlanner is already set
        when(projectRepository.findById(701L)).thenReturn(Optional.of(owned));
        when(employeeRepository.findByUserId(plannerUser.getId())).thenReturn(Optional.of(leadPlanner));

        ProjectResponse resp = service.update(701L,
                new com.wams.dto.request.UpdateProjectRequest(
                        null, null, "Updated Venue", null, null, null));

        assertThat(resp.getVenue()).isEqualTo("Updated Venue");
    }

    @Test
    void update_leadPlanner_othersProject_returns404_notLeakingExistence() {
        User plannerUser = leadPlanner.getUser();
        Employee someoneElse = Employee.builder().id(999L)
                .user(User.builder().id(998L).role(Role.LEAD_PLANNER).active(true).build())
                .active(true).build();
        WeddingProject notOwned = sampleProjectWithPlanner(702L, someoneElse);
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        when(projectRepository.findById(702L)).thenReturn(Optional.of(notOwned));
        when(employeeRepository.findByUserId(plannerUser.getId())).thenReturn(Optional.of(leadPlanner));

        assertThatThrownBy(() -> service.update(702L,
                new com.wams.dto.request.UpdateProjectRequest(
                        "Hacker rename", null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        // The project row must NOT be mutated by a rejected update.
        assertThat(notOwned.getProjectName()).isEqualTo("Sample");
    }

    @Test
    void update_leadPlanner_whenNoEmployeeProfile_returns404() {
        // Mirrors getById: no Employee row means isInScope -> false -> 404.
        User plannerUser = User.builder().id(20L).role(Role.LEAD_PLANNER).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        when(projectRepository.findById(703L)).thenReturn(Optional.of(sampleProject(703L)));
        when(employeeRepository.findByUserId(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(703L,
                new com.wams.dto.request.UpdateProjectRequest(
                        "x", null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_leadPlanner_missingProject_returns404() {
        User plannerUser = leadPlanner.getUser();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(plannerUser);
        when(projectRepository.findById(704L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(704L,
                new com.wams.dto.request.UpdateProjectRequest(
                        "x", null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(employeeRepository, never()).findByUserId(any());
    }

    @Test
    void update_primaryClient_denied_returns403() {
        User clientUser = User.builder().id(10L).role(Role.PRIMARY_CLIENT).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(clientUser);

        assertThatThrownBy(() -> service.update(1L,
                new com.wams.dto.request.UpdateProjectRequest(
                        "x", null, null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void update_partnerClient_denied_returns403() {
        User partnerUser = User.builder().id(15L).role(Role.PARTNER_CLIENT).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(partnerUser);

        assertThatThrownBy(() -> service.update(1L,
                new com.wams.dto.request.UpdateProjectRequest(
                        "x", null, null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void update_employee_denied_returns403() {
        User employeeUser = User.builder().id(30L).role(Role.EMPLOYEE).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(employeeUser);

        assertThatThrownBy(() -> service.update(1L,
                new com.wams.dto.request.UpdateProjectRequest(
                        "x", null, null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void update_vendor_denied_returns403() {
        User vendorUser = User.builder().id(40L).role(Role.VENDOR).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(vendorUser);

        assertThatThrownBy(() -> service.update(1L,
                new com.wams.dto.request.UpdateProjectRequest(
                        "x", null, null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void getById_adminMissingProject_returns404() {
        User adminUser = User.builder().id(1L).role(Role.ADMIN).active(true).build();
        when(authenticatedUserProvider.requireCurrentUser()).thenReturn(adminUser);
        when(projectRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(9999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- helpers ----------

    private WeddingProject sampleProject(Long id) {
        return WeddingProject.builder()
                .id(id)
                .projectName("Sample")
                .primaryClient(primaryClient)
                .leadPlanner(leadPlanner)
                .weddingDate(LocalDate.now().plusDays(60))
                .projectStatus(ProjectStatus.PLANNING)
                .agreementSigned(true)
                .bookingDepositPaid(true)
                .build();
    }

    private WeddingProject sampleProjectWithPlanner(Long id, Employee planner) {
        WeddingProject p = sampleProject(id);
        p.setLeadPlanner(planner);
        return p;
    }

    private WeddingProject sampleProjectWithPrimary(Long id, Client primary) {
        WeddingProject p = sampleProject(id);
        p.setPrimaryClient(primary);
        return p;
    }

    private CreateProjectRequest req(boolean signed, boolean paid) {
        return new CreateProjectRequest(
                "Alice & Bob Wedding",
                100L,
                null,
                200L,
                LocalDate.now().plusDays(90),
                "Taj Palace, Jaipur",
                signed,
                paid
        );
    }
}
