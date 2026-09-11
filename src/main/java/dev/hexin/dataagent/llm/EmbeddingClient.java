package dev.hexin.dataagent.llm;

import java.util.List;

public interface EmbeddingClient {
    List<Double> embed(String text);
}
