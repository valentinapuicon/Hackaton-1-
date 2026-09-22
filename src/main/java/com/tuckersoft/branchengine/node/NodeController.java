package com.tuckersoft.branchengine.node;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @PostMapping
    public ResponseEntity<NodeResponse> create(@Valid @RequestBody NodeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nodeService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<NodeResponse>> findAll() {
        return ResponseEntity.ok(nodeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NodeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(nodeService.findById(id));
    }
}
