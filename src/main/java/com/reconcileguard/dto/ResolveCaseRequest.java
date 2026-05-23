package com.reconcileguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveCaseRequest(
        @NotBlank
        @Size(max = 500)
        String resolutionNote
) {
}
