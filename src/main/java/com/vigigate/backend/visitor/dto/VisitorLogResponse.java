package com.vigigate.backend.visitor.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.vigigate.backend.visitor.model.RiskLevel;
import com.vigigate.backend.visitor.model.VisitStatus;

public record VisitorLogResponse(
        Long id,
        String visitorName,
        String nik,
        String destination,
        String purpose,
        String photoDataUrl,
        String notes,
        LocalDateTime visitTime,
        LocalDateTime checkoutTime,
        Integer riskScore,
        RiskLevel riskLevel,
        String riskSummary,
        List<String> riskFactors,
        VisitStatus visitStatus,
        long durationMinutes) {
}
