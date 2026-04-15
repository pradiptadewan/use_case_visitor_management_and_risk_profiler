package com.vigigate.backend.visitor.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vigigate.backend.visitor.dto.SummaryReportResponse;
import com.vigigate.backend.visitor.model.RiskLevel;
import com.vigigate.backend.visitor.model.VisitStatus;
import com.vigigate.backend.visitor.model.VisitorLog;
import com.vigigate.backend.visitor.repository.VisitorLogRepository;

@Service
@Transactional(readOnly = true)
public class SummaryReportService {

    private final VisitorLogRepository visitorLogRepository;
    private final Clock clock;

    public SummaryReportService(VisitorLogRepository visitorLogRepository, Clock clock) {
        this.visitorLogRepository = visitorLogRepository;
        this.clock = clock;
    }

    public SummaryReportResponse generateTodaySummary() {
        LocalDateTime start = LocalDate.now(clock).atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<VisitorLog> visits = visitorLogRepository.findByVisitTimeBetweenOrderByVisitTimeDesc(start, end);
        LocalDateTime generatedAt = LocalDateTime.now(clock);

        if (visits.isEmpty()) {
            return new SummaryReportResponse(
                    generatedAt,
                    "Belum ada aktivitas kunjungan untuk dirangkum hari ini.",
                    List.of(
                            "Dashboard masih kosong sehingga risiko operasional hari ini rendah.",
                            "Sistem siap menerima pendaftaran tamu berikutnya."),
                    List.of(
                            "Siapkan satu skenario demo registrasi agar fitur scoring bisa langsung terlihat.",
                            "Pastikan petugas mengetahui cara checkout tamu setelah sesi demo."),
                    "Hari ini belum ada data kunjungan yang terekam. Saat tamu pertama didaftarkan, sistem akan langsung menghitung skor risiko dan memperbarui log aktif secara real-time.");
        }

        long activeVisitors = visits.stream().filter(visit -> visit.getVisitStatus() == VisitStatus.ACTIVE).count();
        long redVisitors = visits.stream().filter(visit -> visit.getRiskLevel() == RiskLevel.RED).count();
        long yellowVisitors = visits.stream().filter(visit -> visit.getRiskLevel() == RiskLevel.YELLOW).count();
        long greenVisitors = visits.stream().filter(visit -> visit.getRiskLevel() == RiskLevel.GREEN).count();
        long afterHoursVisitors = visits.stream()
                .filter(visit -> {
                    int hour = visit.getVisitTime().getHour();
                    return hour < 7 || hour >= 20;
                })
                .count();

        String busiestWindow = resolveBusiestWindow(visits);
        String frequentVisitorSummary = resolveFrequentVisitorSummary(visits);

        List<String> highlights = new ArrayList<>();
        highlights.add("%d kunjungan tercatat hari ini, dengan %d tamu masih berada di dalam area.".formatted(visits.size(), activeVisitors));
        highlights.add("Distribusi risiko hari ini: Green %d, Yellow %d, Red %d.".formatted(greenVisitors, yellowVisitors, redVisitors));
        highlights.add("Periode tersibuk terjadi pada %s.".formatted(busiestWindow));
        if (afterHoursVisitors > 0) {
            highlights.add("%d kunjungan terjadi di luar jam operasional utama dan perlu perhatian satpam.".formatted(afterHoursVisitors));
        }
        if (!frequentVisitorSummary.isBlank()) {
            highlights.add(frequentVisitorSummary);
        }

        List<String> recommendations = new ArrayList<>();
        if (redVisitors > 0) {
            recommendations.add("Wajibkan verifikasi identitas manual untuk seluruh tamu berstatus Red sebelum akses diberikan.");
        }
        if (yellowVisitors > 0) {
            recommendations.add("Tambahkan pertanyaan konfirmasi tujuan untuk tamu berstatus Yellow di pos keamanan.");
        }
        if (activeVisitors > 0) {
            recommendations.add("Lakukan pengecekan ulang terhadap tamu yang masih aktif agar tidak ada log yang tertinggal checkout.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Arus kunjungan relatif stabil. Fokuskan petugas pada pencatatan yang konsisten dan checkout tepat waktu.");
        }

        String narrative = """
                Ringkasan hari ini menunjukkan %d kunjungan dengan profil risiko yang tersebar dari Green hingga Red.
                Titik aktivitas tertinggi berada pada %s, sementara %d tamu masih tercatat aktif di dalam area.
                Pola kunjungan berisiko lebih tinggi terutama muncul pada tamu yang datang di luar jam operasional utama atau memiliki frekuensi kunjungan berulang dalam beberapa hari terakhir.
                """.formatted(visits.size(), busiestWindow, activeVisitors).replace("\n", " ").trim();

        return new SummaryReportResponse(
                generatedAt,
                "Aktivitas kunjungan hari ini berada pada level %s.".formatted(resolveRiskPosture(redVisitors, yellowVisitors)),
                List.copyOf(highlights),
                List.copyOf(recommendations),
                narrative);
    }

    private String resolveBusiestWindow(List<VisitorLog> visits) {
        Map<Integer, Long> hourlyCounts = visits.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        visit -> visit.getVisitTime().getHour(),
                        java.util.stream.Collectors.counting()));
        Map.Entry<Integer, Long> busiest = hourlyCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        int hour = busiest.getKey();
        return "%02d:00 - %02d:59 (%d kunjungan)".formatted(hour, hour, busiest.getValue());
    }

    private String resolveFrequentVisitorSummary(List<VisitorLog> visits) {
        return visits.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        visit -> visit.getVisitorName() + " (" + visit.getNik() + ")",
                        java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(2)
                .map(entry -> "%s muncul %d kali".formatted(entry.getKey(), entry.getValue()))
                .reduce((left, right) -> left + "; " + right)
                .map(value -> "Pengunjung yang paling sering kembali hari ini: " + value + ".")
                .orElse("");
    }

    private String resolveRiskPosture(long redVisitors, long yellowVisitors) {
        if (redVisitors > 0) {
            return "WASPADA TINGGI";
        }
        if (yellowVisitors > 0) {
            return "PERLU MONITORING";
        }
        return "TERKENDALI";
    }
}
