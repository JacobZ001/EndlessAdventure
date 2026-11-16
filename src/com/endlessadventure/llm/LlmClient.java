package com.endlessadventure.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** LLM client for interacting with the Gemini API
 * This class is developed by Cursor AI and modified by me to fit my needs.
 */
public final class LlmClient {
	private static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
	private static final String MODEL_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
	private static final Gson GSON = new Gson();
	private static final int BRIEF_LENGTH = 80; //error details shown to the player end up on one status line

	private final HttpClient http;
	private final String apiKey;
	private final String model;
	private final String endpointOverride;
	private final Path debugLog;	//every request, reply and error in full, appended; null when off
	private boolean logFailed;		//set once the log cannot be written; later notes are skipped

	public LlmClient() {
		this(readLocalConfig());
	}

	private LlmClient(Properties config) {
		this(config.getProperty("ENDLESS_ADVENTURE_API_KEY", System.getenv("ENDLESS_ADVENTURE_API_KEY")),
				config.getProperty("ENDLESS_ADVENTURE_MODEL", System.getenv("ENDLESS_ADVENTURE_MODEL")),
				config.getProperty("ENDLESS_ADVENTURE_API_URL", System.getenv("ENDLESS_ADVENTURE_API_URL")),
				config.getProperty("ENDLESS_ADVENTURE_LLM_LOG", System.getenv("ENDLESS_ADVENTURE_LLM_LOG")));
	}

	LlmClient(String apiKey, String model, String endpointOverride) {
		this(apiKey, model, endpointOverride, null);
	}

	/** debugLog is a file path for the debug log, or null or blank for none */
	LlmClient(String apiKey, String model, String endpointOverride, String debugLog) {
		this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
		this.endpointOverride = endpointOverride == null ? "" : endpointOverride.trim();
		this.debugLog = debugLog == null || debugLog.isBlank() ? null : Path.of(debugLog.trim());
		note("CLIENT STARTED", "model " + this.model);
	}

	/** append a titled section to the debug log, if there is one; the API key never reaches the file,
	 *  and a log that cannot be written is skipped so it never stops the game */
	public void note(String title, String text) {
		if (debugLog == null || logFailed) {
			return;
		}
		String entry = "----- " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) + "  " + title + " -----\n"
				+ (text == null ? "" : text.strip()) + "\n\n";
		if (!apiKey.isEmpty()) {
			entry = entry.replace(apiKey, "[redacted]");
		}
		try {
			if (debugLog.getParent() != null) {
				Files.createDirectories(debugLog.getParent());
			}
			Files.writeString(debugLog, entry, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			logFailed = true; //the log is a debugging aid only: stop trying and let the game go on
		}
	}

