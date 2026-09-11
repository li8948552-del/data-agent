package dev.hexin.dataagent.sql;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReadOnlySqlExecutor {

    private static final int MAX_ROWS = 200;

    private final JdbcTemplate jdbcTemplate;
    private final SqlSafetyValidator validator;

    public ReadOnlySqlExecutor(JdbcTemplate jdbcTemplate, SqlSafetyValidator validator) {
        this.jdbcTemplate = jdbcTemplate;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public QueryResult execute(String sql) {
        SqlSafetyValidator.ValidationResult validation = validator.validate(sql);
        if (!validation.allowed()) {
            throw new UnsafeSqlException(validation.reason());
        }

        jdbcTemplate.execute("SET LOCAL statement_timeout = '5s'");
        jdbcTemplate.execute("SET LOCAL default_transaction_read_only = on");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        boolean truncated = rows.size() > MAX_ROWS;
        List<Map<String, Object>> safeRows = truncated ? rows.subList(0, MAX_ROWS) : rows;
        return new QueryResult(safeRows, safeRows.size(), truncated);
    }

    public record QueryResult(List<Map<String, Object>> rows, int rowCount, boolean truncated) {}

    public static class UnsafeSqlException extends RuntimeException {
        public UnsafeSqlException(String message) { super(message); }
    }
}
