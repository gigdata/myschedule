package myschedule.rest.domain;


public class BasicResponse {

	private boolean success;
	private String msg;
	private String redirect;

	public BasicResponse() {
	}

	public BasicResponse(boolean success) {
		this.success = success;
	}

	public BasicResponse(boolean success, String msg) {
		this.success = success;
		this.msg = msg;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getRedirect() {
		return redirect;
	}

	public void setRedirect(String redirect) {
		this.redirect = redirect;
	}

}
