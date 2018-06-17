package com.nitin.json.patch.exception;

public class JsonPatchException extends Exception {

	public JsonPatchException() {
		// TODO Auto-generated constructor stub
	}
	
	public JsonPatchException(String errorMsg) {
		super(errorMsg);
	}
	
	public JsonPatchException(String errorMsg, Exception e) {
		super(errorMsg, e);
	}

}
