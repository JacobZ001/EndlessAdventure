package com.endlessadventure.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** LLM client for interacting with the Gemini API
 * This class is developed by Cursor AI and modified by me to fit my needs.
 */
public final class LlmClient {
	static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
	private static final String MODEL_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

	private final HttpClient http;
	private final String apiKey;
	private final String model;
	private final String endpointOverride;

	public LlmClient() {
		this(System.getenv("ENDLESS_ADVENTURE_API_KEY"),
				System.getenv("ENDLESS_ADVENTURE_MODEL"),
				System.getenv("ENDLESS_ADVENTURE_API_URL"));
	}

	LlmClient(String apiKey, String model, String endpointOverride) {
		this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
		this.endpointOverride = endpointOverride == null ? "" : endpointOverride.trim();
	}

	/** Generate a response from the LLM
	 * @param systemInstruction The system instruction to use for the request
	 * @param userContent The user content to use for the request
	 * @return The response from the LLM
	 * @throws LlmRequestException If the request fails
	 */
	public String generate(String systemInstruction, String userContent) throws LlmRequestException {
		if (apiKey.isEmpty()) {
			throw new LlmRequestException(LlmRequestException.Stage.MISSING_KEY,
					"ENDLESS_ADVENTURE_API_KEY is not set");
		}
		if (userContent == null || userContent.isBlank()) {
			throw new LlmRequestException(LlmRequestException.Stage.INVALID_RESPONSE, "User content is empty");
		}

		String body = requestBody(systemInstruction, userContent);
		HttpRequest request = HttpRequest.newBuilder(endpoint())
				.timeout(REQUEST_TIMEOUT)
				.header("Content-Type", "application/json")
				.header("x-goog-api-key", apiKey)
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> response;
		try {
			response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (HttpTimeoutException e) {
			throw new LlmRequestException(LlmRequestException.Stage.TIMEOUT, "Gemini request timed out", e);
		} catch (IOException e) {
			throw new LlmRequestException(LlmRequestException.Stage.HTTP_ERROR, "Gemini request failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmRequestException(LlmRequestException.Stage.HTTP_ERROR, "Gemini request was interrupted", e);
		}
		if (response.statusCode() / 100 != 2) {
			throw new LlmRequestException(LlmRequestException.Stage.HTTP_ERROR,
					"Gemini returned HTTP " + response.statusCode() + ": " + brief(response.body()));
		}
		return extractReply(response.body());
	}

	/** Get the endpoint for the LLM
	 * @return The endpoint for the LLM
	 * @throws LlmRequestException If the endpoint is invalid
	 */
	private URI endpoint() throws LlmRequestException {
		String url = endpointOverride.isEmpty() ? MODEL_URL + model + ":generateContent" : endpointOverride;
		try {
			return URI.create(url);
		} catch (IllegalArgumentException e) {
			throw new LlmRequestException(LlmRequestException.Stage.INVALID_RESPONSE, "Gemini URL is invalid", e);
		}
	}

	/** Generate the request body for the LLM
	 * @param systemInstruction The system instruction to use for the request
	 * @param userContent The user content to use for the request
	 * @return The request body for the LLM
	 */
	private static String requestBody(String systemInstruction, String userContent) {
		String user = "{\"role\":\"user\",\"parts\":[{\"text\":\"" + escape(userContent) + "\"}]}";
		if (systemInstruction == null || systemInstruction.isBlank()) {
			return "{\"contents\":[" + user + "]}";
		}
		return "{\"systemInstruction\":{\"parts\":[{\"text\":\"" + escape(systemInstruction)
				+ "\"}]},\"contents\":[" + user + "]}";
	}

	/** Extract the reply from the JSON response
	 * @param json The JSON response from the LLM
	 * @return The reply from the LLM
	 * @throws LlmRequestException If the reply is invalid
	 */
	static String extractReply(String json) throws LlmRequestException {
		try {
			JsonCursor cursor = new JsonCursor(json);
			String text = cursor.replyText();
			if (text.isBlank()) {
				throw new LlmRequestException(LlmRequestException.Stage.INVALID_RESPONSE, "Gemini response had no text");
			}
			return text;
		} catch (LlmRequestException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new LlmRequestException(LlmRequestException.Stage.INVALID_RESPONSE, "Gemini response could not be read", e);
		}
	}

	/** Get a brief summary of the body
	 * @param body The body to summarize
	 * @return A brief summary of the body
	 */
	private String brief(String body) {
		if (body == null || body.isBlank()) {
			return "(empty body)";
		}
		String cleaned = body.replace(apiKey, "[redacted]").replace('\n', ' ').replace('\r', ' ').trim();
		return cleaned.length() <= 180 ? cleaned : cleaned.substring(0, 180);
	}

	/** Escape the value for the JSON request
	 * @param value The value to escape
	 * @return The escaped value
	 */
	static String escape(String value) {
		StringBuilder out = new StringBuilder(value.length() + 8);
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '\\' -> out.append("\\\\");
			case '"' -> out.append("\\\"");
			case '\n' -> out.append("\\n");
			case '\r' -> out.append("\\r");
			case '\t' -> out.append("\\t");
			default -> {
				if (c < 0x20) {
					out.append(String.format("\\u%04x", (int) c));
				} else {
					out.append(c);
				}
			}
		}
		}
		return out.toString();
	}

