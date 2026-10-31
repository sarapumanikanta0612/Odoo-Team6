package com.wams.enquiry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An enquiry from a prospective client (SRS 6.1, 6.3, 6.4).
 * <p>
 * It remains an enquiry until a wedding project is created from it. Planner assignment
 * and follow-ups are added in a later phase, once the shared User entity exists.
 */
@Entity
@Table(name = "enquiries", indexes = {
		@Index(name = "idx_enquiries_status", columnList = "status"),
		@Index(name = "idx_enquiries_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Enquiry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Contact details

	@Column(name = "contact_name", nullable = false, length = 120)
	private String contactName;

	@Column(name = "email", nullable = false, length = 254)
	private String email;

	@Column(name = "phone", nullable = false, length = 30)
	private String phone;

	// Wedding details

	@Column(name = "wedding_start_date")
	private LocalDate weddingStartDate;

	@Column(name = "wedding_end_date")
	private LocalDate weddingEndDate;

	@Column(name = "destination", length = 150)
	private String destination;

	@Column(name = "estimated_guest_count")
	private Integer estimatedGuestCount;

	/** Lower end of the client's budget range, in INR. */
	@Column(name = "budget_min", precision = 12, scale = 2)
	private BigDecimal budgetMin;

	/** Upper end of the client's budget range, in INR. */
	@Column(name = "budget_max", precision = 12, scale = 2)
	private BigDecimal budgetMax;

	// Requirements

	@Column(name = "services_of_interest", length = 1000)
	private String servicesOfInterest;

	@Column(name = "theme_style", length = 200)
	private String themeStyle;

	@Column(name = "functions_required", length = 500)
	private String functionsRequired;

	@Column(name = "special_requests", length = 2000)
	private String specialRequests;

	/** Free-text message from the enquirer. */
	@Column(name = "message", length = 2000)
	private String message;

	/** Requested consultation date. This is a request, not a confirmed booking. */
	@Column(name = "preferred_consultation_date")
	private LocalDate preferredConsultationDate;

	// Tracking

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
