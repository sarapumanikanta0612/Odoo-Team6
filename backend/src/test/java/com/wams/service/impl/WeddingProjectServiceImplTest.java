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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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

    // ---------- helpers ----------

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
