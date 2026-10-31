package com.wams.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "clients",
        uniqueConstraints = @UniqueConstraint(name = "uk_clients_user", columnNames = "user_id"))
public class Client extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Each Client is backed by exactly one User account.
     * Primary vs partner distinction is derived from the User's role
     * (PRIMARY_CLIENT or PARTNER_CLIENT).
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_clients_user"))
    private User user;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "address", length = 500)
    private String address;
}
