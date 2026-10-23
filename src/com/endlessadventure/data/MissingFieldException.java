package com.endlessadventure.data;

public class MissingFieldException extends Exception {
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
