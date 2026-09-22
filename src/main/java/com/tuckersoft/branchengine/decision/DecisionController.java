package com.tuckersoft.branchengine.decision;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;
    private final DecisionQueryService decisionQueryService;

    @PostMapping
    public ResponseEntity<DecisionResponse> create(
            @Valid @RequestBody DecisionRequest req,
            @RequestHeader(value = "X-Bandersnatch-Simulate", required = false) String simulate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionService.create(req, simulate));
    }

    @GetMapping
    public ResponseEntity<DecisionPageResponse> list(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(decisionQueryService.list(branchType, impactLevel, status, playthroughId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecisionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(decisionQueryService.findById(id));
    }

    @GetMapping("/{id}/reality-logs")
    public ResponseEntity<List<RealityLogResponse>> realityLogs(@PathVariable Long id) {
        return ResponseEntity.ok(decisionQueryService.realityLogs(id));
    }
}
