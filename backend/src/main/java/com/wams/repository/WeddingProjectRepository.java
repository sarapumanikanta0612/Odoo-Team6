package com.wams.repository;

import com.wams.entity.WeddingProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeddingProjectRepository extends JpaRepository<WeddingProject, Long> {

    List<WeddingProject> findAllByPrimaryClientId(Long clientId);

    List<WeddingProject> findAllByPartnerClientId(Long clientId);

    List<WeddingProject> findAllByLeadPlannerId(Long employeeId);
}
