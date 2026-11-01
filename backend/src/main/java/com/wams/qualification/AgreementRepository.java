package com.wams.qualification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {
    Optional<Agreement> findByEnquiryId(Long enquiryId);
    boolean existsByAgreementReference(String agreementReference);
    boolean existsByAgreementReferenceAndIdNot(String agreementReference, Long id);
}
