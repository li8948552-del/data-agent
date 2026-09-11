package dev.hexin.dataagent.sql;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchemaService {

    private final JdbcTemplate jdbcTemplate;

    public SchemaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TableSchema> inspectPublicSchema() {
        String tableSql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """;

        return jdbcTemplate.queryForList(tableSql, String.class).stream()
                .map(this::describeTable)
                .toList();
    }

    private TableSchema describeTable(String tableName) {
        String columnSql = """
                SELECT column_name, data_type, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                ORDER BY ordinal_position
                """;

        List<ColumnSchema> columns = jdbcTemplate.query(
                columnSql,
                (rs, rowNum) -> new ColumnSchema(
                        rs.getString("column_name"),
                        rs.getString("data_type"),
                        "YES".equalsIgnoreCase(rs.getString("is_nullable"))
                ),
                tableName
        );

        return new TableSchema(tableName, columns);
    }

    public Map<String, List<String>> compactSchema() {
        Map<String, List<String>> schema = new LinkedHashMap<>();
        inspectPublicSchema().forEach(table -> schema.put(
                table.name(),
                table.columns().stream().map(ColumnSchema::name).toList()
        ));
        return schema;
    }

    public record TableSchema(String name, List<ColumnSchema> columns) {}

    public record ColumnSchema(String name, String dataType, boolean nullable) {}
}
