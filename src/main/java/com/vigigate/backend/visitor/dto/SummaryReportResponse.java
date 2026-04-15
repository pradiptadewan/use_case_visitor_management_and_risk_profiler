package com.vigigate.backend.visitor.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SummaryReportResponse(
        LocalDateTime generatedAt,
        String headline,
        List<String> highlights,
        List<String> recommendations,
        String narrative) {
}
