package com.wams.operations;

import com.wams.entity.BaseEntity;
import com.wams.entity.Employee;
import com.wams.entity.WeddingProject;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "project_tasks", indexes = {
        @Index(name = "idx_task_project_status", columnList = "project_id,status"),
        @Index(name = "idx_task_assignee_status", columnList = "assigned_employee_id,status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectTask extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_project"))
    private WeddingProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "function_id", foreignKey = @ForeignKey(name = "fk_task_function"))
    private WeddingFunction function;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_assignee"))
    private Employee assignedEmployee;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;
}
