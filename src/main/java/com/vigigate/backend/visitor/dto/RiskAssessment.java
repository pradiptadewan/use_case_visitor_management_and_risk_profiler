package com.vigigate.backend.visitor.dto;

import java.util.List;

import com.vigigate.backend.visitor.model.RiskLevel;

public record RiskAssessment(
        int score,
        RiskLevel level,
        String summary,
        List<String> factors) {
}
