package dev.hexin.dataagent.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PythonSandboxService {

    private final ObjectMapper objectMapper;
    private final String image;
    private final long timeoutSeconds;
    private final int maxOutputChars;

    public PythonSandboxService(
            ObjectMapper objectMapper,
            @Value("${agent.python.image:python:3.12-slim}") String image,
            @Value("${agent.python.timeout-seconds:10}") long timeoutSeconds,
            @Value("${agent.python.max-output-chars:12000}") int maxOutputChars) {
        this.objectMapper = objectMapper;
        this.image = image;
        this.timeoutSeconds = timeoutSeconds;
        this.maxOutputChars = maxOutputChars;
    }

    public SandboxResult execute(String code, Object data) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("data-agent-python-");
            Files.writeString(workDir.resolve("analysis.py"), cleanCode(code), StandardCharsets.UTF_8);
            objectMapper.writeValue(workDir.resolve("data.json").toFile(), data);

            List<String> command = List.of(
                    "docker", "run", "--rm",
                    "--network", "none",
                    "--read-only",
                    "--cap-drop", "ALL",
                    "--pids-limit", "64",
                    "--memory", "256m",
                    "--cpus", "0.5",
                    "--tmpfs", "/tmp:rw,noexec,nosuid,size=64m",
                    "-v", workDir.toAbsolutePath() + ":/work:ro",
                    image,
                    "python", "-I", "/work/analysis.py"
            );

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                throw new SandboxExecutionException("Python analysis exceeded " + timeoutSeconds + " seconds");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            output = truncate(output, maxOutputChars);

            if (process.exitValue() != 0) {
                throw new SandboxExecutionException("Python exited with code " + process.exitValue() + ": " + output);
            }

            return new SandboxResult(output.trim(), process.exitValue());
        } catch (IOException ex) {
            throw new SandboxExecutionException("Could not start Docker Python sandbox: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SandboxExecutionException("Python sandbox execution was interrupted", ex);
        } finally {
            deleteRecursively(workDir);
        }
    }

    static String cleanCode(String code) {
        if (code == null) return "";
        String cleaned = code.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:python)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned.trim();
    }

    private static String truncate(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        return value.substring(0, maxChars) + "\n...[output truncated]";
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }

    public record SandboxResult(String output, int exitCode) {}

    public static class SandboxExecutionException extends RuntimeException {
        public SandboxExecutionException(String message) { super(message); }
        public SandboxExecutionException(String message, Throwable cause) { super(message, cause); }
    }
}