	/** JSON cursor for reading the response from the LLM
	 * @param json The JSON response from the LLM
	 * @return The JSON cursor
	 */
	private static final class JsonCursor {
		private final String json;
		private int i;

		/** Initialize the JSON cursor
		 * @param json The JSON response from the LLM
		 */
		JsonCursor(String json) {
			this.json = json == null ? "" : json;
		}

		/** Get the reply text from the JSON response
		 * @return The reply text from the JSON response
		 */
		String replyText() {
			expect('{');
			String text = null;
			while (peek() != '}') {
				String key = readString();
				expect(':');
				if ("candidates".equals(key)) {
					text = readCandidates();
				} else {
					skipValue();
				}
				if (peek() == ',') {
					i++;
				}
			}
			expect('}');
			return text == null ? "" : text;
		}

		/** Read the candidates from the JSON response
		 * @return The candidates from the JSON response
		 */
		private String readCandidates() {
			expect('[');
			if (peek() == ']') {
				i++;
				return "";
			}
			String text = readCandidate();
			while (peek() != ']') {
				expect(',');
				skipValue();
			}
			expect(']');
			return text;
		}

		/** Read the candidate from the JSON response
		 * @return The candidate from the JSON response
		 */
		private String readCandidate() {
			expect('{');
			String text = "";
			while (peek() != '}') {
				String key = readString();
				expect(':');
				if ("content".equals(key)) {
					text = readContent();
				} else {
					skipValue();
				}
				if (peek() == ',') {
					i++;
				}
			}
			expect('}');
			return text;
		}

		/** Read the content from the JSON response
		 * @return The content from the JSON response
		 */
		private String readContent() {
			expect('{');
			String text = "";
			while (peek() != '}') {
				String key = readString();
				expect(':');
				if ("parts".equals(key)) {
					text = readParts();
				} else {
					skipValue();
				}
				if (peek() == ',') {
					i++;
				}
			}
			expect('}');
			return text;
		}

		/** Read the parts from the JSON response
		 * @return The parts from the JSON response
		 */
		private String readParts() {
			expect('[');
			StringBuilder text = new StringBuilder();
			if (peek() != ']') {
				appendPart(text);
				while (peek() == ',') {
					i++;
					appendPart(text);
				}
			}
			expect(']');
			return text.toString();
		}

		/** Append the part to the text
		 * @param text The text to append the part to
		 */
		private void appendPart(StringBuilder text) {
			expect('{');
			String partText = null;
			boolean thought = false;
			while (peek() != '}') {
				String key = readString();
				expect(':');
				if ("text".equals(key) && peek() == '"') {
					partText = readString();
				} else if ("thought".equals(key) && peek() == 't') {
					thought = readTrue();
				} else {
					skipValue();
				}
				if (peek() == ',') {
					i++;
				}
			}
			expect('}');
			if (!thought && partText != null) {
				text.append(partText);
			}
		}

