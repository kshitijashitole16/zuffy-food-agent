package in.coderarmy.demo;

import java.util.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import static in.coderarmy.demo.ApiModels.*;

/** A new instance is made for each chat turn, so tool traces never leak between sessions. */
public class FoodTools {
    private final MenuService menu;
    private final OrderService orders;
    private final String sessionId;
    private final List<String> used = new ArrayList<>();
    private final Map<String, MenuItem> recommendations = new LinkedHashMap<>();
    private int calls;

    public FoodTools(MenuService menu, OrderService orders, String sessionId) {
        this.menu = menu;
        this.orders = orders;
        this.sessionId = sessionId;
    }

    @Tool(description = "Search the real Zuffy demo menu for dishes, ingredients, dietary preferences and prices in INR. Use this before quoting menu prices or recommending menu items. An empty query lists matching dishes.")
    public List<MenuItem> searchMenu(
            @ToolParam(description = "A simple food or ingredient keyword, or empty string for all dishes", required = false) String query,
            @ToolParam(description = "All, Vegetarian, Vegan, or Non-Vegetarian", required = false) String diet,
            @ToolParam(description = "Maximum price per item in Indian rupees, or null if no budget", required = false) Integer maximumPrice) {
        record("Checked the menu");
        List<MenuItem> matches = menu.search(query, diet, maximumPrice);
        matches.forEach(item -> recommendations.put(item.id(), item));
        return matches;
    }

    @Tool(description = "Look up the actual status of this user's demo order. Omit orderId to check their latest order. Returns not found if there is no matching order. Never invent order status.")
    public Map<String, Object> checkMyOrder(
            @ToolParam(description = "Order ID such as ZF-1234ABCD, or empty string for the latest order", required = false) String orderId) {
        record("Checked your order");
        return orders.find(sessionId, orderId).<Map<String, Object>>map(order -> Map.of("found", true, "order", order))
                .orElseGet(() -> Map.of("found", false, "message", "No matching order exists in this browser session. Create a demo order using the bag."));
    }

    @Tool(description = "Get the Zuffy demo delivery fees, checkout behavior and cancellation/refund policy. Use for delivery, refunds, payments, cancellation or support questions.")
    public Map<String, Object> getStorePolicy() {
        record("Checked store policy");
        return Map.of("currency", "INR", "deliveryFee", 29, "freeDeliveryAt", 399,
                "checkout", "Demo only. No payment is collected and no restaurant receives the order.",
                "tracking", "Simulated progression: Preparing for 45 seconds, then On the way, then Delivered after 2 minutes.",
                "refunds", "No money is charged, so there are no real refunds. For a real order, contact that food platform's customer support.",
                "cancellation", "This demo does not offer cancellation or real customer support actions. Do not claim to issue a refund or cancel an order.");
    }

    private void record(String label) {
        if (++calls > 6) throw new IllegalStateException("Tool call budget exceeded. Ask a shorter food question.");
        if (!used.contains(label)) used.add(label);
    }

    public List<String> toolsUsed() { return List.copyOf(used); }
    public List<MenuItem> recommendations() { return recommendations.values().stream().limit(4).toList(); }
}
