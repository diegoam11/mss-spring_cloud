package msteam.department_service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "employee-service", path = "/api/v1/employees")
public interface EmployeeClient {

	@GetMapping("/department/{departmentId}")
	List<EmployeeDto> getEmployeesByDepartmentId(@PathVariable Long departmentId);

}
