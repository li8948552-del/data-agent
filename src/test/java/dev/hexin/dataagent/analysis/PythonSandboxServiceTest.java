package dev.hexin.dataagent.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PythonSandboxServiceTest {

    @Test
    void stripsPythonMarkdownFence() {
        assertEquals(
                "print('ok')",
                PythonSandboxService.cleanCode("```python\nprint('ok')\n```")
        );
    }
}
