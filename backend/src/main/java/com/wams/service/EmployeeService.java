package com.wams.service;

import com.wams.dto.request.CreateEmployeeRequest;
import com.wams.dto.request.UpdateEmployeeRequest;
import com.wams.dto.response.EmployeeResponse;
import com.wams.entity.Employee;

import java.util.List;

public interface EmployeeService {
    EmployeeResponse create(CreateEmployeeRequest request);
    EmployeeResponse getById(Long id);
    Employee getEntityById(Long id);
    List<EmployeeResponse> list();
    EmployeeResponse update(Long id, UpdateEmployeeRequest request);
}
