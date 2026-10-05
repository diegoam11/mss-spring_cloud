package msteam.department_service.service;

import java.util.List;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import msteam.department_service.client.EmployeeClient;
import msteam.department_service.dto.EmployeeDto;
import msteam.department_service.exception.EmployeeServiceUnavailableException;

@Service
public class EmployeeIntegrationService {

	private final EmployeeClient employeeClient;

	public EmployeeIntegrationService(EmployeeClient employeeClient) {
		this.employeeClient = employeeClient;
	}

	@CircuitBreaker(name = "employeeService", fallbackMethod = "getEmployeesByDepartmentIdFallback")
	public List<EmployeeDto> getEmployeesByDepartmentId(Long departmentId) {
		return employeeClient.getEmployeesByDepartmentId(departmentId);
	}

	private List<EmployeeDto> getEmployeesByDepartmentIdFallback(Long departmentId, Throwable throwable) {
		throw new EmployeeServiceUnavailableException(throwable);
	}

}
