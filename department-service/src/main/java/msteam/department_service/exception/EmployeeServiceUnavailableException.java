package msteam.department_service.exception;

public class EmployeeServiceUnavailableException extends RuntimeException {

	public EmployeeServiceUnavailableException(Throwable cause) {
		super("employee-service is unavailable", cause);
	}

}
