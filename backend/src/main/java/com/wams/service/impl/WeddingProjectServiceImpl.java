package com.wams.service.impl;

import com.wams.dto.request.CreateProjectRequest;
import com.wams.dto.request.UpdateProjectRequest;
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
import com.wams.service.WeddingProjectService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owns two invariants:
 *
 * <ol>
 *   <li><b>Booking gate:</b> a WeddingProject row MUST NOT be created unless both
 *       the service agreement is signed AND the booking deposit is paid.</li>
 *   <li><b>Read &amp; update scope:</b> list, get-by-id, and update operations
 *       are filtered to the authenticated user's role-defined scope. Identity
 *       is read from the Spring Security context — never from the request.</li>
 * </ol>
 */
@Service
public class WeddingProjectServiceImpl implements WeddingProjectService {

    private final WeddingProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public WeddingProjectServiceImpl(WeddingProjectRepository projectRepository,
                                     ClientRepository clientRepository,
                                     EmployeeRepository employeeRepository,
                                     AuthenticatedUserProvider authenticatedUserProvider) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
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

    // ----- Read endpoints: role-scoped -----

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        User me = authenticatedUserProvider.requireCurrentUser();
        return switch (me.getRole()) {
            case ADMIN -> projectRepository.findAll().stream().map(ProjectResponse::from).toList();
            case LEAD_PLANNER -> {
                Employee emp = employeeOf(me);
                yield projectRepository.findAllByLeadPlannerId(emp.getId()).stream()
                        .map(ProjectResponse::from).toList();
            }
            case PRIMARY_CLIENT -> {
                Client c = clientOf(me);
                yield projectRepository.findAllByPrimaryClientId(c.getId()).stream()
                        .map(ProjectResponse::from).toList();
            }
            case PARTNER_CLIENT -> {
                Client c = clientOf(me);
                yield projectRepository.findAllByPartnerClientId(c.getId()).stream()
                        .map(ProjectResponse::from).toList();
            }
            // The current data model has no FK from a plain EMPLOYEE to a project,
            // so there is no safe way to scope their listing. VENDOR has no
            // business reason to read project rows. Both are denied here; keep
            // this in sync with the task's role matrix.
            case EMPLOYEE, VENDOR -> throw new AccessDeniedException(
                    "Role " + me.getRole() + " is not permitted to list wedding projects");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        User me = authenticatedUserProvider.requireCurrentUser();
        return switch (me.getRole()) {
            case ADMIN -> ProjectResponse.from(entity(id));
            case LEAD_PLANNER, PRIMARY_CLIENT, PARTNER_CLIENT ->
                    ProjectResponse.from(loadInScope(me, id));
            case EMPLOYEE, VENDOR -> throw new AccessDeniedException(
                    "Role " + me.getRole() + " is not permitted to view wedding projects");
        };
    }

    @Override
    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest r) {
        User me = authenticatedUserProvider.requireCurrentUser();
        WeddingProject p = switch (me.getRole()) {
            case ADMIN -> entity(id);
            case LEAD_PLANNER -> loadInScope(me, id);
            // Clients, plain employees, and vendors have no legitimate reason to
            // modify project rows. SecurityConfig also denies these at the URL
            // layer; this is defense-in-depth so a future URL rule change can't
            // silently open the gate.
            case PRIMARY_CLIENT, PARTNER_CLIENT, EMPLOYEE, VENDOR ->
                    throw new AccessDeniedException(
                            "Role " + me.getRole() + " is not permitted to update wedding projects");
        };

        // Reassigning ownership is an ADMIN-only operation. A lead planner may
        // send their current leadPlannerId (a no-op, for example when a frontend
        // submits a complete edit form), but may not transfer the project to a
        // different Employee. Check this before mutating any project fields so a
        // rejected request cannot partially update the managed entity.
        if (me.getRole() == Role.LEAD_PLANNER
                && r.leadPlannerId() != null
                && !r.leadPlannerId().equals(p.getLeadPlanner().getId())) {
            throw new AccessDeniedException("Only ADMIN may reassign a wedding project");
        }

        // Existing field-update + validation logic. Unchanged.
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

    // ----- helpers -----

    /**
     * Load a project if the caller's role-defined scope covers it. If the row
     * does not exist OR exists but is outside the caller's scope, a
     * {@link ResourceNotFoundException} (→ 404) is thrown. The two cases are
     * deliberately indistinguishable to the caller so project ids cannot be
     * enumerated.
     */
    private WeddingProject loadInScope(User me, Long id) {
        WeddingProject p = projectRepository.findById(id).orElseThrow(() -> notFound(id));
        if (!isInScope(me, p)) {
            throw notFound(id);
        }
        return p;
    }

    /** Scope check for role-restricted viewers. */
    private boolean isInScope(User me, WeddingProject p) {
        return switch (me.getRole()) {
            case LEAD_PLANNER -> {
                Employee emp = employeeRepository.findByUserId(me.getId()).orElse(null);
                yield emp != null && emp.getId().equals(p.getLeadPlanner().getId());
            }
            case PRIMARY_CLIENT -> {
                Client c = clientRepository.findByUserId(me.getId()).orElse(null);
                yield c != null && c.getId().equals(p.getPrimaryClient().getId());
            }
            case PARTNER_CLIENT -> {
                Client c = clientRepository.findByUserId(me.getId()).orElse(null);
                yield c != null
                        && p.getPartnerClient() != null
                        && c.getId().equals(p.getPartnerClient().getId());
            }
            default -> false;
        };
    }

    private Employee employeeOf(User me) {
        return employeeRepository.findByUserId(me.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No Employee profile linked to the authenticated user"));
    }

    private Client clientOf(User me) {
        return clientRepository.findByUserId(me.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No Client profile linked to the authenticated user"));
    }

    private WeddingProject entity(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("WeddingProject not found: " + id);
    }
}
