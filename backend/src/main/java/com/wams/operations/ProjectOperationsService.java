package com.wams.operations;

import com.wams.entity.Employee;
import com.wams.entity.User;
import com.wams.entity.WeddingProject;
import com.wams.enums.Role;
import com.wams.exception.BusinessRuleException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.operations.dto.*;
import com.wams.repository.EmployeeRepository;
import com.wams.repository.WeddingProjectRepository;
import com.wams.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectOperationsService {
    private final WeddingProjectRepository projectRepository;
    private final WeddingFunctionRepository functionRepository;
    private final ProjectTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final AuthenticatedUserProvider userProvider;

    public OperationsResponse getOperations(Long projectId) {
        requireStaffProject(projectId);
        List<FunctionResponse> functions = functionRepository.findAllByProjectIdOrderByFunctionDateAscStartTimeAsc(projectId).stream().map(FunctionResponse::from).toList();
        List<TaskResponse> tasks = taskRepository.findAllByProjectIdOrderByDueDateAsc(projectId).stream().map(TaskResponse::from).toList();
        int completed = (int) tasks.stream().filter(t -> t.status() == TaskStatus.COMPLETED).count();
        int percentage = tasks.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / tasks.size());
        return new OperationsResponse(functions, tasks, completed, tasks.size(), percentage);
    }

    @Transactional
    public FunctionResponse createFunction(Long projectId, CreateFunctionRequest request) {
        WeddingProject project = requireStaffProject(projectId);
        if (!request.endTime().isAfter(request.startTime())) throw new BusinessRuleException("Function end time must be after start time");
        return FunctionResponse.from(functionRepository.save(WeddingFunction.builder()
                .project(project).name(request.name().trim()).functionDate(request.functionDate())
                .startTime(request.startTime()).endTime(request.endTime()).venue(request.venue().trim()).build()));
    }

    @Transactional
    public TaskResponse createTask(Long projectId, CreateTaskRequest request) {
        WeddingProject project = requireStaffProject(projectId);
        WeddingFunction function = request.functionId() == null ? null : functionRepository.findByIdAndProjectId(request.functionId(), projectId)
                .orElseThrow(() -> new BusinessRuleException("Function does not belong to this project"));
        Employee assignee = employeeRepository.findById(request.assignedEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.assignedEmployeeId()));
        if (!assignee.isActive() || !assignee.getUser().isActive()) throw new BusinessRuleException("Assigned employee account must be active");
        return TaskResponse.from(taskRepository.save(ProjectTask.builder()
                .project(project).function(function).assignedEmployee(assignee).title(request.title().trim())
                .description(trim(request.description())).dueDate(request.dueDate()).status(TaskStatus.PENDING).build()));
    }

    public List<TaskResponse> myTasks() {
        User user = userProvider.requireCurrentUser();
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("No Employee profile linked to current user"));
        if (!employee.isActive()) throw new AccessDeniedException("Employee profile is inactive");
        return taskRepository.findAllByAssignedEmployeeIdOrderByDueDateAsc(employee.getId()).stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest request) {
        User user = userProvider.requireCurrentUser();
        ProjectTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        if (user.getRole() == Role.EMPLOYEE) {
            Employee employee = employeeRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("No Employee profile linked to current user"));
            if (!employee.getId().equals(task.getAssignedEmployee().getId())) throw new AccessDeniedException("Task is not assigned to current employee");
        } else if (user.getRole() == Role.LEAD_PLANNER) {
            if (!task.getProject().getLeadPlanner().getUser().getId().equals(user.getId())) throw new AccessDeniedException("Project is not assigned to current planner");
        } else if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Role cannot update tasks");
        }
        task.setStatus(request.status());
        return TaskResponse.from(task);
    }

    private WeddingProject requireStaffProject(Long projectId) {
        User user = userProvider.requireCurrentUser();
        WeddingProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("WeddingProject not found: " + projectId));
        if (user.getRole() == Role.ADMIN) return project;
        if (user.getRole() == Role.LEAD_PLANNER && project.getLeadPlanner().getUser().getId().equals(user.getId())) return project;
        throw new AccessDeniedException("Project is not available to current staff user");
    }

    private static String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
