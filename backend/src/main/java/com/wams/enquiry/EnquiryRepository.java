package com.wams.enquiry;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long>, JpaSpecificationExecutor<Enquiry> {

    /** Serializes qualification/conversion updates for one enquiry. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enquiry e where e.id = :id")
    Optional<Enquiry> findByIdForUpdate(@Param("id") Long id);
}
