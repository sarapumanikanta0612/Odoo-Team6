package com.wams.service.impl;

import com.wams.dto.request.CreateEmployeeRequest;
import com.wams.dto.request.UpdateEmployeeRequest;
import com.wams.dto.response.EmployeeResponse;
import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.exception.BusinessRuleException;
import com.wams.exception.DuplicateResourceException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.UserRepository;
import com.wams.service.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Set<Role> STAFF_ROLES =
            Set.of(Role.ADMIN, Role.LEAD_PLANNER, Role.EMPLOYEE);

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest r) {
        User user = userRepository.findById(r.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + r.userId()));
        if (!STAFF_ROLES.contains(user.getRole())) {
            throw new BusinessRuleException(
                    "Employee record requires a user with ADMIN, LEAD_PLANNER or EMPLOYEE role");
        }
        if (employeeRepository.existsByUserId(user.getId())) {
            throw new DuplicateResourceException("Employee already exists for userId=" + user.getId());
        }
        if (employeeRepository.existsByEmployeeCode(r.employeeCode())) {
            throw new DuplicateResourceException("Employee code already in use: " + r.employeeCode());
        }
        Employee e = Employee.builder()
                .user(user)
                .employeeCode(r.employeeCode().trim())
                .designation(r.designation())
                .department(r.department())
                .joiningDate(r.joiningDate())
                .active(r.active() == null ? Boolean.TRUE : r.active())
                .build();
        return EmployeeResponse.from(employeeRepository.save(e));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return EmployeeResponse.from(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEntityById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> list() {
        return employeeRepository.findAll().stream().map(EmployeeResponse::from).toList();
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest r) {
        Employee e = getEntityById(id);
        if (r.designation() != null) e.setDesignation(r.designation());
        if (r.department() != null) e.setDepartment(r.department());
        if (r.joiningDate() != null) e.setJoiningDate(r.joiningDate());
        if (r.active() != null) e.setActive(r.active());
        return EmployeeResponse.from(e);
    }
}
