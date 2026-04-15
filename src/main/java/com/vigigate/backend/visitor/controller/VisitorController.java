package com.vigigate.backend.visitor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vigigate.backend.visitor.dto.RegisterVisitorRequest;
import com.vigigate.backend.visitor.dto.VisitorLogResponse;
import com.vigigate.backend.visitor.service.VisitorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/visitors")
public class VisitorController {

    private final VisitorService visitorService;

    public VisitorController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitorLogResponse registerVisitor(@Valid @RequestBody RegisterVisitorRequest request) {
        return visitorService.registerVisitor(request);
    }

    @GetMapping("/active")
    public List<VisitorLogResponse> getActiveVisitors() {
        return visitorService.getActiveVisitors();
    }

    @GetMapping("/today")
    public List<VisitorLogResponse> getTodayVisitors() {
        return visitorService.getTodayVisitors();
    }

    @PatchMapping("/{visitorId}/checkout")
    public VisitorLogResponse checkoutVisitor(@PathVariable Long visitorId) {
        return visitorService.checkoutVisitor(visitorId);
    }
}
