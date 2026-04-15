package com.vigigate.backend.visitor.dto;

import java.util.List;

public record DashboardOverviewResponse(
        long todayTotal,
        long activeVisitors,
        long checkedOutVisitors,
        double averageRiskScore,
        String busiestWindow,
        RiskDistributionResponse riskDistribution,
        List<VisitorLogResponse> activeLogs,
        List<VisitorLogResponse> recentVisits) {
}
