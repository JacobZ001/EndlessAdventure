package com.endlessadventure.save;

public class MissingFieldException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final String field;

	public MissingFieldException(String field) {
		super("Field not found: " + field);
		this.field = field;
	}

	public String getField() {
		return field;
	}

}
