package dev.hexin.dataagent.rag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping
    public ResponseEntity<?> index(@RequestBody IndexRequest request) {
        if (request.title() == null || request.title().isBlank() || request.content() == null || request.content().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "title and content are required"));
        }
        long id = knowledgeService.index(request.title().trim(), request.content().trim());
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping("/search")
    public List<KnowledgeService.KnowledgeChunk> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "4") int topK) {
        return knowledgeService.retrieve(q, topK);
    }

    public record IndexRequest(String title, String content) {}
}
