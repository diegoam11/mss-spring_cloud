package msteam.department_service.controller;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import msteam.department_service.dto.EmployeeDto;
import msteam.department_service.exception.EmployeeServiceUnavailableException;
import msteam.department_service.model.Department;
import msteam.department_service.service.DepartmentService;
import msteam.department_service.service.EmployeeIntegrationService;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

	private final DepartmentService departmentService;
	private final EmployeeIntegrationService employeeIntegrationService;

	public DepartmentController(DepartmentService departmentService, EmployeeIntegrationService employeeIntegrationService) {
		this.departmentService = departmentService;
		this.employeeIntegrationService = employeeIntegrationService;
	}

	@GetMapping
	@RateLimiter(name = "getAllDepartments", fallbackMethod = "getAllDepartmentsRateLimitFallback")
	public ResponseEntity<Collection<Department>> getAllDepartments() {
		return ResponseEntity.ok(departmentService.getAllDepartments());
	}

	private ResponseEntity<Collection<Department>> getAllDepartmentsRateLimitFallback(RequestNotPermitted exception) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
	}

	@GetMapping("/async")
	@TimeLimiter(name = "getAllDepartments", fallbackMethod = "getAllDepartmentsAsyncTimeoutFallback")
	public CompletableFuture<ResponseEntity<Collection<Department>>> getAllDepartmentsAsync() {
		return departmentService.getAllDepartmentsAsync().thenApply(ResponseEntity::ok);
	}

	private CompletableFuture<ResponseEntity<Collection<Department>>> getAllDepartmentsAsyncTimeoutFallback(TimeoutException exception) {
		return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Department> getDepartmentById(@PathVariable Long id) {
		return departmentService.getDepartmentById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
		return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(department));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
		return departmentService.updateDepartment(id, department)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
		if (!departmentService.deleteDepartment(id)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/employees")
	public ResponseEntity<List<EmployeeDto>> getEmployeesByDepartment(@PathVariable Long id) {
		if (!departmentService.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		try {
			return ResponseEntity.ok(employeeIntegrationService.getEmployeesByDepartmentId(id));
		} catch (EmployeeServiceUnavailableException exception) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(List.of());
		}
	}

}
