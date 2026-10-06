package in.coderarmy.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import static in.coderarmy.demo.ApiModels.*;

@Service
public class OllamaStatusService {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaModel(String name) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaTags(List<OllamaModel> models) {}
    private final RestClient client;
    private final String model;

    public OllamaStatusService(@Value("${spring.ai.ollama.base-url}") String baseUrl,
                              @Value("${zuffy.model}") String model) {
        this.model = model;
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public Health health() {
        try {
            OllamaTags tags = client.get().uri("/api/tags").retrieve().body(OllamaTags.class);
            boolean available = tags != null && tags.models() != null && tags.models().stream()
                    .anyMatch(item -> item.name().equals(model) || item.name().equals(model + ":latest"));
            return available ? new Health("UP", "ready", model, "Your local food assistant is ready.") :
                    new Health("UP", "model_missing", model, "Download the model: ollama pull " + model);
        } catch (RuntimeException error) {
            return new Health("UP", "offline", model, "Start the Ollama app or run ollama serve, then refresh.");
        }
    }
}
