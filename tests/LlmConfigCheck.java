package com.endlessadventure.llm;

import com.sun.net.httpserver.HttpServer;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Offline LLM configuration check. Run only in a fresh endless-llm-check-* temporary directory. */
public final class LlmConfigCheck {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of("").toAbsolutePath();
        Path envFile = directory.resolve(".env");
        if (!directory.getFileName().toString().startsWith("endless-llm-check-") || Files.exists(envFile)) {
            throw new IllegalStateException("Use a fresh endless-llm-check-* temporary working directory");
        }
        check("environment-test-key".equals(System.getenv("ENDLESS_ADVENTURE_API_KEY")),
                "Run with the documented dummy environment values");
        check("environment-test-model".equals(System.getenv("ENDLESS_ADVENTURE_MODEL")),
                "Run with the documented dummy environment values");

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String marker = exchange.getRequestHeaders().getFirst("x-goog-api-key")
                    + "|" + exchange.getRequestURI().getPath();
            byte[] response = ("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\""
                    + LlmClient.escape(marker) + "\"}]}}]}").getBytes(StandardCharsets.UTF_8);
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta/models/";
        try {
            // A missing .env must not prevent client construction.
            new LlmClient();

            // Exercise the public constructor and HTTP path, not just a configuration helper.
            Files.writeString(envFile, "\uFEFF# Dummy settings only\r\n\r\n"
                    + " ENDLESS_ADVENTURE_API_KEY = 'file-test-key=='\r\n"
                    + "ENDLESS_ADVENTURE_MODEL=\"file-test-model\"\r\n"
                    + "ENDLESS_ADVENTURE_API_URL=\"" + base + "file-test-model:generateContent\"\r\n",
                    StandardCharsets.UTF_8);
            String response = new LlmClient().generate("Test only", "Test only");
            check(response.equals("file-test-key==|/v1beta/models/file-test-model:generateContent"),
                    ".env values must override inherited values, preserve equals signs, and support quotes/BOM/CRLF");

            Files.writeString(envFile, "ENDLESS_ADVENTURE_API_URL="
                    + base + "environment-test-model:generateContent\n", StandardCharsets.UTF_8);
            response = new LlmClient().generate("Test only", "Test only");
            check(response.equals("environment-test-key|/v1beta/models/environment-test-model:generateContent"),
                    "Settings absent from .env must use inherited environment values");

            Files.writeString(envFile, "ENDLESS_ADVENTURE_API_KEY=\n", StandardCharsets.UTF_8);
            try {
                new LlmClient().generate("Test only", "Test only");
                throw new AssertionError("A deliberately empty local key must not use the inherited key");
            } catch (LlmRequestException e) {
                check(e.getStage() == LlmRequestException.Stage.MISSING_KEY,
                        "An empty local key must report MISSING_KEY before any HTTP request");
            }

            for (String invalid : new String[] { "private-dummy-without-separator", "export NAME=value", "NAME='unclosed" }) {
                Files.writeString(envFile, invalid, StandardCharsets.UTF_8);
                try {
                    new LlmClient();
                    throw new AssertionError("Malformed configuration must be rejected");
                } catch (IllegalArgumentException e) {
                    check(e.getMessage().contains("line 1") && !e.getMessage().contains(invalid),
                            "Configuration errors must identify the line without exposing its contents");
                }
            }

            Files.delete(envFile);
            Files.createDirectory(envFile);
            try {
                new LlmClient();
                throw new AssertionError("Unreadable .env must not silently use inherited settings");
            } catch (UncheckedIOException e) {
                check(e.getCause() != null, "Read errors must retain their cause");
            }
            System.out.println("PASS: project .env loading, HTTP use, precedence, fallback, blank keys, and safe errors");
        } finally {
            server.stop(0);
            Files.deleteIfExists(envFile);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
