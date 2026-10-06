package in.coderarmy.demo;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static in.coderarmy.demo.ApiModels.*;

@Service
public class MenuService {
    private final List<MenuItem> menu = List.of(
        new MenuItem("paneer-bowl", "Paneer sunshine bowl", "Grilled paneer, jeera rice & a bright mint chutney.", "Bowls", "Vegetarian", 229, 25, 4.8, "🥗", "peach", List.of("paneer", "rice", "tomato", "mint"), List.of("milk"), "Bestseller"),
        new MenuItem("masala-dosa", "The classic masala dosa", "A crisp golden dosa with potato masala & sambar.", "Indian", "Vegan", 149, 20, 4.7, "🫓", "yellow", List.of("rice", "lentils", "potato", "onion"), List.of(), "Comfort food"),
        new MenuItem("veg-biryani", "Garden veg biryani", "Fragrant basmati, seasonal vegetables & warming spices.", "Indian", "Vegan", 189, 30, 4.9, "🍛", "green", List.of("rice", "carrot", "peas", "onion", "spices"), List.of(), "Fan favorite"),
        new MenuItem("margherita", "Margherita, with love", "Slow tomato sauce, mozzarella & a basil finish.", "Pizza", "Vegetarian", 299, 30, 4.6, "🍕", "pink", List.of("wheat", "tomato", "mozzarella", "basil"), List.of("wheat", "milk"), "Freshly baked"),
        new MenuItem("chicken-wrap", "Smoky chicken wrap", "Grilled chicken, crunchy greens & a creamy house sauce.", "Wraps", "Non-Vegetarian", 219, 20, 4.8, "🌯", "lavender", List.of("chicken", "wheat", "lettuce", "yogurt"), List.of("wheat", "milk"), "Protein pick"),
        new MenuItem("dal-khichdi", "A hug of dal khichdi", "One-pot rice & moong dal, finished with gentle spices.", "Indian", "Vegan", 139, 20, 4.8, "🍲", "cream", List.of("rice", "moong dal", "turmeric", "cumin"), List.of(), "Light & lovely"),
        new MenuItem("chicken-biryani", "Homestyle chicken biryani", "Tender chicken, aromatic basmati & our secret spice blend.", "Indian", "Non-Vegetarian", 279, 30, 4.9, "🍗", "peach", List.of("chicken", "rice", "onion", "yogurt", "spices"), List.of("milk"), "Weekend mood"),
        new MenuItem("choco-brownie", "Little chocolate happiness", "A fudgy eggless brownie for a very sweet finish.", "Desserts", "Vegetarian", 99, 15, 4.7, "🍫", "pink", List.of("cocoa", "wheat", "milk", "sugar"), List.of("wheat", "milk"), "Sweet tooth")
    );

    public List<MenuItem> all() { return menu; }

    public MenuItem byId(String id) {
        return menu.stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown menu item: " + id));
    }

    public List<MenuItem> search(String query, String diet, Integer maximumPrice) {
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        String d = diet == null ? "all" : diet.strip().toLowerCase(Locale.ROOT);
        return menu.stream()
                .filter(item -> d.isBlank() || d.equals("all") ||
                        (d.equals("vegetarian") && !item.diet().equals("Non-Vegetarian")) ||
                        item.diet().toLowerCase(Locale.ROOT).equals(d))
                .filter(item -> maximumPrice == null || item.price() <= maximumPrice)
                .filter(item -> q.isBlank() || (item.name() + " " + item.description() + " " +
                        item.category() + " " + String.join(" ", item.ingredients())).toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }
}
