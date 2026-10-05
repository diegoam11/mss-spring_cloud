package com.msteam.employee_service.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.msteam.employee_service.model.Employee;

@Service
public class EmployeeService {

	private final Map<Long, Employee> employees = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(1);

	public Collection<Employee> getAllEmployees() {
		return employees.values();
	}

	public Optional<Employee> getEmployeeById(Long id) {
		return Optional.ofNullable(employees.get(id));
	}

	public List<Employee> getEmployeesByDepartmentId(Long departmentId) {
		/*
		try {
			Thread.sleep(2000); // simula latencia para probar el @Bulkhead de department-service
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		*/
		return employees.values().stream()
				.filter(employee -> departmentId.equals(employee.getDepartmentId()))
				.toList();
	}

	public Employee createEmployee(Employee employee) {
		employee.setId(nextId.getAndIncrement());
		employees.put(employee.getId(), employee);
		return employee;
	}

}
