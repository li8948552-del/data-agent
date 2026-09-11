package dev.hexin.dataagent.eval;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class SqlEvaluationService {

    public EvaluationResult evaluate(String sql, EvaluationCase testCase) {
        String normalized = normalize(sql);

        List<String> missingTables = testCase.requiredTables().stream()
                .map(String::toLowerCase)
                .filter(table -> !containsToken(normalized, table))
                .toList();

        List<String> missingTokens = testCase.requiredTokens().stream()
                .map(String::toLowerCase)
                .filter(token -> !normalized.contains(token))
                .toList();

        List<String> forbiddenHits = testCase.forbiddenTokens().stream()
                .map(String::toLowerCase)
                .filter(token -> normalized.contains(token))
                .toList();

        int totalChecks = testCase.requiredTables().size() + testCase.requiredTokens().size() + testCase.forbiddenTokens().size();
        int failures = missingTables.size() + missingTokens.size() + forbiddenHits.size();
        double score = totalChecks == 0 ? 1.0 : (double) (totalChecks - failures) / totalChecks;

        return new EvaluationResult(failures == 0, score, missingTables, missingTokens, forbiddenHits);
    }

    private static String normalize(String sql) {
        return sql == null ? "" : sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static boolean containsToken(String normalized, String token) {
        return normalized.matches(".*(^|[^a-z0-9_])" + java.util.regex.Pattern.quote(token) + "([^a-z0-9_]|$).*");
    }

    public record EvaluationCase(
            String id,
            String question,
            List<String> requiredTables,
            List<String> requiredTokens,
            List<String> forbiddenTokens
    ) {}

    public record EvaluationResult(
            boolean passed,
            double score,
            List<String> missingTables,
            List<String> missingTokens,
            List<String> forbiddenHits
    ) {}
}
