package com.endlessadventure;

public class BadSaveException extends RuntimeException {
	private final int slot;
	
	public BadSaveException(int slot, Throwable cause) {
		super("Bad save at slot: " + slot, cause);
		this.slot = slot;
	}

	public BadSaveException(int slot, String message, Throwable cause) {
		super(message, cause);
		this.slot = slot;
	}
	
	public BadSaveException(int slot, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.slot = slot;
	}

	public int getSlot() {
		return slot;
	}
}
