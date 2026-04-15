package com.vigigate.backend.visitor.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.vigigate.backend.visitor.dto.RiskAssessment;
import com.vigigate.backend.visitor.model.RiskLevel;

@Service
public class RiskScoringService {

    public RiskAssessment assess(LocalDateTime visitTime, long weeklyVisitCount, long sameDayVisitCount) {
        int score = 10;
        List<String> factors = new ArrayList<>();
        int hour = visitTime.getHour();

        if (hour >= 23 || hour < 5) {
            score += 60;
            factors.add("Jam kunjungan berada di rentang larut malam");
        } else if (hour >= 20) {
            score += 35;
            factors.add("Jam kunjungan berada di luar jam operasional utama");
        } else if (hour < 7) {
            score += 20;
            factors.add("Kunjungan terjadi sebelum jam kerja normal");
        } else {
            score -= 5;
            factors.add("Jam kunjungan berada di rentang umum");
        }

        if (weeklyVisitCount >= 5) {
            score += 30;
            factors.add("Frekuensi kunjungan 7 hari terakhir sangat tinggi");
        } else if (weeklyVisitCount >= 3) {
            score += 20;
            factors.add("Frekuensi kunjungan 7 hari terakhir cukup tinggi");
        } else if (weeklyVisitCount >= 1) {
            score += 8;
            factors.add("Pengunjung sudah pernah datang dalam 7 hari terakhir");
        }

        if (sameDayVisitCount >= 2) {
            score += 15;
            factors.add("Terdapat beberapa kunjungan lain di hari yang sama");
        } else if (sameDayVisitCount == 1) {
            score += 8;
            factors.add("Sudah ada satu kunjungan lain di hari yang sama");
        }

        int finalScore = Math.max(5, Math.min(score, 100));
        RiskLevel level = determineLevel(finalScore);
        String summary = switch (level) {
            case GREEN -> "Risiko rendah, dapat diproses seperti kunjungan normal.";
            case YELLOW -> "Perlu verifikasi tambahan di pos keamanan.";
            case RED -> "Prioritaskan validasi identitas dan approval manual.";
        };

        return new RiskAssessment(finalScore, level, summary, List.copyOf(factors));
    }

    private RiskLevel determineLevel(int score) {
        if (score >= 70) {
            return RiskLevel.RED;
        }
        if (score >= 40) {
            return RiskLevel.YELLOW;
        }
        return RiskLevel.GREEN;
    }
}