		/** Read the true value from the JSON response
		 * @return The true value from the JSON response
		 */
		private boolean readTrue() {
			skipWs();
			if (json.startsWith("true", i)) {
				i += 4;
				return true;
			}
			skipValue();
			return false;
		}

		/** Skip the value from the JSON response
		 * @throws IllegalArgumentException If the value is invalid
		 */
		private void skipValue() {
			char c = peek();
			if (c == '"') {
				readString();
			} else if (c == '{') {
				skipObject();
			} else if (c == '[') {
				skipArray();
			} else if (c == 't' || c == 'f' || c == 'n') {
				while (i < json.length() && Character.isLetter(json.charAt(i))) {
					i++;
				}
			} else if (c == '-' || Character.isDigit(c)) {
				while (i < json.length() && "0123456789+-.eE".indexOf(json.charAt(i)) >= 0) {
					i++;
				}
			} else {
				throw new IllegalArgumentException("Unexpected JSON at " + i);
			}
		}

		/** Skip the object from the JSON response
		 * @throws IllegalArgumentException If the object is invalid
		 */
		private void skipObject() {
			expect('{');
			while (peek() != '}') {
				readString();
				expect(':');
				skipValue();
				if (peek() == ',') {
					i++;
				}
			}
			expect('}');
		}

		/** Skip the array from the JSON response
		 * @throws IllegalArgumentException If the array is invalid
		 */
		private void skipArray() {
			expect('[');
			while (peek() != ']') {
				skipValue();
				if (peek() == ',') {
					i++;
				}
			}
			expect(']');
		}

		/** Read the string from the JSON response
		 * @return The string from the JSON response
		 * @throws IllegalArgumentException If the string is invalid
		 */
		private String readString() {
			expect('"');
			StringBuilder out = new StringBuilder();
			while (i < json.length()) {
				char c = json.charAt(i++);
				if (c == '"') {
					return out.toString();
				}
				if (c != '\\') {
					out.append(c);
					continue;
				}
				if (i >= json.length()) {
					break;
				}
				char escaped = json.charAt(i++);
				switch (escaped) {
				case '"', '\\', '/' -> out.append(escaped);
				case 'b' -> out.append('\b');
				case 'f' -> out.append('\f');
				case 'n' -> out.append('\n');
				case 'r' -> out.append('\r');
				case 't' -> out.append('\t');
				case 'u' -> out.append(readHex());
				default -> throw new IllegalArgumentException("Bad JSON escape");
				}
			}
			throw new IllegalArgumentException("Unclosed JSON string");
		}

		/** Read the hex value from the JSON response
		 * @return The hex value from the JSON response
		 * @throws IllegalArgumentException If the hex value is invalid
		 */
		private char readHex() {
			if (i + 4 > json.length()) {
				throw new IllegalArgumentException("Short JSON unicode escape");
			}
			int value = Integer.parseInt(json.substring(i, i + 4), 16);
			i += 4;
			return (char) value;
		}

		/** Peek the next character from the JSON response
		 * @return The next character from the JSON response
		 * @throws IllegalArgumentException If the next character is invalid
		 */
		private char peek() {
			skipWs();
			if (i >= json.length()) {
				throw new IllegalArgumentException("Unexpected end of JSON");
			}
			return json.charAt(i);
		}

		/** Expect the next character from the JSON response
		 * @param c The character to expect
		 * @throws IllegalArgumentException If the next character is invalid
		 */
		private void expect(char c) {
			if (peek() != c) {
				throw new IllegalArgumentException("Expected '" + c + "'");
			}
			i++;
		}

		/** Skip the whitespace from the JSON response
		 * @throws IllegalArgumentException If the whitespace is invalid
		 */
		private void skipWs() {
			while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
				i++;
			}
		}
	}
}
