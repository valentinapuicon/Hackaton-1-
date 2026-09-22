package com.tuckersoft.branchengine.playthrough;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {

    private final PlaythroughService playthroughService;

    @PostMapping
    public ResponseEntity<PlaythroughResponse> create(@Valid @RequestBody PlaythroughRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playthroughService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<PlaythroughResponse>> findAll() {
        return ResponseEntity.ok(playthroughService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaythroughResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(playthroughService.findById(id));
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<PathResponse> path(@PathVariable Long id) {
        return ResponseEntity.ok(playthroughService.path(id));
    }
}
