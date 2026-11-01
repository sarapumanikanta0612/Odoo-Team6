package com.wams.enquiry;

import com.wams.enquiry.dto.AssignEnquiryRequest;
import com.wams.enquiry.dto.CreateFollowUpRequest;
import com.wams.enquiry.dto.FollowUpResponse;
import com.wams.enquiry.dto.InternalEnquiryDetailResponse;
import com.wams.enquiry.dto.InternalEnquirySummaryResponse;
import com.wams.enquiry.dto.LinkPrimaryClientRequest;
import com.wams.enquiry.dto.UpdateCommercialTermsRequest;
import com.wams.enquiry.dto.UpdateEnquiryStatusRequest;
import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.entity.WeddingProject;
import com.wams.enums.Role;
import com.wams.exception.BusinessRuleException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.qualification.Agreement;
import com.wams.qualification.AgreementRepository;
import com.wams.qualification.AgreementStatus;
import com.wams.qualification.BookingDeposit;
import com.wams.qualification.BookingDepositRepository;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.WeddingProjectRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalEnquiryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "weddingStartDate", "contactName", "status");

    private final EnquiryRepository enquiryRepository;
    private final EnquiryFollowUpRepository followUpRepository;
    private final EmployeeRepository employeeRepository;
    private final AgreementRepository agreementRepository;
    private final BookingDepositRepository bookingDepositRepository;
    private final WeddingProjectRepository projectRepository;
    private final EnquiryAccessService accessService;
    private final PrimaryClientAssociationService clientAssociationService;

    public Page<InternalEnquirySummaryResponse> list(
            EnquiryStatus status,
            Long assignedLeadPlannerId,
            String search,
            Pageable pageable) {
        User actor = accessService.currentStaff();
        Long effectiveAssignee = assignedLeadPlannerId;
        if (actor.getRole() == Role.LEAD_PLANNER) {
            effectiveAssignee = accessService.currentPlanner(actor).getId();
        }
        Specification<Enquiry> specification = filter(status, effectiveAssignee, search);
        return enquiryRepository.findAll(specification, bounded(pageable))
                .map(enquiry -> InternalEnquirySummaryResponse.from(
                        enquiry,
                        followUpRepository.findFirstByEnquiryIdOrderByOccurredAtDesc(enquiry.getId())
                                .map(EnquiryFollowUp::getNextFollowUpDate)
                                .orElse(null)));
    }

    public InternalEnquiryDetailResponse getById(Long enquiryId) {
        return detail(accessService.requireAccessible(enquiryId));
    }

    @Transactional
    public InternalEnquiryDetailResponse assign(Long enquiryId, AssignEnquiryRequest request) {
        User actor = accessService.currentStaff();
        if (actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN may assign enquiries");
        }
        Enquiry enquiry = accessService.requireAccessibleForUpdate(enquiryId);
        requireMutable(enquiry, "assigned");
        Employee planner = employeeRepository.findById(request.assignedLeadPlannerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lead planner not found: " + request.assignedLeadPlannerId()));
        if (planner.getUser().getRole() != Role.LEAD_PLANNER) {
            throw new BusinessRuleException("Assigned employee must have LEAD_PLANNER role");
        }
        if (!planner.isActive() || !planner.getUser().isActive()) {
            throw new BusinessRuleException("Assigned lead planner Employee and User accounts must both be active");
        }
        enquiry.setAssignedLeadPlanner(planner);
        return detail(enquiryRepository.save(enquiry));
    }

    @Transactional
    public InternalEnquiryDetailResponse linkPrimaryClient(
            Long enquiryId,
            LinkPrimaryClientRequest request) {
        return detail(clientAssociationService.linkByAdmin(enquiryId, request));
    }

    @Transactional
    public InternalEnquiryDetailResponse updateStatus(Long enquiryId, UpdateEnquiryStatusRequest request) {
        Enquiry enquiry = accessService.requireAccessibleForUpdate(enquiryId);
        EnquiryStatus requested = request.status();
        if (requested == enquiry.getStatus()) return detail(enquiry);
        if (!allowedTransitions(enquiry.getStatus()).contains(requested)) {
            throw new BusinessRuleException(
                    "Invalid enquiry status transition: " + enquiry.getStatus() + " -> " + requested);
        }
        enquiry.setStatus(requested);
        return detail(enquiryRepository.save(enquiry));
    }

    @Transactional
    public InternalEnquiryDetailResponse updateCommercialTerms(
            Long enquiryId,
            UpdateCommercialTermsRequest request) {
        Enquiry enquiry = accessService.requireAccessibleForUpdate(enquiryId);
        requireMutable(enquiry, "updated");
        if (agreementRepository.findByEnquiryId(enquiryId)
                .map(Agreement::getStatus)
                .filter(status -> status == AgreementStatus.SIGNED)
                .isPresent()) {
            throw new BusinessRuleException("Commercial terms cannot change after the agreement is signed");
        }
        enquiry.setSelectedPackage(request.selectedPackage().trim());
        enquiry.setPackageDescription(trimToNull(request.packageDescription()));
        enquiry.setAgreedAmount(request.agreedAmount());
        return detail(enquiryRepository.save(enquiry));
    }

    @Transactional
    public FollowUpResponse addFollowUp(Long enquiryId, CreateFollowUpRequest request) {
        Enquiry enquiry = accessService.requireAccessibleForUpdate(enquiryId);
        requireMutable(enquiry, "given a follow-up");
        User recorder = accessService.currentStaff();
        EnquiryFollowUp followUp = followUpRepository.save(EnquiryFollowUp.builder()
                .enquiry(enquiry)
                .occurredAt(request.occurredAt())
                .notes(request.notes().trim())
                .nextFollowUpDate(request.nextFollowUpDate())
                .recordedBy(recorder)
                .build());
        return FollowUpResponse.from(followUp);
    }

    public Page<FollowUpResponse> listFollowUps(Long enquiryId, Pageable pageable) {
        accessService.requireAccessible(enquiryId);
        Pageable bounded = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "occurredAt"));
        return followUpRepository.findAllByEnquiryId(enquiryId, bounded).map(FollowUpResponse::from);
    }

    public InternalEnquiryDetailResponse detail(Enquiry enquiry) {
        var agreementStatus = agreementRepository.findByEnquiryId(enquiry.getId())
                .map(Agreement::getStatus).orElse(null);
        var depositStatus = bookingDepositRepository.findByEnquiryId(enquiry.getId())
                .map(BookingDeposit::getStatus).orElse(null);
        WeddingProject project = projectRepository.findByEnquiryId(enquiry.getId()).orElse(null);
        Long projectId = project == null ? null : project.getId();
        Client associatedClient = enquiry.getPrimaryClient() != null
                ? enquiry.getPrimaryClient()
                : project == null ? null : project.getPrimaryClient();
        return InternalEnquiryDetailResponse.from(
                enquiry, associatedClient, agreementStatus, depositStatus, projectId,
                allowedTransitions(enquiry.getStatus()));
    }

    public static List<EnquiryStatus> allowedTransitions(EnquiryStatus current) {
        return switch (current) {
            case NEW -> List.of(EnquiryStatus.CONTACTED, EnquiryStatus.CONSULTATION,
                    EnquiryStatus.PROPOSAL, EnquiryStatus.LOST);
            case CONTACTED -> List.of(EnquiryStatus.CONSULTATION, EnquiryStatus.PROPOSAL, EnquiryStatus.LOST);
            case CONSULTATION -> List.of(EnquiryStatus.PROPOSAL, EnquiryStatus.LOST);
            case PROPOSAL -> List.of(EnquiryStatus.LOST);
            case CONVERTED, LOST -> List.of();
        };
    }

    private void requireMutable(Enquiry enquiry, String operation) {
        if (enquiry.getStatus() == EnquiryStatus.CONVERTED || enquiry.getStatus() == EnquiryStatus.LOST) {
            throw new BusinessRuleException("A " + enquiry.getStatus() + " enquiry cannot be " + operation);
        }
    }

    private Pageable bounded(Pageable pageable) {
        List<Sort.Order> safeOrders = pageable.getSort().stream()
                .filter(order -> SORTABLE.contains(order.getProperty()))
                .toList();
        Sort sort = safeOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(safeOrders);
        return PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                sort);
    }

    private Specification<Enquiry> filter(EnquiryStatus status, Long assigneeId, String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(criteriaBuilder.equal(root.get("status"), status));
            if (assigneeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignedLeadPlanner").get("id"), assigneeId));
            }
            if (search != null && !search.isBlank()) {
                String literal = escapeLike(search.trim().toLowerCase(Locale.ROOT));
                String pattern = "%" + literal + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("contactName")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("destination")), pattern, '\\')));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
