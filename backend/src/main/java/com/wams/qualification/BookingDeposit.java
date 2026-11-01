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

import java.math.BigDecimal;
import java.time.LocalDate;

/** Persisted evidence that an enquiry's booking deposit was received. */
@Entity
@Table(name = "booking_deposits", uniqueConstraints = {
        @UniqueConstraint(name = "uk_booking_deposits_enquiry", columnNames = "enquiry_id"),
        @UniqueConstraint(name = "uk_booking_deposits_reference", columnNames = "payment_reference")
})
@Check(constraints = "amount > 0 and (status <> 'PAID' or (paid_on is not null and payment_method is not null and payment_reference is not null))")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDeposit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_deposit_enquiry"))
    private Enquiry enquiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingDepositStatus status;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_deposit_recorded_by"))
    private User recordedBy;
}
