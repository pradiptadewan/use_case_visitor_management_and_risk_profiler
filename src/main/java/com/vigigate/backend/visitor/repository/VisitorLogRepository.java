package com.vigigate.backend.visitor.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vigigate.backend.visitor.model.RiskLevel;
import com.vigigate.backend.visitor.model.VisitStatus;
import com.vigigate.backend.visitor.model.VisitorLog;

public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {

    List<VisitorLog> findByVisitStatusOrderByVisitTimeDesc(VisitStatus visitStatus);

    List<VisitorLog> findByVisitTimeBetweenOrderByVisitTimeDesc(LocalDateTime start, LocalDateTime end);

    long countByNikAndVisitTimeBetween(String nik, LocalDateTime start, LocalDateTime end);

    long countByVisitTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByVisitStatusAndVisitTimeBetween(VisitStatus visitStatus, LocalDateTime start, LocalDateTime end);

    long countByRiskLevelAndVisitTimeBetween(RiskLevel riskLevel, LocalDateTime start, LocalDateTime end);
}
