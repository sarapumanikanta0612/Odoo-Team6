package com.wams.enquiry;

import com.wams.dto.request.ConvertEnquiryRequest;
import com.wams.dto.response.ProjectResponse;
import com.wams.enquiry.dto.AssignEnquiryRequest;
import com.wams.enquiry.dto.CreateFollowUpRequest;
import com.wams.enquiry.dto.FollowUpResponse;
import com.wams.enquiry.dto.InternalEnquiryDetailResponse;
import com.wams.enquiry.dto.InternalEnquirySummaryResponse;
import com.wams.enquiry.dto.LinkPrimaryClientRequest;
import com.wams.enquiry.dto.UpdateCommercialTermsRequest;
import com.wams.enquiry.dto.UpdateEnquiryStatusRequest;
import com.wams.service.WeddingProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enquiries")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LEAD_PLANNER')")
public class InternalEnquiryController {

    private final InternalEnquiryService enquiryService;
    private final WeddingProjectService projectService;

    @GetMapping
    public PagedModel<InternalEnquirySummaryResponse> list(
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(required = false) Long assignedLeadPlannerId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(enquiryService.list(status, assignedLeadPlannerId, search, pageable));
    }

    @GetMapping("/{id}")
    public InternalEnquiryDetailResponse getById(@PathVariable Long id) {
        return enquiryService.getById(id);
    }

    @PatchMapping("/{id}/assignment")
    @PreAuthorize("hasRole('ADMIN')")
    public InternalEnquiryDetailResponse assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignEnquiryRequest request) {
        return enquiryService.assign(id, request);
    }

    @PatchMapping("/{id}/primary-client")
    @PreAuthorize("hasRole('ADMIN')")
    public InternalEnquiryDetailResponse linkPrimaryClient(
            @PathVariable Long id,
            @Valid @RequestBody LinkPrimaryClientRequest request) {
        return enquiryService.linkPrimaryClient(id, request);
    }

    @PatchMapping("/{id}/status")
    public InternalEnquiryDetailResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEnquiryStatusRequest request) {
        return enquiryService.updateStatus(id, request);
    }

    @PatchMapping("/{id}/commercial")
    public InternalEnquiryDetailResponse updateCommercialTerms(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommercialTermsRequest request) {
        return enquiryService.updateCommercialTerms(id, request);
    }

    @GetMapping("/{id}/follow-ups")
    public PagedModel<FollowUpResponse> listFollowUps(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(enquiryService.listFollowUps(id, pageable));
    }

    @PostMapping("/{id}/follow-ups")
    @ResponseStatus(HttpStatus.CREATED)
    public FollowUpResponse addFollowUp(
            @PathVariable Long id,
            @Valid @RequestBody CreateFollowUpRequest request) {
        return enquiryService.addFollowUp(id, request);
    }

    @PostMapping("/{id}/convert")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse convert(
            @PathVariable Long id,
            @Valid @RequestBody ConvertEnquiryRequest request) {
        return projectService.convert(id, request);
    }
}
