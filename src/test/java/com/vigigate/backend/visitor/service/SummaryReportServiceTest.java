package com.vigigate.backend.visitor.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vigigate.backend.visitor.dto.SummaryReportResponse;
import com.vigigate.backend.visitor.model.RiskLevel;
import com.vigigate.backend.visitor.model.VisitStatus;
import com.vigigate.backend.visitor.model.VisitorLog;
import com.vigigate.backend.visitor.repository.VisitorLogRepository;

class SummaryReportServiceTest {

    @Test
    void shouldGenerateNarrativeForTodayVisits() {
        VisitorLogRepository repository = Mockito.mock(VisitorLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-15T10:15:30Z"), ZoneId.of("Asia/Jakarta"));
        SummaryReportService summaryReportService = new SummaryReportService(repository, clock);

        VisitorLog visitorLog = new VisitorLog();
        visitorLog.setVisitorName("Rian Saputra");
        visitorLog.setNik("3174012101900001");
        visitorLog.setVisitTime(LocalDateTime.of(2026, 4, 15, 22, 10));
        visitorLog.setVisitStatus(VisitStatus.ACTIVE);
        visitorLog.setRiskLevel(RiskLevel.RED);
        visitorLog.setRiskScore(84);

        Mockito.when(repository.findByVisitTimeBetweenOrderByVisitTimeDesc(
                LocalDateTime.of(2026, 4, 15, 0, 0),
                LocalDateTime.of(2026, 4, 16, 0, 0)))
                .thenReturn(List.of(visitorLog));

        SummaryReportResponse response = summaryReportService.generateTodaySummary();

        assertThat(response.headline()).contains("WASPADA TINGGI");
        assertThat(response.highlights()).isNotEmpty();
        assertThat(response.recommendations()).isNotEmpty();
    }
}
