package com.wams.operations;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WeddingFunctionRepository extends JpaRepository<WeddingFunction, Long> {
    List<WeddingFunction> findAllByProjectIdOrderByFunctionDateAscStartTimeAsc(Long projectId);
    Optional<WeddingFunction> findByIdAndProjectId(Long id, Long projectId);
}
