package msteam.department_service.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import msteam.department_service.model.Department;

@Service
public class DepartmentService {

	private final Map<Long, Department> departments = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(1);

	public Collection<Department> getAllDepartments() {
		return departments.values();
	}

	public Optional<Department> getDepartmentById(Long id) {
		return Optional.ofNullable(departments.get(id));
	}

	public boolean existsById(Long id) {
		return departments.containsKey(id);
	}

	public Department createDepartment(Department department) {
		department.setId(nextId.getAndIncrement());
		departments.put(department.getId(), department);
		return department;
	}

	public Optional<Department> updateDepartment(Long id, Department department) {
		if (!departments.containsKey(id)) {
			return Optional.empty();
		}
		department.setId(id);
		departments.put(id, department);
		return Optional.of(department);
	}

	public boolean deleteDepartment(Long id) {
		return departments.remove(id) != null;
	}

}
