package com.wams.enquiry;

import com.wams.entity.Client;
import com.wams.entity.Employee;
import com.wams.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Prospective booking from public submission through qualified project conversion. */
@Entity
@Table(name = "enquiries", indexes = {
        @Index(name = "idx_enquiries_status_created", columnList = "status,created_at"),
        @Index(name = "idx_enquiries_assignee_status", columnList = "assigned_lead_planner_id,status"),
        @Index(name = "idx_enquiries_primary_client", columnList = "primary_client_id")
})
@Check(constraints = "agreed_amount is null or agreed_amount > 0")
@Getter
@Setter
@NoArgsConstructor
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contact_name", nullable = false, length = 120)
    private String contactName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "wedding_start_date")
    private LocalDate weddingStartDate;

    @Column(name = "wedding_end_date")
    private LocalDate weddingEndDate;

    @Column(name = "destination", length = 150)
    private String destination;

    @Column(name = "estimated_guest_count")
    private Integer estimatedGuestCount;

    @Column(name = "budget_min", precision = 12, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 12, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "services_of_interest", length = 1000)
    private String servicesOfInterest;

    @Column(name = "theme_style", length = 200)
    private String themeStyle;

    @Column(name = "functions_required", length = 500)
    private String functionsRequired;

    @Column(name = "special_requests", length = 2000)
    private String specialRequests;

    @Column(name = "message", length = 2000)
    private String message;

    /** Requested consultation date; this is not a confirmed calendar booking. */
    @Column(name = "preferred_consultation_date")
    private LocalDate preferredConsultationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_lead_planner_id",
            foreignKey = @ForeignKey(name = "fk_enquiry_assigned_planner"))
    private Employee assignedLeadPlanner;

    /** Authoritative account association used by project conversion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_client_id",
            foreignKey = @ForeignKey(name = "fk_enquiry_primary_client"))
    private Client primaryClient;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_link_source", length = 20)
    private ClientLinkSource clientLinkSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_linked_by_user_id",
            foreignKey = @ForeignKey(name = "fk_enquiry_client_linked_by"))
    private User clientLinkedBy;

    @Column(name = "client_linked_at")
    private Instant clientLinkedAt;

    /** Minimal package snapshot until a reusable package-template module exists. */
    @Column(name = "selected_package", length = 200)
    private String selectedPackage;

    @Column(name = "package_description", length = 1000)
    private String packageDescription;

    /** Final agreed agency contract amount, INR. */
    @Column(name = "agreed_amount", precision = 14, scale = 2)
    private BigDecimal agreedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnquiryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private EnquirySource source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
