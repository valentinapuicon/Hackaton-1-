package com.tuckersoft.branchengine.node;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NodeRequest(
        @NotBlank @Size(min = 3, max = 40) String nodeCode,
        @NotBlank @Size(min = 3, max = 80) String title,
        @NotBlank @Size(min = 10) String sceneText,
        @NotNull @Positive Integer branchCapacity,
        String primaryBranchCode,
        String glitchBranchCode
) {}
