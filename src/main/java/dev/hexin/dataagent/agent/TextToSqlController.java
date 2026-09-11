package dev.hexin.dataagent.agent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class TextToSqlController {

    private final TextToSqlAgent agent;

    public TextToSqlController(TextToSqlAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question must not be blank"));
        }
        try {
            return ResponseEntity.ok(agent.ask(request.question().trim()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "LLM_CONFIGURATION_ERROR",
                    "message", ex.getMessage()
            ));
        } catch (TextToSqlAgent.AgentExecutionException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of(
                    "error", "AGENT_EXECUTION_FAILED",
                    "message", ex.getMessage()
            ));
        }
    }

    public record AskRequest(String question) {}
}
