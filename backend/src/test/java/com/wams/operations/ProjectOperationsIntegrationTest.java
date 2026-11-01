package com.wams.operations;

import com.wams.entity.*;
import com.wams.enums.*;
import com.wams.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:wams_operations_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ProjectOperationsIntegrationTest {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired ClientRepository clients;
    @Autowired EmployeeRepository employees; @Autowired WeddingProjectRepository projects;
    @Autowired WeddingFunctionRepository functions; @Autowired ProjectTaskRepository tasks;
    private Long projectId; private Long employeeId; private Long otherEmployeeId;

    @BeforeEach void seed() {
        tasks.deleteAll(); functions.deleteAll(); projects.deleteAll(); clients.deleteAll(); employees.deleteAll(); users.deleteAll();
        User plannerUser=user("planner@ops.test",Role.LEAD_PLANNER); Employee planner=employee(plannerUser,"OPS-PLAN");
        User clientUser=user("client@ops.test",Role.PRIMARY_CLIENT); Client client=clients.save(Client.builder().user(clientUser).build());
        employeeId=employee(user("employee@ops.test",Role.EMPLOYEE),"OPS-EMP").getId();
        otherEmployeeId=employee(user("other@ops.test",Role.EMPLOYEE),"OPS-OTHER").getId();
        user("admin@ops.test",Role.ADMIN);
        projectId=projects.save(WeddingProject.builder().projectName("Operations Wedding").primaryClient(client).leadPlanner(planner)
                .weddingDate(LocalDate.now().plusMonths(2)).projectStatus(ProjectStatus.PLANNING)
                .agreementSigned(true).bookingDepositPaid(true).build()).getId();
    }

    @Test @WithMockUser(username="admin@ops.test",roles="ADMIN")
    void staffCreatesFunctionAndTask_progressUsesPersistedTaskStatus() throws Exception {
        String function=mvc.perform(post("/api/projects/"+projectId+"/functions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Wedding Ceremony\",\"functionDate\":\""+LocalDate.now().plusMonths(2)+"\",\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"venue\":\"Temple Hall\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.venue").value("Temple Hall"))
                .andReturn().getResponse().getContentAsString();
        long functionId=tools.jackson.databind.json.JsonMapper.builder().build().readTree(function).get("id").asLong();
        String task=mvc.perform(post("/api/projects/"+projectId+"/tasks").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Confirm ceremony setup\",\"functionId\":"+functionId+",\"assignedEmployeeId\":"+employeeId+",\"dueDate\":\""+LocalDate.now().plusDays(7)+"\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long taskId=tools.jackson.databind.json.JsonMapper.builder().build().readTree(task).get("id").asLong();
        mvc.perform(get("/api/projects/"+projectId+"/operations"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.functions.length()").value(1))
                .andExpect(jsonPath("$.tasks.length()").value(1)).andExpect(jsonPath("$.completionPercentage").value(0));
        mvc.perform(patch("/api/tasks/"+taskId+"/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/projects/"+projectId+"/operations"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completionPercentage").value(100));
    }

    @Test @WithMockUser(username="employee@ops.test",roles="EMPLOYEE")
    void assignedEmployeeSeesMineAndCanUpdate_onlyOwnTask() throws Exception {
        ProjectTask mine=task(employeeId,"Mine"); ProjectTask other=task(otherEmployeeId,"Other");
        mvc.perform(get("/api/tasks/mine")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].title").value("Mine"));
        mvc.perform(patch("/api/tasks/"+mine.getId()+"/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/tasks/"+other.getId()+"/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username="planner-other@ops.test",roles="LEAD_PLANNER")
    void unrelatedPlannerCannotAccessProjectOperations() throws Exception {
        users.save(User.builder().fullName("Other Planner").email("planner-other@ops.test").passwordHash("x").role(Role.LEAD_PLANNER).active(true).build());
        mvc.perform(get("/api/projects/"+projectId+"/operations")).andExpect(status().isForbidden());
    }

    private ProjectTask task(Long id,String title){return tasks.save(ProjectTask.builder().project(projects.findById(projectId).orElseThrow()).assignedEmployee(employees.findById(id).orElseThrow()).title(title).dueDate(LocalDate.now().plusDays(1)).status(TaskStatus.PENDING).build());}
    private User user(String email,Role role){return users.save(User.builder().fullName(email).email(email).passwordHash("x").role(role).active(true).build());}
    private Employee employee(User u,String code){return employees.save(Employee.builder().user(u).employeeCode(code).active(true).build());}
}
