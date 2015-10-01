package myschedule.rest.exception;


public class BadCronExpressionException extends Exception{
    
	private static final long serialVersionUID = 1L;

	public BadCronExpressionException(String message) {
        super(message);
    }
}
