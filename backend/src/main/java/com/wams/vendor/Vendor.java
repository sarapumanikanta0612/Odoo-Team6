package com.wams.vendor;

import com.wams.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optional during directory-only onboarding; unique when a login is linked. */
    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true,
            foreignKey = @ForeignKey(name = "fk_vendors_user"))
    private User user;

    @Column(nullable = false, length = 150)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VendorCategory category;

    @Column(length = 100)
    private String contactPerson;

    /** Contact email, stored lower-case. Not the login email (that lives on DEV 1's User). */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String city;

    @Column(length = 255)
    private String address;

    @Column(length = 2000)
    private String serviceDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VendorStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
