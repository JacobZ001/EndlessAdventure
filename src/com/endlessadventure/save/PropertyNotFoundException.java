package com.endlessadventure.save;

public class PropertyNotFoundException extends RuntimeException {
	private final String propertyName;

	public PropertyNotFoundException(String propertyName) {
		super("Property not found: " + propertyName);
		this.propertyName = propertyName;
	}

	public String getPropertyName() {
		return propertyName;
	}

}
