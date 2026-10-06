package in.coderarmy.demo;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static in.coderarmy.demo.ApiModels.*;

@RestController
@RequestMapping("/api")
public class ZuffyController {
    private final FoodAgentService agent;
    private final MenuService menu;
    private final OrderService orders;
    private final OllamaStatusService ollama;

    public ZuffyController(FoodAgentService agent, MenuService menu, OrderService orders, OllamaStatusService ollama) {
        this.agent = agent;
        this.menu = menu;
        this.orders = orders;
        this.ollama = ollama;
    }

    @GetMapping("/health")
    public Health health() { return ollama.health(); }

    @GetMapping("/menu")
    public List<MenuItem> menu() { return menu.all(); }

    // The recipe alias lets you reuse the endpoint from your previous project.
    @PostMapping({"/chat", "/zuffy/chat", "/recipes/chat"})
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) { return agent.chat(request); }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/orders")
    public OrderView createOrder(@Valid @RequestBody OrderRequest request) { return orders.create(request); }

    @GetMapping("/orders")
    public List<OrderView> orders(@RequestParam String sessionId) { return orders.list(sessionId); }

    @GetMapping("/orders/{id}")
    public OrderView order(@PathVariable String id, @RequestParam String sessionId) {
        return orders.find(sessionId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found in this session."));
    }
}
