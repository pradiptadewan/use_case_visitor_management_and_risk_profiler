package com.vigigate.backend.visitor.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterVisitorRequest(
        @NotBlank(message = "Nama tamu wajib diisi")
        @Size(max = 120, message = "Nama tamu maksimal 120 karakter")
        String visitorName,

        @NotBlank(message = "NIK wajib diisi")
        @Pattern(regexp = "\\d{16}", message = "NIK harus 16 digit angka")
        String nik,

        @NotBlank(message = "Tujuan wajib diisi")
        @Size(max = 140, message = "Tujuan maksimal 140 karakter")
        String destination,

        @NotBlank(message = "Keperluan wajib diisi")
        @Size(max = 180, message = "Keperluan maksimal 180 karakter")
        String purpose,

        String photoDataUrl,

        @Size(max = 255, message = "Catatan maksimal 255 karakter")
        String notes,

        @NotNull(message = "Jam kunjungan wajib diisi")
        LocalDateTime visitTime) {
}
