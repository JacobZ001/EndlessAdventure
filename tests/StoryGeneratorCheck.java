package com.endlessadventure.llm;

import com.endlessadventure.GameState;
import com.endlessadventure.Story.Scene;
import com.endlessadventure.Story.StoryGenerator;
import com.endlessadventure.entity.Player;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Offline story check using dummy credentials and a local HTTP server. Developed with GenAI assistance. */
public final class StoryGeneratorCheck {
	private static final String VALID = """
			protocol.version=1
			location=Old Gate
			description=A sign reads C:\\road. Runes a=b. The gate is locked.
			option.count=2
			option.0.text=Inspect the Runes
			option.1.text=Follow the Road
			""";

	public static void main(String[] args) throws Exception {
		AtomicReference<String> reply = new AtomicReference<>(VALID);
		AtomicReference<String> request = new AtomicReference<>();
		AtomicInteger calls = new AtomicInteger();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			calls.incrementAndGet();
			request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] body = ("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\""
					+ LlmClient.escape(reply.get()) + "\"}]}}]}").getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (var output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		server.start();
		try {
			String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
			StoryGenerator generator = new StoryGenerator(new LlmClient("dummy-key", "test-model", endpoint));
			GameState state = new GameState(new Player("ZaRiX"));
			Scene original = state.getScene();
			Scene opening = generator.generateOpening(state);
			check(opening.getDescription().contains("Runes a=b") && opening.getDescription().contains("C:\\road"),
					"Literal equals signs and backslashes must survive parsing");
			check(request.get().contains("player.name=ZaRiX") && request.get().contains("request=opening"),
					"Opening context includes player facts");
			check(state.getScene() == original && state.getTurn() == 1 && state.getPlayer().getHp() == 40,
					"Generation must not commit a scene or change game state");
			String[] options = opening.getOptions();
			options[0] = "Changed";
			check(opening.getOptions()[0].equals("Inspect the Runes"), "Choice getters must return a copy");
			Scene copied = new Scene("Gate", "Description", options);
			options[0] = "Changed again";
			check(copied.getOptions()[0].equals("Changed"), "Constructor must copy the supplied choices");

			state.setScene(opening);
			generator.generateNext(state, "Inspect the Runes");
			check(request.get().contains("scene.location=Old Gate")
					&& request.get().contains("scene.description=")
					&& request.get().contains("player.action=Inspect the Runes"),
					"Continuation context includes the current scene and preserves action case");

			for (String invalid : new String[] {
					VALID.replace("location=Old Gate\n", ""),
					VALID + "description=Duplicate\n",
					VALID + "hp=999\n",
					"```\n" + VALID + "```",
					VALID.replace("option.count=2", "option.count=2147483647"),
					VALID.replace("option.1.text=Follow the Road", "option.1.text="),
					VALID.replace("Follow the Road", "inspect the runes"),
					VALID.replace("Old Gate", "x".repeat(25)),
					VALID.replace("The gate is locked.", "\u001b[31mThe gate is locked."),
					VALID.replace("protocol.version=1", "protocol.version=2")}) {
				reply.set(invalid);
				int before = calls.get();
				try {
					generator.generateNext(state, "Inspect the Runes");
					throw new AssertionError("Invalid story must be rejected");
				} catch (LlmRequestException e) {
					check(e.getStage() == LlmRequestException.Stage.INVALID_RESPONSE,
							"Malformed game text must report INVALID_RESPONSE");
				}
				check(calls.get() == before + 1 && state.getScene() == opening && state.getTurn() == 1,
						"Failure must neither retry nor partially update the state");
			}

			reply.set(VALID.replace("The gate is locked.", "\u201c" + "x".repeat(400) + "\u201d")
					.replace("option.count=2", "option.count=5")
					+ "option.2.text=Look Around\noption.3.text=Wait\noption.4.text=Leave\n");
			Scene bounded = generator.generateNext(state, "Inspect the Runes");
			check(bounded.getOptions().length == 4 && bounded.getDescription().length() == 300
					&& bounded.getDescription().contains("\"" + "x".repeat(10)),
					"Long text is bounded, punctuation normalized, and extra choices capped at four");
			System.out.println("PASS: opening/continuation HTTP requests, context, parsing, bounds, and state preservation");
		} finally {
			server.stop(0);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
