package dev.hexin.dataagent.agent;

import dev.hexin.dataagent.llm.LlmClient;
import dev.hexin.dataagent.sql.ReadOnlySqlExecutor;
import dev.hexin.dataagent.sql.SchemaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TextToSqlAgent {

    private static final int MAX_ATTEMPTS = 3;

    private final SchemaService schemaService;
    private final ReadOnlySqlExecutor executor;
    private final LlmClient llmClient;

    public TextToSqlAgent(SchemaService schemaService, ReadOnlySqlExecutor executor, LlmClient llmClient) {
        this.schemaService = schemaService;
        this.executor = executor;
        this.llmClient = llmClient;
    }

    public AgentAnswer ask(String question) {
        Map<String, List<String>> schema = schemaService.compactSchema();
        String sql = generateSql(question, schema);
        String lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ReadOnlySqlExecutor.QueryResult result = executor.execute(cleanSql(sql));
                String explanation = explain(question, cleanSql(sql), result);
                return new AgentAnswer(question, cleanSql(sql), result.rows(), explanation, attempt);
            } catch (RuntimeException ex) {
                lastError = ex.getMessage();
                if (attempt == MAX_ATTEMPTS) {
                    break;
                }
                sql = repairSql(question, schema, sql, lastError);
            }
        }

        throw new AgentExecutionException("Text-to-SQL failed after " + MAX_ATTEMPTS + " attempts: " + lastError);
    }

    private String generateSql(String question, Map<String, List<String>> schema) {
        return llmClient.generate(sqlSystemPrompt(), "Schema:\n" + schema + "\n\nQuestion:\n" + question);
    }

    private String repairSql(String question, Map<String, List<String>> schema, String previousSql, String error) {
        String prompt = """
                Schema:
                %s

                User question:
                %s

                Previous SQL:
                %s

                Database/validation error:
                %s

                Correct the SQL. Return SQL only.
                """.formatted(schema, question, previousSql, error);
        return llmClient.generate(sqlSystemPrompt(), prompt);
    }

    private String explain(String question, String sql, ReadOnlySqlExecutor.QueryResult result) {
        String prompt = """
                Question: %s
                SQL: %s
                Query rows: %s

                Explain the answer concisely. Do not invent values not present in the query result.
                """.formatted(question, sql, result.rows());
        return llmClient.generate("You are a careful data analyst. Ground every claim in the supplied query result.", prompt);
    }

    private String sqlSystemPrompt() {
        return """
                You are a PostgreSQL Text-to-SQL engine.
                Generate exactly one read-only SELECT statement or WITH ... SELECT statement.
                Use only tables and columns present in the supplied schema.
                Never use INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE, CREATE, COPY, CALL, DO, MERGE, GRANT or REVOKE.
                Return SQL only: no markdown fences and no explanation.
                """;
    }

    static String cleanSql(String sql) {
        if (sql == null) return "";
        String cleaned = sql.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:sql)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned.trim();
    }

    public record AgentAnswer(String question, String sql, List<Map<String, Object>> rows, String explanation, int attempts) {}

    public static class AgentExecutionException extends RuntimeException {
        public AgentExecutionException(String message) { super(message); }
    }
}
