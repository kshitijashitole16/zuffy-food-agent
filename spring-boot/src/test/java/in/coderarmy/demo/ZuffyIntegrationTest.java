package in.coderarmy.demo;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real HTTP routes and the Spring AI tool loop against a controlled Ollama-compatible server. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ZuffyIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final List<String> MODEL_REQUESTS = new CopyOnWriteArrayList<>();
    private static volatile boolean modelAvailable = true;
    private static final HttpServer OLLAMA = startOllama();
    private final HttpClient http = HttpClient.newHttpClient();
    @LocalServerPort int port;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", () -> "http://127.0.0.1:" + OLLAMA.getAddress().getPort());
    }

    @AfterAll static void shutdown() { OLLAMA.stop(0); }

    @Test void realAgentExecutesMenuToolAndReceivesItsResult() throws Exception {
        int before = MODEL_REQUESTS.size();
        var response = post("/api/chat", """
            {"message":"MENU_TOOL_TEST: suggest a vegetarian meal under 200","sessionId":"test-menu-session",
             "history":[{"role":"user","content":"I like rice."},{"role":"assistant","content":"I will keep that in mind."}],
             "preferences":{"diet":"Vegetarian","avoid":""}}
            """);
        assertEquals(200, response.statusCode(), response.body());
        Map<?, ?> data = parse(response.body());
        assertTrue(data.get("reply").toString().contains("biryani"));
        assertTrue(data.get("toolsUsed").toString().contains("Checked the menu"));
        assertTrue(data.get("recommendations").toString().contains("veg-biryani"));
        assertFalse(data.get("recommendations").toString().contains("chicken"));
        List<String> calls = MODEL_REQUESTS.subList(before, MODEL_REQUESTS.size());
        assertTrue(calls.size() >= 2, "The model must be re-called with the tool result.");
        for (String call : calls) assertEquals("llama3.2:3b", parse(call).get("model"),
                "Every inference request must use the configured Ollama model.");
        assertTrue(calls.get(0).contains("I like rice."), "Conversation history reaches the model.");
        assertTrue(calls.stream().anyMatch(body -> body.contains("Garden veg biryani")), "Actual catalog data reaches Ollama.");
    }

    @Test void createsOrderWithServerPricesAndIsolatesOtherSessions() throws Exception {
        var response = post("/api/orders", """
            {"sessionId":"test-order-owner","items":[{"itemId":"veg-biryani","quantity":2}]}
            """);
        assertEquals(201, response.statusCode(), response.body());
        Map<?, ?> order = parse(response.body());
        assertEquals(378, ((Number) order.get("subtotal")).intValue());
        assertEquals(29, ((Number) order.get("deliveryFee")).intValue());
        assertEquals(407, ((Number) order.get("total")).intValue());
        assertEquals(Boolean.TRUE, order.get("demo"));
        String id = order.get("id").toString();
        assertEquals(200, get("/api/orders/" + id + "?sessionId=test-order-owner").statusCode());
        assertEquals(404, get("/api/orders/" + id + "?sessionId=different-session").statusCode());
        assertEquals("[]", get("/api/orders?sessionId=different-session").body());
    }

    @Test void agentLooksUpAnActualOrder() throws Exception {
        Map<?, ?> order = parse(post("/api/orders", """
            {"sessionId":"test-track-owner","items":[{"itemId":"masala-dosa","quantity":3}]}
            """).body());
        var response = post("/api/chat", """
            {"message":"TRACK_TOOL_TEST: what is my latest order status?","sessionId":"test-track-owner"}
            """);
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("Checked your order"));
        assertTrue(MODEL_REQUESTS.stream().anyMatch(body -> body.contains(order.get("id").toString())));
    }

    @Test void rejectsInvalidInputAndDuplicateQuantityOverflow() throws Exception {
        assertEquals(400, get("/api/orders").statusCode());
        assertEquals(400, post("/api/chat", "{\"message\":\"   \"}").statusCode());
        assertEquals(400, post("/api/orders", "{\"sessionId\":\"test-validation\",\"items\":[{\"itemId\":\"veg-biryani\",\"quantity\":0}]}").statusCode());
        assertEquals(400, post("/api/orders", "{\"sessionId\":\"test-validation\",\"items\":[{\"itemId\":\"missing-item\",\"quantity\":1}]}").statusCode());
        assertEquals(400, post("/api/orders", "{\"sessionId\":\"test-validation\",\"items\":[{\"itemId\":\"veg-biryani\",\"quantity\":6},{\"itemId\":\"veg-biryani\",\"quantity\":6}]}").statusCode());
    }

    @Test void healthAndRecipeCompatibilityEndpointWork() throws Exception {
        var response = get("/api/health");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("ready"));
        assertEquals(8, ((List<?>) JSON.readValue(get("/api/menu").body(), List.class)).size());
        assertEquals(200, post("/api/recipes/chat", "{\"message\":\"Suggest a rice recipe\",\"ingredients\":[\"rice\",\"tomato\"]}").statusCode());
    }

    @Test void missingModelReturnsActionableErrorWithoutCallingChat() throws Exception {
        int before = MODEL_REQUESTS.size();
        modelAvailable = false;
        try {
            assertTrue(get("/api/health").body().contains("model_missing"));
            var response = post("/api/chat", "{\"message\":\"Suggest dinner\"}");
            assertEquals(503, response.statusCode(), response.body());
            assertTrue(response.body().contains("ollama pull llama3.2:3b"));
            assertEquals(before, MODEL_REQUESTS.size());
        } finally { modelAvailable = true; }
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static Map<?, ?> parse(String body) { return JSON.readValue(body, Map.class); }

    private static HttpServer startOllama() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/tags", exchange -> {
                byte[] payload = (modelAvailable ? "{\"models\":[{\"name\":\"llama3.2:3b\",\"model\":\"llama3.2:3b\",\"size\":2019393189,\"details\":{\"family\":\"llama\"}}]}" : "{\"models\":[]}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, payload.length); exchange.getResponseBody().write(payload); exchange.close();
            });
            server.createContext("/api/chat", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                MODEL_REQUESTS.add(body);
                Map<?, ?> request = parse(body);
                List<?> messages = (List<?>) request.get("messages");
                boolean hasToolResult = messages.stream().anyMatch(message -> "tool".equals(((Map<?, ?>) message).get("role")));
                Map<String, Object> message = new LinkedHashMap<>();
                message.put("role", "assistant");
                if (!hasToolResult && (body.contains("MENU_TOOL_TEST") || body.contains("TRACK_TOOL_TEST"))) {
                    boolean tracking = body.contains("TRACK_TOOL_TEST");
                    message.put("content", "");
                    message.put("tool_calls", List.of(Map.of("function", Map.of("name", tracking ? "checkMyOrder" : "searchMenu",
                            "arguments", tracking ? Map.of("orderId", "") : Map.of("query", "", "diet", "Vegetarian", "maximumPrice", 200)))));
                } else {
                    message.put("content", body.contains("TRACK_TOOL_TEST") ? "Your demo order is preparing." :
                            "Try Garden veg biryani for ₹189, or cook a simple tomato rice at home.");
                }
                byte[] payload = JSON.writeValueAsBytes(Map.of("model", "llama3.2:3b", "created_at", "2026-10-06T12:00:00Z",
                        "message", message, "done", true, "done_reason", "stop", "prompt_eval_count", 40, "eval_count", 20));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, payload.length); exchange.getResponseBody().write(payload); exchange.close();
            });
            server.start(); return server;
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
}
