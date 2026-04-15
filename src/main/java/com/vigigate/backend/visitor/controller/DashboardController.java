package com.vigigate.backend.visitor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vigigate.backend.visitor.dto.DashboardOverviewResponse;
import com.vigigate.backend.visitor.service.VisitorService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final VisitorService visitorService;

    public DashboardController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @GetMapping("/overview")
    public DashboardOverviewResponse getOverview() {
        return visitorService.getDashboardOverview();
    }
}
