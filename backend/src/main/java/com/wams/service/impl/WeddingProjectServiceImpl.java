package com.wams.service.impl;

import com.wams.dto.request.CreateProjectRequest;
import com.wams.dto.request.UpdateProjectRequest;
import com.wams.dto.response.ProjectResponse;
import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.WeddingProject;
import com.wams.enums.ProjectStatus;
import com.wams.enums.Role;
import com.wams.exception.BusinessRuleException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.repository.ClientRepository;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.WeddingProjectRepository;
import com.wams.service.WeddingProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owns the single most important invariant in WAMS:
 *
 *   A WeddingProject row MUST NOT be created unless BOTH:
 *     - the service agreement is signed, AND
 *     - the booking deposit is paid.
 *
 * This check happens here, server-side, regardless of what the frontend sends.
 */
@Service
public class WeddingProjectServiceImpl implements WeddingProjectService {

    private final WeddingProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;

    public WeddingProjectServiceImpl(WeddingProjectRepository projectRepository,
                                     ClientRepository clientRepository,
                                     EmployeeRepository employeeRepository) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public ProjectResponse create(CreateProjectRequest r) {
        // --- Booking gate: both conditions must be true, no exceptions. ---
        boolean signed  = Boolean.TRUE.equals(r.agreementSigned());
        boolean paid    = Boolean.TRUE.equals(r.bookingDepositPaid());
        if (!signed || !paid) {
            throw new BusinessRuleException(
                    "Project creation rejected: requires signed agreement AND paid booking deposit (agreementSigned="
                            + signed + ", bookingDepositPaid=" + paid + ")");
        }

        Client primary = clientRepository.findById(r.primaryClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Primary client not found: " + r.primaryClientId()));
        if (primary.getUser().getRole() != Role.PRIMARY_CLIENT) {
            throw new BusinessRuleException(
                    "Primary client's user must have PRIMARY_CLIENT role");
        }

        Client partner = null;
        if (r.partnerClientId() != null) {
            partner = clientRepository.findById(r.partnerClientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Partner client not found: " + r.partnerClientId()));
            if (partner.getUser().getRole() != Role.PARTNER_CLIENT) {
                throw new BusinessRuleException(
                        "Partner client's user must have PARTNER_CLIENT role");
            }
            if (partner.getId().equals(primary.getId())) {
                throw new BusinessRuleException("Partner client cannot be the same as the primary client");
            }
        }

        Employee leadPlanner = employeeRepository.findById(r.leadPlannerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lead planner not found: " + r.leadPlannerId()));
        Role plannerRole = leadPlanner.getUser().getRole();
        if (plannerRole != Role.LEAD_PLANNER && plannerRole != Role.ADMIN) {
            throw new BusinessRuleException(
                    "Lead planner's user must have LEAD_PLANNER or ADMIN role");
        }
        if (!leadPlanner.isActive()) {
            throw new BusinessRuleException("Lead planner is not active");
        }

        WeddingProject project = WeddingProject.builder()
                .projectName(r.projectName().trim())
                .primaryClient(primary)
                .partnerClient(partner)
                .leadPlanner(leadPlanner)
                .weddingDate(r.weddingDate())
                .venue(r.venue())
                .projectStatus(ProjectStatus.PLANNING)
                .agreementSigned(true)
                .bookingDepositPaid(true)
                .build();

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        return ProjectResponse.from(entity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAll().stream().map(ProjectResponse::from).toList();
    }

    @Override
    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest r) {
        WeddingProject p = entity(id);
        if (r.projectName() != null && !r.projectName().isBlank()) p.setProjectName(r.projectName().trim());
        if (r.weddingDate() != null) p.setWeddingDate(r.weddingDate());
        if (r.venue() != null) p.setVenue(r.venue());
        if (r.projectStatus() != null) p.setProjectStatus(r.projectStatus());
        if (r.partnerClientId() != null) {
            Client partner = clientRepository.findById(r.partnerClientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Partner client not found: " + r.partnerClientId()));
            if (partner.getUser().getRole() != Role.PARTNER_CLIENT) {
                throw new BusinessRuleException("Partner client's user must have PARTNER_CLIENT role");
            }
            p.setPartnerClient(partner);
        }
        if (r.leadPlannerId() != null) {
            Employee planner = employeeRepository.findById(r.leadPlannerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Lead planner not found: " + r.leadPlannerId()));
            p.setLeadPlanner(planner);
        }
        return ProjectResponse.from(p);
    }

    private WeddingProject entity(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WeddingProject not found: " + id));
    }
}
