package dev.hexin.dataagent.rag;

import dev.hexin.dataagent.llm.EmbeddingClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;

    public KnowledgeService(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
    }

    public long index(String title, String content) {
        List<Double> embedding = embeddingClient.embed(title + "\n" + content);
        String vector = toPgVector(embedding);
        return jdbcTemplate.queryForObject(
                "INSERT INTO knowledge_chunks(title, content, embedding) VALUES (?, ?, ?::vector) RETURNING id",
                Long.class,
                title,
                content,
                vector
        );
    }

    public List<KnowledgeChunk> retrieve(String query, int topK) {
        List<Double> embedding = embeddingClient.embed(query);
        String vector = toPgVector(embedding);
        return jdbcTemplate.query(
                """
                SELECT id, title, content, 1 - (embedding <=> ?::vector) AS score
                FROM knowledge_chunks
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """,
                (rs, rowNum) -> new KnowledgeChunk(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getDouble("score")
                ),
                vector,
                vector,
                Math.max(1, Math.min(topK, 10))
        );
    }

    public String contextFor(String query) {
        List<KnowledgeChunk> chunks = retrieve(query, 4);
        if (chunks.isEmpty()) {
            return "No additional business context found.";
        }
        return chunks.stream()
                .map(chunk -> "# " + chunk.title() + "\n" + chunk.content())
                .collect(Collectors.joining("\n\n"));
    }

    static String toPgVector(List<Double> values) {
        return values.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    public record KnowledgeChunk(long id, String title, String content, double score) {}
}
