package com.wams.operations;

import com.wams.entity.BaseEntity;
import com.wams.entity.WeddingProject;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "wedding_functions", indexes = @Index(name = "idx_function_project_date", columnList = "project_id,function_date"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WeddingFunction extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_function_project"))
    private WeddingProject project;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "function_date", nullable = false)
    private LocalDate functionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "venue", nullable = false, length = 300)
    private String venue;
}
