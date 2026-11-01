package com.wams.enquiry;

import com.wams.entity.BaseEntity;
import com.wams.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/** Append-only contact/follow-up note for an internal enquiry. */
@Entity
@Table(name = "enquiry_follow_ups", indexes =
        @Index(name = "idx_followups_enquiry_occurred", columnList = "enquiry_id,occurred_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnquiryFollowUp extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_followup_enquiry"))
    private Enquiry enquiry;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "notes", nullable = false, length = 2000)
    private String notes;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_followup_recorded_by"))
    private User recordedBy;
}
