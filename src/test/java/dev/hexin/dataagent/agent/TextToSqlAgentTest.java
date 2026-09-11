package dev.hexin.dataagent.agent;

import dev.hexin.dataagent.llm.LlmClient;
import dev.hexin.dataagent.sql.ReadOnlySqlExecutor;
import dev.hexin.dataagent.sql.SchemaService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TextToSqlAgentTest {

    @Test
    void cleansMarkdownSqlFence() {
        assertEquals("SELECT * FROM customers;", TextToSqlAgent.cleanSql("```sql\nSELECT * FROM customers;\n```"));
    }

    @Test
    void repairsFailedSqlAndRetries() {
        SchemaService schema = mock(SchemaService.class);
        ReadOnlySqlExecutor executor = mock(ReadOnlySqlExecutor.class);
        LlmClient llm = mock(LlmClient.class);

        when(schema.compactSchema()).thenReturn(Map.of("orders", List.of("id", "amount")));
        AtomicInteger llmCalls = new AtomicInteger();
        when(llm.generate(anyString(), anyString())).thenAnswer(invocation -> switch (llmCalls.getAndIncrement()) {
            case 0 -> "SELECT missing FROM orders";
            case 1 -> "SELECT amount FROM orders";
            default -> "The amount is 10.";
        });

        when(executor.execute("SELECT missing FROM orders"))
                .thenThrow(new RuntimeException("column missing does not exist"));
        when(executor.execute("SELECT amount FROM orders"))
                .thenReturn(new ReadOnlySqlExecutor.QueryResult(List.of(Map.of("amount", 10)), 1, false));

        TextToSqlAgent agent = new TextToSqlAgent(schema, executor, llm);
        TextToSqlAgent.AgentAnswer answer = agent.ask("What is the amount?");

        assertEquals("SELECT amount FROM orders", answer.sql());
        assertEquals(2, answer.attempts());
        assertEquals("The amount is 10.", answer.explanation());
        verify(executor, times(2)).execute(anyString());
    }
}
