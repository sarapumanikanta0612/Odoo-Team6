package com.wams.qualification;

import com.wams.enquiry.Enquiry;
import com.wams.entity.BaseEntity;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Check;

import java.time.Instant;

/** Persisted evidence that an enquiry's service agreement was signed. */
@Entity
@Table(name = "agreements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_agreements_enquiry", columnNames = "enquiry_id"),
        @UniqueConstraint(name = "uk_agreements_reference", columnNames = "agreement_reference")
})
@Check(constraints = "status <> 'SIGNED' or (signed_by_name is not null and signed_at is not null)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_agreement_enquiry"))
    private Enquiry enquiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgreementStatus status;

    @Column(name = "agreement_reference", nullable = false, length = 100)
    private String agreementReference;

    @Column(name = "signed_by_name", length = 150)
    private String signedByName;

    @Column(name = "signed_at")
    private Instant signedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_agreement_recorded_by"))
    private User recordedBy;
}
