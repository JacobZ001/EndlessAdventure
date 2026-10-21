package com.endlessadventure.save;

public class BadSaveException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private final int slot;
	
	public BadSaveException(int slot, Throwable cause) {
		super("Bad save at slot: " + slot + ": " + cause.getMessage(), cause);
		this.slot = slot;
	}
	
	public BadSaveException(int slot, String message) {
		super(message);
		this.slot = slot;
	}

	public int getSlot() {
		return slot;
	}
}
