package dev.hexin.dataagent.agent;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class StreamingAgentController {

    private final TextToSqlAgent agent;

    public StreamingAgentController(TextToSqlAgent agent) {
        this.agent = agent;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String question) {
        SseEmitter emitter = new SseEmitter(120_000L);

        if (question == null || question.isBlank()) {
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", "Question must not be blank")));
            } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }

        Thread.startVirtualThread(() -> {
            try {
                agent.ask(question.trim(), (stage, payload) -> send(emitter, stage, payload));
                emitter.complete();
            } catch (RuntimeException ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of(
                            "message", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
                    )));
                } catch (IOException ignored) {}
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    private static void send(SseEmitter emitter, String stage, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(stage).data(payload));
        } catch (IOException ex) {
            throw new IllegalStateException("SSE client disconnected", ex);
        }
    }
}
