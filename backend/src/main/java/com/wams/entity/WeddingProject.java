package com.wams.entity;

import com.wams.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Represents an officially confirmed wedding engagement.
 *
 * Business invariant (enforced in {@code WeddingProjectService.create}):
 *   A WeddingProject row MUST NOT exist unless both {@code agreementSigned}
 *   AND {@code bookingDepositPaid} were true at the moment of creation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wedding_projects")
public class WeddingProject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_client_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_project_primary_client"))
    private Client primaryClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_client_id",
            foreignKey = @ForeignKey(name = "fk_project_partner_client"))
    private Client partnerClient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_planner_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_project_lead_planner"))
    private Employee leadPlanner;

    @Column(name = "wedding_date", nullable = false)
    private LocalDate weddingDate;

    @Column(name = "venue", length = 300)
    private String venue;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", nullable = false, length = 30)
    private ProjectStatus projectStatus;

    @Column(name = "agreement_signed", nullable = false)
    private boolean agreementSigned;

    @Column(name = "booking_deposit_paid", nullable = false)
    private boolean bookingDepositPaid;
}
