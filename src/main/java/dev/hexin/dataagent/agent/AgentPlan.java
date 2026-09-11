package dev.hexin.dataagent.agent;

import java.util.List;

public record AgentPlan(String question, List<String> steps, String status) {
    public static AgentPlan draft(String question) {
        return new AgentPlan(question, List.of(
                "Inspect database schema",
                "Translate the question into SQL",
                "Validate SQL before execution",
                "Execute query with read-only safeguards",
                "Explain the result"
        ), "AWAITING_APPROVAL");
    }
}
