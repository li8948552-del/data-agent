package dev.hexin.dataagent.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hexin.dataagent.llm.LlmClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PythonAnalysisServiceTest {

    @Test
    void generatesRunsAndExplainsPythonAnalysis() {
        LlmClient llm = mock(LlmClient.class);
        PythonSandboxService sandbox = mock(PythonSandboxService.class);

        when(llm.generate(anyString(), anyString()))
                .thenReturn("print('total=30')")
                .thenReturn("The total is 30.");
        when(sandbox.execute(anyString(), any()))
                .thenReturn(new PythonSandboxService.SandboxResult("total=30", 0));

        PythonAnalysisService service = new PythonAnalysisService(llm, sandbox, new ObjectMapper());
        var result = service.analyze("What is the total?", List.of(Map.of("amount", 30)));

        assertEquals("print('total=30')", result.code());
        assertEquals("total=30", result.pythonOutput());
        assertEquals("The total is 30.", result.explanation());
        verify(sandbox).execute(eq("print('total=30')"), any());
    }
}
