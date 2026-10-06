package in.coderarmy.demo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

/** Small API records keep the controller and service easy to follow. */
public final class ApiModels {
    private ApiModels() {}

    public record MenuItem(String id, String name, String description, String category,
                           String diet, int price, int minutes, double rating,
                           String emoji, String color, List<String> ingredients,
                           List<String> allergens, String tag) {}

    public record HistoryMessage(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 4000) String content) {}

    public record Preferences(@Size(max = 40) String diet, @Size(max = 300) String avoid) {}

    public record ChatRequest(
            @NotBlank @Size(max = 4000) String message,
            @Size(max = 64) String sessionId,
            @Size(max = 12) List<@Valid HistoryMessage> history,
            @Valid Preferences preferences,
            @Size(max = 20) List<@NotBlank @Size(max = 80) String> ingredients) {}

    public record ChatResponse(String reply, String sessionId, String model,
                               List<String> toolsUsed, List<MenuItem> recommendations) {}

    public record OrderLineRequest(@NotBlank String itemId, @Min(1) @Max(10) int quantity) {}

    public record OrderRequest(
            @NotBlank @Size(max = 64) String sessionId,
            @NotEmpty @Size(max = 20) List<@Valid OrderLineRequest> items) {}

    public record OrderLine(String itemId, String name, String emoji, int quantity, int price) {}

    public record OrderView(String id, List<OrderLine> items, int subtotal, int deliveryFee,
                            int total, String status, int estimatedMinutes,
                            Instant createdAt, boolean demo) {}

    public record Health(String status, String ollama, String model, String message) {}
    public record ErrorBody(String code, String message) {}
}
