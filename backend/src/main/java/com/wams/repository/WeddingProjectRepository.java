package com.wams.repository;

import com.wams.entity.WeddingProject;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeddingProjectRepository extends JpaRepository<WeddingProject, Long> {

    List<WeddingProject> findAllByPrimaryClientId(Long clientId);

    List<WeddingProject> findAllByPartnerClientId(Long clientId);

    List<WeddingProject> findAllByLeadPlannerId(Long employeeId);

    Optional<WeddingProject> findByEnquiryId(Long enquiryId);

    boolean existsByEnquiryId(Long enquiryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from WeddingProject p where p.enquiry.id = :enquiryId")
    Optional<WeddingProject> findByEnquiryIdForUpdate(@Param("enquiryId") Long enquiryId);
}
