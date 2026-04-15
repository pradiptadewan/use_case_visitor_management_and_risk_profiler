package com.vigigate.backend.visitor.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.vigigate.backend.visitor.dto.RiskAssessment;
import com.vigigate.backend.visitor.model.RiskLevel;

class RiskScoringServiceTest {

    private final RiskScoringService riskScoringService = new RiskScoringService();

    @Test
    void shouldReturnGreenForNormalWorkingHourAndFirstVisit() {
        RiskAssessment assessment = riskScoringService.assess(
                LocalDateTime.of(2026, 4, 15, 10, 0),
                0,
                0);

        assertThat(assessment.level()).isEqualTo(RiskLevel.GREEN);
        assertThat(assessment.score()).isLessThan(40);
    }

    @Test
    void shouldReturnRedForLateNightFrequentVisitor() {
        RiskAssessment assessment = riskScoringService.assess(
                LocalDateTime.of(2026, 4, 15, 23, 10),
                5,
                2);

        assertThat(assessment.level()).isEqualTo(RiskLevel.RED);
        assertThat(assessment.score()).isGreaterThanOrEqualTo(70);
    }
}
