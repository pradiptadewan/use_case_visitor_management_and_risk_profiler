package com.vigigate.backend.visitor.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

@Service
public class PhotoPlaceholderService {

    private static final String[][] COLOR_PAIRS = {
            {"#0b3c49", "#f2efe6"},
            {"#8b3d1d", "#fff1dc"},
            {"#114b5f", "#d9f0ff"},
            {"#5d2e46", "#f6dbe8"}
    };

    public String resolvePhotoDataUrl(String visitorName, String providedPhotoDataUrl) {
        if (providedPhotoDataUrl != null && !providedPhotoDataUrl.isBlank()) {
            return providedPhotoDataUrl;
        }

        String initials = extractInitials(visitorName);
        int paletteIndex = Math.abs(visitorName.hashCode()) % COLOR_PAIRS.length;
        String background = COLOR_PAIRS[paletteIndex][0];
        String foreground = COLOR_PAIRS[paletteIndex][1];
        String svg = """
                <svg xmlns='http://www.w3.org/2000/svg' width='180' height='180' viewBox='0 0 180 180'>
                    <rect width='180' height='180' rx='36' fill='%s'/>
                    <circle cx='90' cy='70' r='32' fill='%s' opacity='0.18'/>
                    <text x='50%%' y='56%%' dominant-baseline='middle' text-anchor='middle'
                        font-size='58' font-family='Trebuchet MS, Segoe UI Variable, sans-serif'
                        font-weight='700' fill='%s'>%s</text>
                </svg>
                """.formatted(background, foreground, foreground, initials);
        return "data:image/svg+xml;utf8," + URLEncoder.encode(svg, StandardCharsets.UTF_8);
    }

    private String extractInitials(String visitorName) {
        if (visitorName == null || visitorName.isBlank()) {
            return "VG";
        }

        String[] words = visitorName.trim().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
        }
        return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase();
    }
}
