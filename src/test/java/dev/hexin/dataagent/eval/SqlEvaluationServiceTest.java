package dev.hexin.dataagent.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlEvaluationServiceTest {

    private final SqlEvaluationService evaluator = new SqlEvaluationService();

    @Test
    void passesExpectedRevenueAggregation() {
        var testCase = new SqlEvaluationService.EvaluationCase(
                "revenue-by-region",
                "Which region generated the most revenue?",
                List.of("orders", "customers"),
                List.of("sum(", "group by", "region"),
                List.of("delete", "update", "drop")
        );

        String sql = """
                SELECT c.region, SUM(o.amount) AS revenue
                FROM orders o
                JOIN customers c ON c.id = o.customer_id
                GROUP BY c.region
                ORDER BY revenue DESC
                LIMIT 1
                """;

        var result = evaluator.evaluate(sql, testCase);
        assertTrue(result.passed());
        assertEquals(1.0, result.score());
    }

    @Test
    void reportsMissingAndForbiddenRequirements() {
        var testCase = new SqlEvaluationService.EvaluationCase(
                "unsafe",
                "bad case",
                List.of("orders"),
                List.of("sum("),
                List.of("delete")
        );

        var result = evaluator.evaluate("DELETE FROM orders", testCase);
        assertFalse(result.passed());
        assertTrue(result.missingTokens().contains("sum("));
        assertTrue(result.forbiddenHits().contains("delete"));
    }
}
