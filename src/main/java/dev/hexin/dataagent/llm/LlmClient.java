package dev.hexin.dataagent.llm;

public interface LlmClient {
    String generate(String systemPrompt, String userPrompt);
}
