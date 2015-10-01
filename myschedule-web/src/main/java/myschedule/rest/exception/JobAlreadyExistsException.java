package myschedule.rest.exception;


public class JobAlreadyExistsException extends Exception {
  
	private static final long serialVersionUID = 1L;

	public JobAlreadyExistsException(String message) {
        super(message);
    }
}