	/** Read project-local settings without changing the operating system environment. */
	private static Properties readLocalConfig() {
		Properties config = new Properties();
		Path path = Path.of(".env");
		if (Files.notExists(path)) {
			return config;
		}
		// single-line NAME=VALUE only; add expansion or multiline syntax when actually needed.
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line;
			int lineNumber = 0;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (lineNumber == 1 && line.startsWith("\uFEFF")) {
					line = line.substring(1);
				}
				line = line.strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				int separator = line.indexOf('=');
				if (separator <= 0) {
					throw new IllegalArgumentException("Invalid .env entry at line " + lineNumber + "; expected NAME=VALUE");
				}
				String name = line.substring(0, separator).strip();
				if (!name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
					throw new IllegalArgumentException("Invalid .env variable name at line " + lineNumber);
				}
				String value = line.substring(separator + 1).strip();
				if (value.startsWith("\"") || value.startsWith("'")) {
					if (value.length() < 2 || value.charAt(value.length() - 1) != value.charAt(0)) {
						throw new IllegalArgumentException("Unclosed .env quote at line " + lineNumber);
					}
					value = value.substring(1, value.length() - 1);
				}
				config.setProperty(name, value);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot read .env in the working directory", e);
		}
		return config;
	}

	/** Generate a response from the LLM
	 * @param systemInstruction The system instruction to use for the request
	 * @param userContent The user content to use for the request
	 * @return The response from the LLM
	 * @throws LlmRequestException If the request fails
	 */
	public String generate(String systemInstruction, String userContent) throws LlmRequestException {
		return generate(systemInstruction, userContent, null);
	}

	/** Generate a JSON reply that follows a JSON Schema (Gemini structured output)
	 * @param systemInstruction The system instruction to use for the request
	 * @param userContent The user content to use for the request
	 * @param responseSchema The JSON Schema the reply must follow, or null for plain text
	 * @return The response from the LLM
	 * @throws LlmRequestException If the request fails
	 */
	public String generate(String systemInstruction, String userContent, Map<String, Object> responseSchema)
			throws LlmRequestException {
		if (apiKey.isEmpty()) {
			throw new LlmRequestException(LlmRequestException.Stage.MISSING_KEY,
					"ENDLESS_ADVENTURE_API_KEY is not set in .env or the process environment");
		}
		if (userContent == null || userContent.isBlank()) {
			throw new LlmRequestException(LlmRequestException.Stage.INVALID_RESPONSE, "User content is empty");
		}

		note("SYSTEM", systemInstruction);
		note("USER", userContent);
		String body = requestBody(systemInstruction, userContent, responseSchema);
		HttpRequest request = HttpRequest.newBuilder(endpoint())
				.timeout(REQUEST_TIMEOUT)
				.header("Content-Type", "application/json")
				.header("x-goog-api-key", apiKey)
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> response;
		long start = System.nanoTime();
		try {
			response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (HttpTimeoutException e) {
			note("FAILED", "timed out: " + e);
			throw new LlmRequestException(LlmRequestException.Stage.TIMEOUT, "Gemini request timed out", e);
		} catch (IOException e) {
			note("FAILED", e.toString());
			throw new LlmRequestException(LlmRequestException.Stage.HTTP_ERROR, "Gemini request failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmRequestException(LlmRequestException.Stage.HTTP_ERROR, "Gemini request was interrupted", e);
		}
		long millis = (System.nanoTime() - start) / 1_000_000;
		if (response.statusCode() / 100 != 2) {
			note("HTTP " + response.statusCode() + " after " + millis + " ms (full body)", response.body());
			throw new LlmRequestException(LlmRequestException.Stage.HTTP_ERROR,
					"Gemini returned HTTP " + response.statusCode() + ": " + brief(response.body()));
		}
		try {
			String reply = extractReply(response.body());
			note("REPLY after " + millis + " ms", reply);
			return reply;
		} catch (LlmRequestException e) {
			note("UNREADABLE REPLY (full body)", response.body());
			throw e;
		}
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
	 * @param responseSchema The JSON Schema for the reply, or null for plain text
	 * @return The request body for the LLM
	 */
	private static String requestBody(String systemInstruction, String userContent, Map<String, Object> responseSchema) {
		Map<String, Object> body = new HashMap<>();
		body.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userContent)))));
		if (systemInstruction != null && !systemInstruction.isBlank()) {
			body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
		}
		if (responseSchema != null) {
			body.put("generationConfig", Map.of("responseMimeType", "application/json", "responseJsonSchema", responseSchema));
		}
		return GSON.toJson(body);
	}

	/** Extract the reply from the JSON response, joining the first candidate's non-thought text parts
	 * @param json The JSON response from the LLM
	 * @return The reply from the LLM
	 * @throws LlmRequestException If the reply is invalid
	 */
	static String extractReply(String json) throws LlmRequestException {
		StringBuilder text = new StringBuilder();
		try {
			JsonArray candidates = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("candidates");
			JsonObject content = candidates == null || candidates.isEmpty() ? null
					: candidates.get(0).getAsJsonObject().getAsJsonObject("content");
			JsonArray parts = content == null ? null : content.getAsJsonArray("parts");
			for (JsonElement element : parts == null ? new JsonArray() : parts) {
				JsonObject part = element.getAsJsonObject();
				boolean thought = part.has("thought") && part.get("thought").getAsBoolean();
				if (!thought && part.has("text")) {
					text.append(part.get("text").getAsString());
				}
			}
		} catch (RuntimeException e) {
			throw new LlmRequestException(LlmRequestException.Stage.INVALID_RESPONSE, "Gemini response could not be read", e);
		}
		if (text.toString().isBlank()) {
			throw new LlmRequestException(LlmRequestException.Stage.INVALID_RESPONSE, "Gemini response had no text");
		}
		return text.toString();
	}

	/** Get a brief summary of the body
	 * @param body The body to summarize
	 * @return A brief summary of the body
	 */
	private String brief(String body) {
		if (body == null || body.isBlank()) {
			return "(empty body)";
		}
		String cleaned = body;
		try { //Gemini errors are {"error": {"message": ...}}; the message alone fits a status line
			cleaned = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("error").get("message").getAsString();
		} catch (RuntimeException notTheUsualShape) {
			cleaned = body; //not Gemini's error shape: summarize the raw body instead
		}
		cleaned = cleaned.replace(apiKey, "[redacted]").replace('\n', ' ').replace('\r', ' ').trim();
		return cleaned.length() <= BRIEF_LENGTH ? cleaned : cleaned.substring(0, BRIEF_LENGTH - 3) + "...";
	}
}
