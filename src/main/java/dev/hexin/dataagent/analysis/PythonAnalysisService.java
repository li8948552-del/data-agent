package dev.hexin.dataagent.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hexin.dataagent.llm.LlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PythonAnalysisService {

    private final LlmClient llmClient;
    private final PythonSandboxService sandboxService;
    private final ObjectMapper objectMapper;

    public PythonAnalysisService(LlmClient llmClient, PythonSandboxService sandboxService, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.sandboxService = sandboxService;
        this.objectMapper = objectMapper;
    }

    public AnalysisResult analyze(String question, List<Map<String, Object>> rows) {
        String dataPreview = toJson(rows);
        String code = llmClient.generate(systemPrompt(), """
                User question:
                %s

                Query result JSON preview:
                %s

                Write Python code that reads the full dataset from /work/data.json and prints a concise machine-readable or human-readable analysis to stdout.
                """.formatted(question, dataPreview));

        PythonSandboxService.SandboxResult sandbox = sandboxService.execute(code, rows);

        String explanation = llmClient.generate(
                "You are a careful data analyst. Use only the supplied Python output and user question. Do not invent facts.",
                "Question:\n" + question + "\n\nPython output:\n" + sandbox.output()
        );

        return new AnalysisResult(PythonSandboxService.cleanCode(code), sandbox.output(), explanation);
    }

    private String systemPrompt() {
        return """
                You are generating Python for a locked-down analytics sandbox.
                Return Python code only, with no markdown fences.
                Read input from /work/data.json using Python's json module.
                The container has only the Python standard library; do not require pandas, numpy, requests, or other third-party packages.
                Do not access the network, environment variables, subprocesses, the Docker socket, or files other than /work/data.json.
                Keep runtime bounded and print the final result to stdout.
                """;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize query rows for Python analysis", ex);
        }
    }

    public record AnalysisResult(String code, String pythonOutput, String explanation) {}
}
