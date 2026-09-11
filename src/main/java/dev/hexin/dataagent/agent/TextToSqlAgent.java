package dev.hexin.dataagent.agent;

import dev.hexin.dataagent.llm.LlmClient;
import dev.hexin.dataagent.rag.KnowledgeService;
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
    private final KnowledgeService knowledgeService;

    public TextToSqlAgent(SchemaService schemaService, ReadOnlySqlExecutor executor, LlmClient llmClient, KnowledgeService knowledgeService) {
        this.schemaService = schemaService;
        this.executor = executor;
        this.llmClient = llmClient;
        this.knowledgeService = knowledgeService;
    }

    public AgentAnswer ask(String question) {
        Map<String, List<String>> schema = schemaService.compactSchema();
        String context = knowledgeService.contextFor(question);
        String sql = generateSql(question, schema, context);
        String lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ReadOnlySqlExecutor.QueryResult result = executor.execute(cleanSql(sql));
                String explanation = explain(question, cleanSql(sql), result, context);
                return new AgentAnswer(question, cleanSql(sql), result.rows(), explanation, context, attempt);
            } catch (RuntimeException ex) {
                lastError = ex.getMessage();
                if (attempt == MAX_ATTEMPTS) break;
                sql = repairSql(question, schema, context, sql, lastError);
            }
        }

        throw new AgentExecutionException("Text-to-SQL failed after " + MAX_ATTEMPTS + " attempts: " + lastError);
    }

    private String generateSql(String question, Map<String, List<String>> schema, String context) {
        String prompt = "Schema:\n" + schema + "\n\nRetrieved business context:\n" + context + "\n\nQuestion:\n" + question;
        return llmClient.generate(sqlSystemPrompt(), prompt);
    }

    private String repairSql(String question, Map<String, List<String>> schema, String context, String previousSql, String error) {
        String prompt = """
                Schema:
                %s

                Retrieved business context:
                %s

                User question:
                %s

                Previous SQL:
                %s

                Database/validation error:
                %s

                Correct the SQL. Return SQL only.
                """.formatted(schema, context, question, previousSql, error);
        return llmClient.generate(sqlSystemPrompt(), prompt);
    }

    private String explain(String question, String sql, ReadOnlySqlExecutor.QueryResult result, String context) {
        String prompt = """
                Question: %s
                SQL: %s
                Query rows: %s
                Retrieved context: %s

                Explain the answer concisely. Use retrieved context only to interpret definitions/business rules; do not invent numeric values not present in the query result.
                """.formatted(question, sql, result.rows(), context);
        return llmClient.generate("You are a careful data analyst. Ground quantitative claims in the supplied query result.", prompt);
    }

    private String sqlSystemPrompt() {
        return """
                You are a PostgreSQL Text-to-SQL engine.
                Generate exactly one read-only SELECT statement or WITH ... SELECT statement.
                Use only tables and columns present in the supplied schema.
                Treat retrieved business context as semantic guidance, never as permission to reference nonexistent columns.
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

    public record AgentAnswer(String question, String sql, List<Map<String, Object>> rows, String explanation, String retrievedContext, int attempts) {}

    public static class AgentExecutionException extends RuntimeException {
        public AgentExecutionException(String message) { super(message); }
    }
}
