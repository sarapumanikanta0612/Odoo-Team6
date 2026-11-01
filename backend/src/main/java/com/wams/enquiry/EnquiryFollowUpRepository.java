package com.wams.enquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnquiryFollowUpRepository extends JpaRepository<EnquiryFollowUp, Long> {
    Page<EnquiryFollowUp> findAllByEnquiryId(Long enquiryId, Pageable pageable);
    Optional<EnquiryFollowUp> findFirstByEnquiryIdOrderByOccurredAtDesc(Long enquiryId);
}
