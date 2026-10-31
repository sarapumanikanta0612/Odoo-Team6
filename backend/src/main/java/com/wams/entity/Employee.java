package com.wams.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employees_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_employees_code", columnNames = "employee_code")
        })
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Each Employee is backed by exactly one User account.
     * The user's role is expected to be ADMIN, LEAD_PLANNER or EMPLOYEE.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employees_user"))
    private User user;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "active", nullable = false)
    private boolean active;
}
