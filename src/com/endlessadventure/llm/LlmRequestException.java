package com.endlessadventure.llm;

public class LlmRequestException extends Exception {
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
