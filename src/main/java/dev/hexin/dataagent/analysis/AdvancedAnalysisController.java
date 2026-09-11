package dev.hexin.dataagent.analysis;

import dev.hexin.dataagent.agent.TextToSqlAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AdvancedAnalysisController {

    private final TextToSqlAgent textToSqlAgent;
    private final PythonAnalysisService pythonAnalysisService;

    public AdvancedAnalysisController(TextToSqlAgent textToSqlAgent, PythonAnalysisService pythonAnalysisService) {
        this.textToSqlAgent = textToSqlAgent;
        this.pythonAnalysisService = pythonAnalysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question must not be blank"));
        }

        try {
            TextToSqlAgent.AgentAnswer sqlAnswer = textToSqlAgent.ask(request.question().trim());
            PythonAnalysisService.AnalysisResult analysis = pythonAnalysisService.analyze(
                    request.question().trim(), sqlAnswer.rows());

            return ResponseEntity.ok(new AdvancedAnalysisResponse(
                    sqlAnswer.question(),
                    sqlAnswer.sql(),
                    sqlAnswer.rows(),
                    sqlAnswer.explanation(),
                    sqlAnswer.retrievedContext(),
                    sqlAnswer.attempts(),
                    analysis.code(),
                    analysis.pythonOutput(),
                    analysis.explanation()
            ));
        } catch (PythonSandboxService.SandboxExecutionException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of(
                    "error", "PYTHON_SANDBOX_FAILED",
                    "message", ex.getMessage()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of(
                    "error", "ANALYSIS_FAILED",
                    "message", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
            ));
        }
    }

    public record AnalyzeRequest(String question) {}

    public record AdvancedAnalysisResponse(
            String question,
            String sql,
            Object rows,
            String sqlExplanation,
            String retrievedContext,
            int sqlAttempts,
            String pythonCode,
            String pythonOutput,
            String analysis
    ) {}
}
