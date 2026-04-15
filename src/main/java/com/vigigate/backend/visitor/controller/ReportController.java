package com.vigigate.backend.visitor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vigigate.backend.visitor.dto.SummaryReportResponse;
import com.vigigate.backend.visitor.service.SummaryReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final SummaryReportService summaryReportService;

    public ReportController(SummaryReportService summaryReportService) {
        this.summaryReportService = summaryReportService;
    }

    @GetMapping("/today-summary")
    public SummaryReportResponse getTodaySummary() {
        return summaryReportService.generateTodaySummary();
    }
}
