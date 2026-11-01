package com.wams.vendor;

import com.wams.vendor.dto.VendorRequest;
import com.wams.vendor.dto.VendorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VendorResponse> create(@Valid @RequestBody VendorRequest request) {
        VendorResponse created = vendorService.createByAdmin(request);
        return ResponseEntity.created(URI.create("/api/vendors/" + created.id())).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LEAD_PLANNER')")
    public PagedModel<VendorResponse> search(
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(required = false) VendorCategory category,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "businessName") Pageable pageable,
            Authentication authentication) {
        return new PagedModel<>(vendorService.search(status, category, search, isAdmin(authentication), pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('VENDOR')")
    public VendorResponse getMine() {
        return vendorService.getMine();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEAD_PLANNER')")
    public VendorResponse getById(@PathVariable Long id, Authentication authentication) {
        return vendorService.getById(id, isAdmin(authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VendorResponse update(@PathVariable Long id, @Valid @RequestBody VendorRequest request) {
        return vendorService.update(id, request);
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
