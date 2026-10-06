package com.endlessadventure.llm;

public class LlmRequestException extends Exception {
	private static final long serialVersionUID = 1L;

	public enum Stage {
		MISSING_KEY, TIMEOUT, HTTP_ERROR, INVALID_RESPONSE
	}

	private final Stage stage;

	public LlmRequestException(Stage stage, String message) {
		super(message);
		this.stage = stage;
	}

	public LlmRequestException(Stage stage, String message, Throwable cause) {
		super(message, cause);
		this.stage = stage;
	}

	public Stage getStage() {
		return stage;
	}
}
