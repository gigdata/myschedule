package myschedule.rest.exception;


public class WebException extends Exception{
   
	private static final long serialVersionUID = 1L;

	public WebException(String message) {
        super(message);
    }
	
	public WebException() {
        super("Exception occuered in web application");
    }
}
