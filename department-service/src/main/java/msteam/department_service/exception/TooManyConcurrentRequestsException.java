package msteam.department_service.exception;

public class TooManyConcurrentRequestsException extends RuntimeException {

	public TooManyConcurrentRequestsException(Throwable cause) {
		super("too many concurrent calls to employee-service", cause);
	}

}
