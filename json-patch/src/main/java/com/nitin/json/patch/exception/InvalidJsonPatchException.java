package com.nitin.json.patch.exception;

import java.io.IOException;

public class InvalidJsonPatchException extends JsonPatchException {

	public InvalidJsonPatchException() {
		// TODO Auto-generated constructor stub
	}
	
	public InvalidJsonPatchException(String errorMsg) {
		super(errorMsg);
	}

	public InvalidJsonPatchException(String errorMsg, Exception e) {
		super(errorMsg, e);
	}

}
