package com.vigigate.backend.visitor.dto;

public record RiskDistributionResponse(
        long green,
        long yellow,
        long red) {
}
