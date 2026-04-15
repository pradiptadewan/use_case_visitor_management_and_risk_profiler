package com.vigigate.backend.visitor.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.vigigate.backend.visitor.dto.RiskAssessment;
import com.vigigate.backend.visitor.model.VisitStatus;
import com.vigigate.backend.visitor.model.VisitorLog;
import com.vigigate.backend.visitor.repository.VisitorLogRepository;
import com.vigigate.backend.visitor.service.PhotoPlaceholderService;
import com.vigigate.backend.visitor.service.RiskScoringService;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final String RISK_FACTOR_DELIMITER = "||";

    private final VisitorLogRepository visitorLogRepository;
    private final RiskScoringService riskScoringService;
    private final PhotoPlaceholderService photoPlaceholderService;
    private final Clock clock;
    private final boolean seedDemoData;

    public DemoDataSeeder(
            VisitorLogRepository visitorLogRepository,
            RiskScoringService riskScoringService,
            PhotoPlaceholderService photoPlaceholderService,
            Clock clock,
            @Value("${app.seed-demo-data:true}") boolean seedDemoData) {
        this.visitorLogRepository = visitorLogRepository;
        this.riskScoringService = riskScoringService;
        this.photoPlaceholderService = photoPlaceholderService;
        this.clock = clock;
        this.seedDemoData = seedDemoData;
    }

    @Override
    public void run(String... args) {
        if (!seedDemoData || visitorLogRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        List<SeedVisit> seedVisits = List.of(
                new SeedVisit("Rian Saputra", "3174012101900001", "Cluster Dahlia / Rumah 12", "Survey teknisi jaringan", today.minusDays(3).atTime(14, 10), today.minusDays(3).atTime(15, 5), "Riwayat kunjungan sebelumnya"),
                new SeedVisit("Rian Saputra", "3174012101900001", "Cluster Dahlia / Rumah 12", "Follow up maintenance", today.minusDays(1).atTime(21, 25), today.minusDays(1).atTime(22, 5), "Datang malam untuk follow up"),
                new SeedVisit("Maya Kartika", "3174011502910002", "Tower B / Lantai 5", "Meeting tenant", today.atTime(8, 40), today.atTime(10, 0), "Pertemuan pagi"),
                new SeedVisit("Sari Wulandari", "3174010803920003", "Office Block C / Unit 3", "Antar dokumen legal", today.atTime(10, 15), today.atTime(11, 0), "Dokumen vendor"),
                new SeedVisit("Dimas Pratama", "3174010101950004", "Tower A / Unit 8", "Interview kandidat", today.atTime(19, 45), null, "Masih berada di area"),
                new SeedVisit("Rian Saputra", "3174012101900001", "Cluster Dahlia / Rumah 12", "Akses malam untuk pengecekan akhir", today.atTime(22, 10), null, "Kunjungan berulang di jam rawan"));

        seedVisits.forEach(this::saveSeedVisit);
    }

    private void saveSeedVisit(SeedVisit seedVisit) {
        long weeklyVisitCount = visitorLogRepository.countByNikAndVisitTimeBetween(
                seedVisit.nik(),
                seedVisit.visitTime().minusDays(7),
                seedVisit.visitTime());
        long sameDayVisitCount = visitorLogRepository.countByNikAndVisitTimeBetween(
                seedVisit.nik(),
                seedVisit.visitTime().toLocalDate().atStartOfDay(),
                seedVisit.visitTime());
        RiskAssessment assessment = riskScoringService.assess(seedVisit.visitTime(), weeklyVisitCount, sameDayVisitCount);

        VisitorLog visitorLog = new VisitorLog();
        visitorLog.setVisitorName(seedVisit.visitorName());
        visitorLog.setNik(seedVisit.nik());
        visitorLog.setDestination(seedVisit.destination());
        visitorLog.setPurpose(seedVisit.purpose());
        visitorLog.setPhotoDataUrl(photoPlaceholderService.resolvePhotoDataUrl(seedVisit.visitorName(), null));
        visitorLog.setNotes(seedVisit.notes());
        visitorLog.setVisitTime(seedVisit.visitTime());
        visitorLog.setCheckoutTime(seedVisit.checkoutTime());
        visitorLog.setRiskScore(assessment.score());
        visitorLog.setRiskLevel(assessment.level());
        visitorLog.setRiskSummary(assessment.summary());
        visitorLog.setRiskFactors(String.join(RISK_FACTOR_DELIMITER, assessment.factors()));
        visitorLog.setVisitStatus(seedVisit.checkoutTime() == null ? VisitStatus.ACTIVE : VisitStatus.CHECKED_OUT);
        visitorLogRepository.save(visitorLog);
    }

    private record SeedVisit(
            String visitorName,
            String nik,
            String destination,
            String purpose,
            LocalDateTime visitTime,
            LocalDateTime checkoutTime,
            String notes) {
    }
}
