package com.wams.enquiry;

import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.exception.ResourceNotFoundException;
import com.wams.repository.EmployeeRepository;
import com.wams.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Central record-level scope for every internal enquiry operation. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnquiryAccessService {

    private final EnquiryRepository enquiryRepository;
    private final EmployeeRepository employeeRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public User currentStaff() {
        User user = authenticatedUserProvider.requireCurrentUser();
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.LEAD_PLANNER) {
            throw new AccessDeniedException("Only ADMIN or LEAD_PLANNER may access internal enquiries");
        }
        return user;
    }

    public Employee currentPlanner(User user) {
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("No Employee profile linked to lead planner"));
        if (!employee.isActive() || !user.isActive() || user.getRole() != Role.LEAD_PLANNER) {
            throw new AccessDeniedException("Lead planner account is not active");
        }
        return employee;
    }

    public Enquiry requireAccessible(Long enquiryId) {
        User actor = currentStaff();
        Enquiry enquiry = enquiryRepository.findById(enquiryId).orElseThrow(() -> notFound(enquiryId));
        assertAccessible(actor, enquiry);
        return enquiry;
    }

    public Enquiry requireAccessibleForUpdate(Long enquiryId) {
        User actor = currentStaff();
        Enquiry enquiry = enquiryRepository.findByIdForUpdate(enquiryId).orElseThrow(() -> notFound(enquiryId));
        assertAccessible(actor, enquiry);
        return enquiry;
    }

    public void assertAccessible(User actor, Enquiry enquiry) {
        if (actor.getRole() == Role.ADMIN) return;
        Employee planner = currentPlanner(actor);
        if (enquiry.getAssignedLeadPlanner() == null
                || !planner.getId().equals(enquiry.getAssignedLeadPlanner().getId())) {
            throw notFound(enquiry.getId());
        }
    }

    public ResourceNotFoundException notFound(Long enquiryId) {
        return new ResourceNotFoundException("Enquiry not found: " + enquiryId);
    }
}
