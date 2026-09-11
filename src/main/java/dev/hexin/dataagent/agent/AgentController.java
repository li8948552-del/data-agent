package dev.hexin.dataagent.agent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "data-agent");
    }

    @PostMapping("/plan")
    public ResponseEntity<AgentPlan> plan(@RequestBody QuestionRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(AgentPlan.draft(request.question().trim()));
    }

    public record QuestionRequest(String question) {}
}
