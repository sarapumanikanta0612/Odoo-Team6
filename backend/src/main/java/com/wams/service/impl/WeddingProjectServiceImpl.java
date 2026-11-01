package com.wams.service.impl;

import com.wams.dto.request.ConvertEnquiryRequest;
import com.wams.dto.request.CreateProjectRequest;
import com.wams.dto.request.UpdateProjectRequest;
import com.wams.dto.response.ProjectResponse;
import com.wams.enquiry.Enquiry;
import com.wams.enquiry.EnquiryAccessService;
import com.wams.enquiry.EnquiryRepository;
import com.wams.enquiry.EnquiryStatus;
import com.wams.enquiry.PrimaryClientAssociationService;
import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.entity.WeddingProject;
import com.wams.enums.ProjectStatus;
import com.wams.enums.Role;
import com.wams.exception.BusinessRuleException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.qualification.Agreement;
import com.wams.qualification.AgreementRepository;
import com.wams.qualification.AgreementStatus;
import com.wams.qualification.BookingDeposit;
import com.wams.qualification.BookingDepositRepository;
import com.wams.qualification.BookingDepositStatus;
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
 * Enforces persisted booking qualification and role-scoped project access.
 */
@Service
public class WeddingProjectServiceImpl implements WeddingProjectService {

    private final WeddingProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final EnquiryRepository enquiryRepository;
    private final AgreementRepository agreementRepository;
    private final BookingDepositRepository bookingDepositRepository;
    private final EnquiryAccessService enquiryAccessService;
    private final PrimaryClientAssociationService clientAssociationService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public WeddingProjectServiceImpl(WeddingProjectRepository projectRepository,
                                     ClientRepository clientRepository,
                                     EmployeeRepository employeeRepository,
                                     EnquiryRepository enquiryRepository,
                                     AgreementRepository agreementRepository,
                                     BookingDepositRepository bookingDepositRepository,
                                     EnquiryAccessService enquiryAccessService,
                                     PrimaryClientAssociationService clientAssociationService,
                                     AuthenticatedUserProvider authenticatedUserProvider) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.enquiryRepository = enquiryRepository;
        this.agreementRepository = agreementRepository;
        this.bookingDepositRepository = bookingDepositRepository;
        this.enquiryAccessService = enquiryAccessService;
        this.clientAssociationService = clientAssociationService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Override
    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        User actor = requireProjectCreator();
        Enquiry enquiry = enquiryRepository.findByIdForUpdate(request.enquiryId())
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found: " + request.enquiryId()));
        enquiryAccessService.assertAccessible(actor, enquiry);
        if (enquiry.getAssignedLeadPlanner() != null
                && !enquiry.getAssignedLeadPlanner().getId().equals(request.leadPlannerId())) {
            throw new BusinessRuleException("Project lead must match the enquiry's assigned lead planner");
        }
        return convertLocked(
                actor, enquiry,
                request.projectName(), request.primaryClientId(), request.partnerClientId(),
                request.leadPlannerId(), request.weddingDate(), request.venue());
    }

    @Override
    @Transactional
    public ProjectResponse convert(Long enquiryId, ConvertEnquiryRequest request) {
        User actor = requireProjectCreator();
        Enquiry enquiry = enquiryRepository.findByIdForUpdate(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found: " + enquiryId));
        enquiryAccessService.assertAccessible(actor, enquiry);
        if (enquiry.getAssignedLeadPlanner() == null) {
            throw new BusinessRuleException("Assign a lead planner before converting the enquiry");
        }
        return convertLocked(
                actor, enquiry,
                request.projectName(), null, request.partnerClientId(),
                enquiry.getAssignedLeadPlanner().getId(), request.weddingDate(), request.venue());
    }

    private ProjectResponse convertLocked(
            User actor,
            Enquiry enquiry,
            String projectName,
            Long expectedPrimaryClientId,
            Long partnerClientId,
            Long leadPlannerId,
            java.time.LocalDate weddingDate,
            String venue) {
        if (enquiry.getStatus() == EnquiryStatus.CONVERTED
                || projectRepository.existsByEnquiryId(enquiry.getId())) {
            throw new BusinessRuleException("Enquiry is already converted: " + enquiry.getId());
        }
        if (enquiry.getStatus() == EnquiryStatus.LOST) {
            throw new BusinessRuleException("A lost enquiry cannot be converted: " + enquiry.getId());
        }

        Agreement agreement = agreementRepository.findByEnquiryId(enquiry.getId()).orElse(null);
        BookingDeposit deposit = bookingDepositRepository.findByEnquiryId(enquiry.getId()).orElse(null);
        boolean signed = agreement != null && agreement.getStatus() == AgreementStatus.SIGNED;
        boolean paid = deposit != null && deposit.getStatus() == BookingDepositStatus.PAID;
        if (!signed || !paid) {
            throw new BusinessRuleException(
                    "Project creation rejected: persisted signed agreement and paid booking deposit are required " +
                            "(agreementSigned=" + signed + ", bookingDepositPaid=" + paid + ")");
        }

        Client primary = clientAssociationService.resolveForConversion(enquiry, actor);
        if (expectedPrimaryClientId != null && !expectedPrimaryClientId.equals(primary.getId())) {
            throw new BusinessRuleException(
                    "Requested primary client does not match the enquiry's authoritative client association");
        }

        Client partner = null;
        if (partnerClientId != null) {
            partner = clientRepository.findById(partnerClientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Partner client not found: " + partnerClientId));
            if (partner.getUser().getRole() != Role.PARTNER_CLIENT) {
                throw new BusinessRuleException("Partner client's user must have PARTNER_CLIENT role");
            }
            if (partner.getId().equals(primary.getId())) {
                throw new BusinessRuleException("Partner client cannot be the same as the primary client");
            }
        }

        Employee leadPlanner = requireActiveAuthorizedLead(leadPlannerId);
        WeddingProject project = WeddingProject.builder()
                .projectName(projectName.trim())
                .enquiry(enquiry)
                .primaryClient(primary)
                .partnerClient(partner)
                .leadPlanner(leadPlanner)
                .weddingDate(weddingDate)
                .venue(venue)
                .selectedPackage(enquiry.getSelectedPackage())
                .agreedAmount(enquiry.getAgreedAmount())
                .bookingDepositAmount(deposit.getAmount())
                .bookingDepositReference(deposit.getPaymentReference())
                .bookingDepositPaidOn(deposit.getPaidOn())
                .bookingDepositMethod(deposit.getPaymentMethod())
                .projectStatus(ProjectStatus.PLANNING)
                .agreementSigned(true)
                .bookingDepositPaid(true)
                .build();

        WeddingProject saved = projectRepository.save(project);
        enquiry.setStatus(EnquiryStatus.CONVERTED);
        enquiryRepository.save(enquiry);
        return ProjectResponse.from(saved);
    }

    private User requireProjectCreator() {
        User actor = authenticatedUserProvider.requireCurrentUser();
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.LEAD_PLANNER) {
            throw new AccessDeniedException("Only ADMIN or LEAD_PLANNER may create a wedding project");
        }
        return actor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        User me = authenticatedUserProvider.requireCurrentUser();
        return switch (me.getRole()) {
            case ADMIN -> projectRepository.findAll().stream().map(ProjectResponse::from).toList();
            case LEAD_PLANNER -> {
                Employee employee = employeeOf(me);
                yield projectRepository.findAllByLeadPlannerId(employee.getId()).stream()
                        .map(ProjectResponse::from).toList();
            }
            case PRIMARY_CLIENT -> {
                Client client = clientOf(me);
                yield projectRepository.findAllByPrimaryClientId(client.getId()).stream()
                        .map(ProjectResponse::from).toList();
            }
            case PARTNER_CLIENT -> {
                Client client = clientOf(me);
                yield projectRepository.findAllByPartnerClientId(client.getId()).stream()
                        .map(ProjectResponse::from).toList();
            }
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
            case LEAD_PLANNER, PRIMARY_CLIENT, PARTNER_CLIENT -> ProjectResponse.from(loadInScope(me, id));
            case EMPLOYEE, VENDOR -> throw new AccessDeniedException(
                    "Role " + me.getRole() + " is not permitted to view wedding projects");
        };
    }

    @Override
    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request) {
        User me = authenticatedUserProvider.requireCurrentUser();
        WeddingProject project = switch (me.getRole()) {
            case ADMIN -> entity(id);
            case LEAD_PLANNER -> loadInScope(me, id);
            case PRIMARY_CLIENT, PARTNER_CLIENT, EMPLOYEE, VENDOR -> throw new AccessDeniedException(
                    "Role " + me.getRole() + " is not permitted to update wedding projects");
        };

        if (me.getRole() == Role.LEAD_PLANNER
                && request.leadPlannerId() != null
                && !request.leadPlannerId().equals(project.getLeadPlanner().getId())) {
            throw new AccessDeniedException("Only ADMIN may reassign a wedding project");
        }

        // Resolve every association before mutating simple fields, so an invalid
        // reassignment cannot leave a managed entity partially changed.
        Employee replacementLead = request.leadPlannerId() == null
                ? null
                : requireActiveAuthorizedLead(request.leadPlannerId());

        if (request.projectName() != null && !request.projectName().isBlank()) {
            project.setProjectName(request.projectName().trim());
        }
        if (request.weddingDate() != null) project.setWeddingDate(request.weddingDate());
        if (request.venue() != null) project.setVenue(request.venue());
        if (request.projectStatus() != null) project.setProjectStatus(request.projectStatus());
        if (request.partnerClientId() != null) {
            Client partner = clientRepository.findById(request.partnerClientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Partner client not found: " + request.partnerClientId()));
            if (partner.getUser().getRole() != Role.PARTNER_CLIENT) {
                throw new BusinessRuleException("Partner client's user must have PARTNER_CLIENT role");
            }
            if (partner.getId().equals(project.getPrimaryClient().getId())) {
                throw new BusinessRuleException("Partner client cannot be the same as the primary client");
            }
            project.setPartnerClient(partner);
        }
        if (replacementLead != null) project.setLeadPlanner(replacementLead);
        return ProjectResponse.from(project);
    }

    private Employee requireActiveAuthorizedLead(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead planner not found: " + employeeId));
        User user = employee.getUser();
        if (user == null || (user.getRole() != Role.LEAD_PLANNER && user.getRole() != Role.ADMIN)) {
            throw new BusinessRuleException("Lead planner's user must have LEAD_PLANNER or ADMIN role");
        }
        if (!employee.isActive() || !user.isActive()) {
            throw new BusinessRuleException("Lead planner Employee and User accounts must both be active");
        }
        return employee;
    }

    private WeddingProject loadInScope(User me, Long id) {
        WeddingProject project = projectRepository.findById(id).orElseThrow(() -> notFound(id));
        if (!isInScope(me, project)) throw notFound(id);
        return project;
    }

    private boolean isInScope(User me, WeddingProject project) {
        return switch (me.getRole()) {
            case LEAD_PLANNER -> {
                Employee employee = employeeRepository.findByUserId(me.getId()).orElse(null);
                yield employee != null && employee.getId().equals(project.getLeadPlanner().getId());
            }
            case PRIMARY_CLIENT -> {
                Client client = clientRepository.findByUserId(me.getId()).orElse(null);
                yield client != null && client.getId().equals(project.getPrimaryClient().getId());
            }
            case PARTNER_CLIENT -> {
                Client client = clientRepository.findByUserId(me.getId()).orElse(null);
                yield client != null
                        && project.getPartnerClient() != null
                        && client.getId().equals(project.getPartnerClient().getId());
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
