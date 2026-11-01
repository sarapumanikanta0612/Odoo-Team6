package com.wams.entity;

import com.wams.enquiry.Enquiry;
import com.wams.enums.ProjectStatus;
import com.wams.qualification.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an officially confirmed wedding engagement.
 *
 * New projects carry a unique Enquiry provenance link and are created only by
 * WeddingProjectService after it verifies persisted SIGNED Agreement and PAID
 * BookingDeposit records. The two booleans are a read-only qualification
 * snapshot retained for response and legacy-row compatibility.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wedding_projects",
        uniqueConstraints = @UniqueConstraint(name = "uk_wedding_projects_enquiry", columnNames = "enquiry_id"))
public class WeddingProject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    /**
     * Qualification provenance. Nullable only so existing pre-P0 rows can be
     * loaded; every new project created by the service must set it.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id",
            foreignKey = @ForeignKey(name = "fk_project_enquiry"))
    private Enquiry enquiry;

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

    /** Immutable commercial/payment snapshots captured at conversion for finance. */
    @Column(name = "selected_package", length = 200)
    private String selectedPackage;

    @Column(name = "agreed_amount", precision = 14, scale = 2)
    private BigDecimal agreedAmount;

    @Column(name = "booking_deposit_amount", precision = 14, scale = 2)
    private BigDecimal bookingDepositAmount;

    @Column(name = "booking_deposit_reference", length = 100)
    private String bookingDepositReference;

    @Column(name = "booking_deposit_paid_on")
    private LocalDate bookingDepositPaidOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_deposit_method", length = 30)
    private PaymentMethod bookingDepositMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", nullable = false, length = 30)
    private ProjectStatus projectStatus;

    @Column(name = "agreement_signed", nullable = false)
    private boolean agreementSigned;

    @Column(name = "booking_deposit_paid", nullable = false)
    private boolean bookingDepositPaid;
}
