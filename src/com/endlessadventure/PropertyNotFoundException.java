package com.endlessadventure;

public class PropertyNotFoundException extends RuntimeException {
	private final String propertyName;

	public PropertyNotFoundException(String propertyName) {
		super("Property not found: " + propertyName);
		this.propertyName = propertyName;
	}

	public PropertyNotFoundException(String propertyName, String message) {
		super(message);
		this.propertyName = propertyName;
	}

	public PropertyNotFoundException(String propertyName, String message, Throwable cause) {
		super(message, cause);
		this.propertyName = propertyName;
	}

	public PropertyNotFoundException(String propertyName, String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.propertyName = propertyName;
	}

	public String getPropertyName() {
		return propertyName;
	}

}
