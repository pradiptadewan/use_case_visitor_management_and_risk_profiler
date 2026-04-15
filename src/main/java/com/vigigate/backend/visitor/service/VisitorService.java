package com.vigigate.backend.visitor.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vigigate.backend.visitor.dto.DashboardOverviewResponse;
import com.vigigate.backend.visitor.dto.RegisterVisitorRequest;
import com.vigigate.backend.visitor.dto.RiskAssessment;
import com.vigigate.backend.visitor.dto.RiskDistributionResponse;
import com.vigigate.backend.visitor.dto.VisitorLogResponse;
import com.vigigate.backend.visitor.exception.ResourceNotFoundException;
import com.vigigate.backend.visitor.model.RiskLevel;
import com.vigigate.backend.visitor.model.VisitStatus;
import com.vigigate.backend.visitor.model.VisitorLog;
import com.vigigate.backend.visitor.repository.VisitorLogRepository;

@Service
@Transactional
public class VisitorService {

    private static final String RISK_FACTOR_DELIMITER = "||";

    private final VisitorLogRepository visitorLogRepository;
    private final RiskScoringService riskScoringService;
    private final PhotoPlaceholderService photoPlaceholderService;
    private final Clock clock;

    public VisitorService(
            VisitorLogRepository visitorLogRepository,
            RiskScoringService riskScoringService,
            PhotoPlaceholderService photoPlaceholderService,
            Clock clock) {
        this.visitorLogRepository = visitorLogRepository;
        this.riskScoringService = riskScoringService;
        this.photoPlaceholderService = photoPlaceholderService;
        this.clock = clock;
    }

    public VisitorLogResponse registerVisitor(RegisterVisitorRequest request) {
        LocalDateTime visitTime = request.visitTime();
        long weeklyVisitCount = visitorLogRepository.countByNikAndVisitTimeBetween(
                request.nik(),
                visitTime.minusDays(7),
                visitTime);
        long sameDayVisitCount = visitorLogRepository.countByNikAndVisitTimeBetween(
                request.nik(),
                visitTime.toLocalDate().atStartOfDay(),
                visitTime);

        RiskAssessment assessment = riskScoringService.assess(visitTime, weeklyVisitCount, sameDayVisitCount);

        VisitorLog visitorLog = new VisitorLog();
        visitorLog.setVisitorName(clean(request.visitorName()));
        visitorLog.setNik(request.nik());
        visitorLog.setDestination(clean(request.destination()));
        visitorLog.setPurpose(clean(request.purpose()));
        visitorLog.setPhotoDataUrl(photoPlaceholderService.resolvePhotoDataUrl(request.visitorName(), request.photoDataUrl()));
        visitorLog.setNotes(cleanNullable(request.notes()));
        visitorLog.setVisitTime(visitTime);
        visitorLog.setRiskScore(assessment.score());
        visitorLog.setRiskLevel(assessment.level());
        visitorLog.setRiskSummary(assessment.summary());
        visitorLog.setRiskFactors(String.join(RISK_FACTOR_DELIMITER, assessment.factors()));
        visitorLog.setVisitStatus(VisitStatus.ACTIVE);

        return toResponse(visitorLogRepository.save(visitorLog));
    }

    @Transactional(readOnly = true)
    public List<VisitorLogResponse> getActiveVisitors() {
        return visitorLogRepository.findByVisitStatusOrderByVisitTimeDesc(VisitStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VisitorLogResponse> getTodayVisitors() {
        LocalDateTime start = startOfToday();
        LocalDateTime end = start.plusDays(1);
        return visitorLogRepository.findByVisitTimeBetweenOrderByVisitTimeDesc(start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VisitorLogResponse checkoutVisitor(Long visitorId) {
        VisitorLog visitorLog = visitorLogRepository.findById(visitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Data tamu tidak ditemukan"));

        if (visitorLog.getVisitStatus() == VisitStatus.ACTIVE) {
            visitorLog.setVisitStatus(VisitStatus.CHECKED_OUT);
            visitorLog.setCheckoutTime(LocalDateTime.now(clock));
        }

        return toResponse(visitorLogRepository.save(visitorLog));
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getDashboardOverview() {
        LocalDateTime start = startOfToday();
        LocalDateTime end = start.plusDays(1);
        List<VisitorLog> todayVisits = visitorLogRepository.findByVisitTimeBetweenOrderByVisitTimeDesc(start, end);
        List<VisitorLogResponse> activeLogs = visitorLogRepository.findByVisitStatusOrderByVisitTimeDesc(VisitStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();

        double averageRisk = todayVisits.stream()
                .mapToInt(VisitorLog::getRiskScore)
                .average()
                .orElse(0.0);
        double roundedAverageRisk = Math.round(averageRisk * 10.0) / 10.0;

        long green = todayVisits.stream().filter(log -> log.getRiskLevel() == RiskLevel.GREEN).count();
        long yellow = todayVisits.stream().filter(log -> log.getRiskLevel() == RiskLevel.YELLOW).count();
        long red = todayVisits.stream().filter(log -> log.getRiskLevel() == RiskLevel.RED).count();

        return new DashboardOverviewResponse(
                todayVisits.size(),
                activeLogs.size(),
                todayVisits.size() - activeLogs.size(),
                roundedAverageRisk,
                resolveBusiestWindow(todayVisits),
                new RiskDistributionResponse(green, yellow, red),
                activeLogs,
                todayVisits.stream()
                        .limit(8)
                        .map(this::toResponse)
                        .toList());
    }

    @Transactional(readOnly = true)
    public VisitorLogResponse toResponse(VisitorLog visitorLog) {
        return new VisitorLogResponse(
                visitorLog.getId(),
                visitorLog.getVisitorName(),
                visitorLog.getNik(),
                visitorLog.getDestination(),
                visitorLog.getPurpose(),
                visitorLog.getPhotoDataUrl(),
                visitorLog.getNotes(),
                visitorLog.getVisitTime(),
                visitorLog.getCheckoutTime(),
                visitorLog.getRiskScore(),
                visitorLog.getRiskLevel(),
                visitorLog.getRiskSummary(),
                splitFactors(visitorLog.getRiskFactors()),
                visitorLog.getVisitStatus(),
                resolveDurationMinutes(visitorLog));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<String> splitFactors(String rawFactors) {
        if (rawFactors == null || rawFactors.isBlank()) {
            return List.of();
        }
        return List.of(rawFactors.split("\\Q" + RISK_FACTOR_DELIMITER + "\\E"));
    }

    private long resolveDurationMinutes(VisitorLog visitorLog) {
        LocalDateTime endTime = visitorLog.getCheckoutTime() == null
                ? LocalDateTime.now(clock)
                : visitorLog.getCheckoutTime();
        long minutes = Duration.between(visitorLog.getVisitTime(), endTime).toMinutes();
        return Math.max(minutes, 0);
    }

    private String resolveBusiestWindow(List<VisitorLog> todayVisits) {
        if (todayVisits.isEmpty()) {
            return "Belum ada kunjungan hari ini";
        }

        Map<Integer, Long> hourlyCounts = todayVisits.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        visit -> visit.getVisitTime().getHour(),
                        java.util.stream.Collectors.counting()));

        Map.Entry<Integer, Long> busiestEntry = hourlyCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        int hour = busiestEntry.getKey();
        return "%02d:00 - %02d:59 (%d kunjungan)".formatted(hour, hour, busiestEntry.getValue());
    }

    private LocalDateTime startOfToday() {
        return LocalDate.now(clock).atStartOfDay();
    }
}
