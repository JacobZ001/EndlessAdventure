package com.endlessadventure.ui;

/** thrown when the terminal's input ends; Driver catches it to close the game normally */
public class InputClosedException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private String key;

	public InputClosedException(String key) {
		super("Input closed: Key: " + key);
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
