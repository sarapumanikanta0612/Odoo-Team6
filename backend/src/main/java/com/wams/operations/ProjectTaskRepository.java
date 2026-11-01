package com.wams.operations;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long> {
    List<ProjectTask> findAllByProjectIdOrderByDueDateAsc(Long projectId);
    List<ProjectTask> findAllByAssignedEmployeeIdOrderByDueDateAsc(Long employeeId);
}
