package msteam.department_service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

	private final Map<Long, Department> departments = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(1);
	private final EmployeeClient employeeClient;

	public DepartmentController(EmployeeClient employeeClient) {
		this.employeeClient = employeeClient;
	}

	@GetMapping
	public Collection<Department> getAllDepartments() {
		return departments.values();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Department> getDepartmentById(@PathVariable Long id) {
		Department department = departments.get(id);
		if (department == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(department);
	}

	@PostMapping
	public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
		department.setId(nextId.getAndIncrement());
		departments.put(department.getId(), department);
		return ResponseEntity.status(HttpStatus.CREATED).body(department);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
		if (!departments.containsKey(id)) {
			return ResponseEntity.notFound().build();
		}
		department.setId(id);
		departments.put(id, department);
		return ResponseEntity.ok(department);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
		if (departments.remove(id) == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/employees")
	public ResponseEntity<List<EmployeeDto>> getEmployeesByDepartment(@PathVariable Long id) {
		if (!departments.containsKey(id)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(employeeClient.getEmployeesByDepartmentId(id));
	}

}
