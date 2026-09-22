package com.tuckersoft.branchengine.decision;

import java.util.List;

public record DecisionPageResponse(List<DecisionResponse> content, long totalElements,
                                   int totalPages, int currentPage, int size) {}
