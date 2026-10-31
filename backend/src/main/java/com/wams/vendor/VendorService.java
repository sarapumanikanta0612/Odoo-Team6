package com.wams.vendor;

import com.wams.exception.DuplicateResourceException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.vendor.dto.VendorRequest;
import com.wams.vendor.dto.VendorResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository vendorRepository;

    /** SRS 5.5: vendors added by the agency/admin are trusted, so they start APPROVED. */
    @Transactional
    public VendorResponse createByAdmin(VendorRequest request) {
        String email = normalizeEmail(request.email());
        if (vendorRepository.existsByEmail(email)) {
            throw duplicateEmail();
        }
        Vendor vendor = new Vendor();
        applyRequest(vendor, request, email);
        vendor.setStatus(VendorStatus.APPROVED);
        return VendorResponse.from(vendorRepository.save(vendor));
    }

    /** Admin sees every status. Everyone else only sees APPROVED vendors (the ones a planner may invite). */
    public Page<VendorResponse> search(VendorStatus status, VendorCategory category, String search,
                                       boolean adminView, Pageable pageable) {
        VendorStatus effectiveStatus = adminView ? status : VendorStatus.APPROVED;
        return vendorRepository.findAll(filter(effectiveStatus, category, search), pageable)
                .map(VendorResponse::from);
    }

    public VendorResponse getById(Long id, boolean adminView) {
        Vendor vendor = findVendor(id);
        if (!adminView && vendor.getStatus() != VendorStatus.APPROVED) {
            throw notFound(id);
        }
        return VendorResponse.from(vendor);
    }

    @Transactional
    public VendorResponse update(Long id, VendorRequest request) {
        Vendor vendor = findVendor(id);
        String email = normalizeEmail(request.email());
        if (vendorRepository.existsByEmailAndIdNot(email, id)) {
            throw duplicateEmail();
        }
        applyRequest(vendor, request, email);
        // flush so the response carries the new updatedAt
        return VendorResponse.from(vendorRepository.saveAndFlush(vendor));
    }

    private Vendor findVendor(Long id) {
        return vendorRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static Specification<Vendor> filter(VendorStatus status, VendorCategory category, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("businessName")), pattern),
                        cb.like(cb.lower(root.<String>get("city")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void applyRequest(Vendor vendor, VendorRequest request, String normalizedEmail) {
        vendor.setBusinessName(request.businessName().trim());
        vendor.setCategory(request.category());
        vendor.setContactPerson(trimToNull(request.contactPerson()));
        vendor.setEmail(normalizedEmail);
        vendor.setPhone(request.phone().trim());
        vendor.setCity(trimToNull(request.city()));
        vendor.setAddress(trimToNull(request.address()));
        vendor.setServiceDescription(trimToNull(request.serviceDescription()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Vendor " + id + " not found");
    }

    private static DuplicateResourceException duplicateEmail() {
        return new DuplicateResourceException("A vendor with this email already exists");
    }
}
