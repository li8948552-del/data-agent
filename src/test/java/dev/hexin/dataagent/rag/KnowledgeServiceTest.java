package dev.hexin.dataagent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeServiceTest {

    @Test
    void serializesEmbeddingAsPgVectorLiteral() {
        assertEquals("[0.1,-0.2,3.0]", KnowledgeService.toPgVector(List.of(0.1, -0.2, 3.0)));
    }
}
