package dev.hexin.dataagent.agent;

@FunctionalInterface
public interface AgentProgressListener {
    void onEvent(String stage, Object payload);

    static AgentProgressListener noop() {
        return (stage, payload) -> {};
    }
}
