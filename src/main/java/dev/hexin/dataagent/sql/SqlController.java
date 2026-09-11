package dev.hexin.dataagent.sql;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class SqlController {

    private final SchemaService schemaService;
    private final ReadOnlySqlExecutor executor;

    public SqlController(SchemaService schemaService, ReadOnlySqlExecutor executor) {
        this.schemaService = schemaService;
        this.executor = executor;
    }

    @GetMapping("/schema")
    public List<SchemaService.TableSchema> schema() {
        return schemaService.inspectPublicSchema();
    }

    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody SqlRequest request) {
        try {
            return ResponseEntity.ok(executor.execute(request.sql()));
        } catch (ReadOnlySqlExecutor.UnsafeSqlException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "UNSAFE_SQL",
                    "message", ex.getMessage()
            ));
        }
    }

    public record SqlRequest(String sql) {}
}
