package msteam.department_service.service;

import java.util.List;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import msteam.department_service.client.EmployeeClient;
import msteam.department_service.dto.EmployeeDto;
import msteam.department_service.exception.EmployeeServiceUnavailableException;
import msteam.department_service.exception.TooManyConcurrentRequestsException;

@Slf4j
@Service
public class EmployeeIntegrationService {

	private final EmployeeClient employeeClient;

	public EmployeeIntegrationService(EmployeeClient employeeClient) {
		this.employeeClient = employeeClient;
	}

	@CircuitBreaker(name = "employeeService", fallbackMethod = "getEmployeesByDepartmentIdFallback")
	@Retry(name = "employeeService")
	@Bulkhead(name = "employeeService", fallbackMethod = "getEmployeesByDepartmentIdBulkheadFallback")
	public List<EmployeeDto> getEmployeesByDepartmentId(Long departmentId) {
		log.info("Calling employee-service for departmentId={} on thread={}", departmentId, Thread.currentThread().getName());
		List<EmployeeDto> result = employeeClient.getEmployeesByDepartmentId(departmentId);
		log.info("employee-service responded for departmentId={}", departmentId);
		return result;
	}

	private List<EmployeeDto> getEmployeesByDepartmentIdFallback(Long departmentId, Throwable throwable) {
		log.warn("CircuitBreaker/Retry fallback triggered for departmentId={}: {}", departmentId, throwable.toString());
		throw new EmployeeServiceUnavailableException(throwable);
	}

	private List<EmployeeDto> getEmployeesByDepartmentIdBulkheadFallback(Long departmentId, BulkheadFullException exception) {
		log.warn("Bulkhead fallback triggered for departmentId={}: {}", departmentId, exception.toString());
		throw new TooManyConcurrentRequestsException(exception);
	}

}
