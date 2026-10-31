package com.wams.dto.response;

import com.wams.entity.Employee;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class EmployeeResponse {
    private final Long id;
    private final UserResponse user;
    private final String employeeCode;
    private final String designation;
    private final String department;
    private final LocalDate joiningDate;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static EmployeeResponse from(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .user(UserResponse.from(e.getUser()))
                .employeeCode(e.getEmployeeCode())
                .designation(e.getDesignation())
                .department(e.getDepartment())
                .joiningDate(e.getJoiningDate())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
