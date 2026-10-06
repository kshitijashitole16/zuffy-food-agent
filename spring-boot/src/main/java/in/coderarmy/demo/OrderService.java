package in.coderarmy.demo;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static in.coderarmy.demo.ApiModels.*;

/** Intentionally in-memory: orders are demonstrations, with no payment or restaurant dispatch. */
@Service
public class OrderService {
    private record StoredOrder(String sessionId, OrderView order) {}
    private final Map<String, StoredOrder> orders = new ConcurrentHashMap<>();
    private final MenuService menu;

    public OrderService(MenuService menu) { this.menu = menu; }

    public static String session(String value) {
        if (value == null || value.isBlank()) return UUID.randomUUID().toString();
        if (!value.matches("[a-zA-Z0-9_-]{8,64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a sessionId with 8–64 letters, digits, underscores or hyphens.");
        }
        return value;
    }

    public synchronized OrderView create(OrderRequest request) {
        String sessionId = session(request.sessionId());
        if (orders.size() >= 2000) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demo order storage is full. Restart the backend to clear it.");
        }
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (OrderLineRequest line : request.items()) {
            menu.byId(line.itemId());
            int quantity = quantities.merge(line.itemId(), line.quantity(), Integer::sum);
            if (quantity > 10) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 10 of each menu item per order.");
        }
        List<OrderLine> lines = quantities.entrySet().stream().map(entry -> {
            MenuItem item = menu.byId(entry.getKey());
            return new OrderLine(item.id(), item.name(), item.emoji(), entry.getValue(), item.price());
        }).toList();
        // Always calculate totals using server-side prices, never client-supplied amounts.
        int subtotal = lines.stream().mapToInt(line -> line.price() * line.quantity()).sum();
        int delivery = subtotal >= 399 ? 0 : 29;
        OrderView order = new OrderView("ZF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                lines, subtotal, delivery, subtotal + delivery, "Preparing", 2, Instant.now(), true);
        orders.put(order.id(), new StoredOrder(sessionId, order));
        return order;
    }

    public List<OrderView> list(String sessionId) {
        String owner = session(sessionId);
        return orders.values().stream().filter(order -> order.sessionId().equals(owner))
                .map(order -> current(order.order()))
                .sorted(Comparator.comparing(OrderView::createdAt).reversed()).toList();
    }

    public Optional<OrderView> find(String sessionId, String id) {
        if (id == null || id.isBlank()) return list(sessionId).stream().findFirst();
        StoredOrder stored = orders.get(id.strip().toUpperCase(Locale.ROOT));
        if (stored == null || !stored.sessionId().equals(session(sessionId))) return Optional.empty();
        return Optional.of(current(stored.order()));
    }

    private OrderView current(OrderView order) {
        long seconds = Duration.between(order.createdAt(), Instant.now()).getSeconds();
        String status = seconds < 45 ? "Preparing" : seconds < 120 ? "On the way" : "Delivered";
        int remaining = (int) Math.max(0, Math.ceil((120 - seconds) / 60.0));
        return new OrderView(order.id(), order.items(), order.subtotal(), order.deliveryFee(),
                order.total(), status, remaining, order.createdAt(), true);
    }
}
