package com.wams.qualification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingDepositRepository extends JpaRepository<BookingDeposit, Long> {
    Optional<BookingDeposit> findByEnquiryId(Long enquiryId);
    boolean existsByPaymentReference(String paymentReference);
    boolean existsByPaymentReferenceAndIdNot(String paymentReference, Long id);
}
