package dev.hexin.dataagent.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleEmbeddingClient(
            RestClient.Builder builder,
            @Value("${agent.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${agent.llm.api-key:}") String apiKey,
            @Value("${agent.embedding.model:text-embedding-3-small}") String model) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Double> embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LLM_API_KEY is not configured");
        }

        Map<String, Object> response = restClient.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "input", text))
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Embedding endpoint returned an empty response");
        }
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("Embedding response contains no data");
        }
        List<Number> raw = (List<Number>) data.getFirst().get("embedding");
        return raw.stream().map(Number::doubleValue).toList();
    }
}
