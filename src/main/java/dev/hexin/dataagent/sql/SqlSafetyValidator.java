package dev.hexin.dataagent.sql;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SqlSafetyValidator {

    private static final Set<String> FORBIDDEN = Set.of(
            "insert", "update", "delete", "drop", "alter", "truncate",
            "create", "grant", "revoke", "copy", "call", "do", "merge"
    );

    private static final Pattern LEADING_COMMENT = Pattern.compile("^(?:\\s|--[^\\n]*(?:\\n|$)|/\\*.*?\\*/)*", Pattern.DOTALL);

    public ValidationResult validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return ValidationResult.reject("SQL must not be blank");
        }

        String stripped = LEADING_COMMENT.matcher(sql).replaceFirst("").trim();
        String normalized = stripped.toLowerCase(Locale.ROOT);

        if (!(normalized.startsWith("select ") || normalized.startsWith("with "))) {
            return ValidationResult.reject("Only SELECT/CTE queries are allowed");
        }

        if (containsMultipleStatements(stripped)) {
            return ValidationResult.reject("Only one SQL statement is allowed");
        }

        String tokenized = normalized.replaceAll("[^a-z0-9_]+", " ");
        for (String keyword : FORBIDDEN) {
            if (Pattern.compile("(^|\\s)" + Pattern.quote(keyword) + "(\\s|$)").matcher(tokenized).find()) {
                return ValidationResult.reject("Forbidden SQL keyword: " + keyword.toUpperCase(Locale.ROOT));
            }
        }

        return ValidationResult.allow();
    }

    private boolean containsMultipleStatements(String sql) {
        String trimmed = sql.trim();
        int firstSemicolon = trimmed.indexOf(';');
        return firstSemicolon >= 0 && firstSemicolon < trimmed.length() - 1;
    }

    public record ValidationResult(boolean allowed, String reason) {
        static ValidationResult allow() { return new ValidationResult(true, "OK"); }
        static ValidationResult reject(String reason) { return new ValidationResult(false, reason); }
    }
}
