import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin, interval, startWith, switchMap } from 'rxjs';

import {
  ApiService,
  DashboardOverviewResponse,
  RegisterVisitorPayload,
  RiskLevel,
  SummaryReportResponse,
  VisitorLogResponse,
  VisitStatus
} from './api.service';

type ControlName =
  | 'visitorName'
  | 'nik'
  | 'destination'
  | 'purpose'
  | 'visitTime'
  | 'notes';

type AppSection = 'dashboard' | 'register' | 'history' | 'reports' | 'policy';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent {
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dateTimeFormatter = new Intl.DateTimeFormat('id-ID', {
    dateStyle: 'medium',
    timeStyle: 'short'
  });
  private readonly dayFormatter = new Intl.DateTimeFormat('id-ID', {
    weekday: 'long'
  });
  private readonly dateFormatter = new Intl.DateTimeFormat('id-ID', {
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  });
  private readonly longDateFormatter = new Intl.DateTimeFormat('id-ID', {
    dateStyle: 'full'
  });
  private readonly timeFormatter = new Intl.DateTimeFormat('id-ID', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });

  readonly overview = signal<DashboardOverviewResponse | null>(null);
  readonly summary = signal<SummaryReportResponse | null>(null);
  readonly todayVisits = signal<VisitorLogResponse[]>([]);
  readonly activeSection = signal<AppSection>('dashboard');
  readonly loadingOverview = signal(true);
  readonly registering = signal(false);
  readonly generatingSummary = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly photoPreview = signal('');
  readonly now = signal(new Date());
  readonly defaultPhotoPlaceholder = this.createDefaultPhotoPlaceholder();

  readonly navigationItems = [
    { id: 'dashboard' as AppSection, label: 'Dashboard', caption: 'Ringkasan hari ini' },
    { id: 'register' as AppSection, label: 'Register', caption: 'Input tamu baru' },
    { id: 'history' as AppSection, label: 'Riwayat', caption: 'Log kunjungan' },
    { id: 'reports' as AppSection, label: 'Laporan', caption: 'Summary harian' },
    { id: 'policy' as AppSection, label: 'Screening', caption: 'Aturan risiko' }
  ];

  readonly generatedSummaryLabel = computed(() => {
    const report = this.summary();
    return report ? this.formatDate(report.generatedAt) : 'Belum tersedia';
  });
  readonly checkedOutVisits = computed(() =>
    this.todayVisits().filter((visit) => visit.visitStatus === 'CHECKED_OUT')
  );
  readonly currentDateLabel = computed(() => this.longDateFormatter.format(this.now()));
  readonly currentDayLabel = computed(() => this.dayFormatter.format(this.now()));
  readonly currentCalendarLabel = computed(() => this.dateFormatter.format(this.now()));
  readonly currentTimeLabel = computed(() => `${this.timeFormatter.format(this.now())} WIB`);

  readonly sectionHeading = computed(() => {
    switch (this.activeSection()) {
      case 'register':
        return {
          title: 'Register',
          description: 'Daftarkan tamu baru dengan cepat.'
        };
      case 'history':
        return {
          title: 'Riwayat',
          description: 'Lihat kunjungan yang sudah checkout.'
        };
      case 'reports':
        return {
          title: 'Laporan',
          description: 'Buat ringkasan harian untuk supervisor.'
        };
      case 'policy':
        return {
          title: 'Screening',
          description: 'Pahami indikator waktu, frekuensi, dan batas skornya.'
        };
      default:
        return {
          title: 'Dashboard',
          description: 'Lihat kondisi kunjungan hari ini.'
        };
    }
  });

  readonly servicePosture = computed(() => {
    const overview = this.overview();
    if (!overview) {
      return {
        title: 'Menunggu sinkronisasi',
        detail: 'Data akan tampil setelah backend aktif.',
        tone: 'neutral'
      };
    }

    if (overview.riskDistribution.red > 0) {
      return {
        title: 'Perlu verifikasi manual',
        detail: 'Ada tamu berprofil merah.',
        tone: 'red'
      };
    }

    if (overview.riskDistribution.yellow > 0) {
      return {
        title: 'Monitoring tambahan',
        detail: 'Beberapa kunjungan butuh konfirmasi.',
        tone: 'yellow'
      };
    }

    return {
      title: 'Operasional terkendali',
      detail: 'Mayoritas tamu berada pada profil aman.',
      tone: 'green'
    };
  });

  readonly executiveSnapshot = computed(() => {
    const overview = this.overview();
    if (!overview || overview.todayTotal === 0) {
      return 'Belum ada aktivitas hari ini.';
    }

    if (overview.activeVisitors === 0) {
      return 'Semua kunjungan hari ini sudah selesai.';
    }

    return `${overview.activeVisitors} tamu masih berada di area.`;
  });

  readonly summaryHint = computed(() => {
    const overview = this.overview();
    if (!overview || overview.todayTotal === 0) {
      return 'Ringkasan akan tersedia setelah ada kunjungan.';
    }
    if (overview.riskDistribution.red > 0) {
      return 'Disarankan dibuat karena ada profil merah.';
    }
    return 'Gunakan untuk briefing singkat.';
  });

  readonly riskLegend = [
    {
      label: 'Green',
      rule: 'Skor akhir di bawah 40 dan kunjungan masih dalam pola normal.'
    },
    {
      label: 'Yellow',
      rule: 'Skor akhir 40 sampai 69 dan perlu verifikasi tambahan.'
    },
    {
      label: 'Red',
      rule: 'Skor akhir 70 ke atas dan diprioritaskan untuk screening manual.'
    }
  ];

  readonly riskDetailCards = [
    {
      label: 'Green',
      summary: 'Status aman saat total skor masih di bawah ambang verifikasi tambahan.',
      score: 'Skor akhir < 40',
      points: [
        'Jam umum 07.00 sampai 19.59 memberi faktor waktu paling rendah.',
        'Belum ada pola kunjungan berulang yang signifikan dalam 7 hari terakhir.',
        'Belum ada kunjungan lain pada hari yang sama atau total skornya tetap rendah.'
      ],
      action: 'Proses dengan alur registrasi standar.'
    },
    {
      label: 'Yellow',
      summary: 'Status waspada untuk kunjungan yang mulai keluar dari pola operasional normal.',
      score: 'Skor akhir 40 sampai 69',
      points: [
        'Datang pada 05.00 sampai 06.59 sehingga masuk sebelum jam kerja normal.',
        'Datang pada 20.00 sampai 22.59 sehingga masuk luar jam operasional utama.',
        'Frekuensi 7 hari terakhir sudah 3 sampai 4 kali sehingga perlu dicek ulang.',
        'Sudah ada 1 kunjungan lain di hari yang sama sehingga butuh konfirmasi tambahan.'
      ],
      action: 'Verifikasi identitas, tujuan, dan PIC sebelum akses diberikan.'
    },
    {
      label: 'Red',
      summary: 'Status prioritas screening untuk pola kunjungan yang tergolong tinggi atau sangat sensitif.',
      score: 'Skor akhir >= 70',
      points: [
        'Datang pada 23.00 sampai 04.59 sehingga masuk jam rawan larut malam.',
        'Frekuensi 7 hari terakhir mencapai 5 kali atau lebih.',
        'Sudah ada 2 kunjungan lain atau lebih pada hari yang sama.',
        'Kombinasi beberapa indikator membuat total skor menembus ambang merah.'
      ],
      action: 'Lakukan screening manual dan minta approval petugas berwenang.'
    }
  ];

  readonly riskScoreComponents = [
    {
      title: 'Skor dasar',
      points: ['Setiap registrasi dimulai dari 10 poin sebelum faktor lain ditambahkan.']
    },
    {
      title: 'Faktor waktu',
      points: [
        '23.00 sampai 04.59: +60 poin',
        '20.00 sampai 22.59: +35 poin',
        '05.00 sampai 06.59: +20 poin',
        '07.00 sampai 19.59: -5 poin'
      ]
    },
    {
      title: 'Frekuensi 7 hari terakhir',
      points: [
        '5 kali atau lebih: +30 poin',
        '3 sampai 4 kali: +20 poin',
        '1 sampai 2 kali: +8 poin'
      ]
    },
    {
      title: 'Kunjungan pada hari yang sama',
      points: [
        '2 kunjungan lain atau lebih: +15 poin',
        '1 kunjungan lain: +8 poin'
      ]
    },
    {
      title: 'Ambang status',
      points: [
        'Green: skor di bawah 40',
        'Yellow: skor 40 sampai 69',
        'Red: skor 70 atau lebih'
      ]
    }
  ];

  readonly registrationForm = this.formBuilder.nonNullable.group({
    visitorName: ['', [Validators.required, Validators.maxLength(120)]],
    nik: ['', [Validators.required, Validators.pattern(/^\d{16}$/)]],
    destination: ['', [Validators.required, Validators.maxLength(140)]],
    purpose: ['', [Validators.required, Validators.maxLength(180)]],
    visitTime: [this.currentDateTimeLocal(), Validators.required],
    notes: ['', [Validators.maxLength(255)]],
    photoDataUrl: ['']
  });

  constructor() {
    interval(1000)
      .pipe(startWith(0), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.now.set(new Date()));

    interval(12000)
      .pipe(
        startWith(0),
        switchMap(() =>
          forkJoin({
            overview: this.api.getOverview(),
            todayVisits: this.api.getTodayVisitors()
          })
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ({ overview, todayVisits }) => {
          this.loadingOverview.set(false);
          this.overview.set(overview);
          this.todayVisits.set(todayVisits);
        },
        error: (error: HttpErrorResponse) => {
          this.loadingOverview.set(false);
          this.errorMessage.set(this.extractError(error) || 'Backend belum terhubung.');
        }
      });
  }

  openSection(section: AppSection): void {
    this.activeSection.set(section);
  }

  isSection(section: AppSection): boolean {
    return this.activeSection() === section;
  }

  submitRegistration(): void {
    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    this.registering.set(true);
    this.errorMessage.set('');

    const rawValue = this.registrationForm.getRawValue();
    const payload: RegisterVisitorPayload = {
      visitorName: rawValue.visitorName.trim(),
      nik: rawValue.nik.trim(),
      destination: rawValue.destination.trim(),
      purpose: rawValue.purpose.trim(),
      visitTime: rawValue.visitTime,
      notes: rawValue.notes.trim(),
      photoDataUrl: rawValue.photoDataUrl
    };

    this.api
      .registerVisitor(payload)
      .pipe(finalize(() => this.registering.set(false)))
      .subscribe({
        next: (visitor) => {
          this.summary.set(null);
          this.setSuccess(`${visitor.visitorName} berhasil didaftarkan dengan status ${this.levelLabel(visitor.riskLevel)}.`);
          this.resetForm();
          this.activeSection.set('dashboard');
          this.refreshOverview();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.extractError(error));
        }
      });
  }

  refreshOverview(): void {
    this.loadingOverview.set(true);
    forkJoin({
      overview: this.api.getOverview(),
      todayVisits: this.api.getTodayVisitors()
    }).subscribe({
      next: ({ overview, todayVisits }) => {
        this.loadingOverview.set(false);
        this.overview.set(overview);
        this.todayVisits.set(todayVisits);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingOverview.set(false);
        this.errorMessage.set(this.extractError(error));
      }
    });
  }

  checkoutVisitor(visitorId: number): void {
    this.api.checkoutVisitor(visitorId).subscribe({
      next: (visitor) => {
        this.setSuccess(`Kunjungan ${visitor.visitorName} telah ditutup.`);
        this.refreshOverview();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.extractError(error));
      }
    });
  }

  generateSummary(): void {
    this.generatingSummary.set(true);
    this.errorMessage.set('');

    this.api
      .getTodaySummary()
      .pipe(finalize(() => this.generatingSummary.set(false)))
      .subscribe({
        next: (summary) => {
          this.summary.set(summary);
          this.activeSection.set('reports');
          this.setSuccess('Ringkasan harian berhasil dibuat.');
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.extractError(error));
        }
      });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result?.toString() ?? '';
      this.photoPreview.set(result);
      this.registrationForm.patchValue({ photoDataUrl: result });
    };
    reader.readAsDataURL(file);
  }

  useMockPhoto(): void {
    const preview = this.createMockPhoto(this.registrationForm.controls.visitorName.value || 'Vigi Gate');
    this.photoPreview.set(preview);
    this.registrationForm.patchValue({ photoDataUrl: preview });
  }

  clearPhoto(): void {
    this.photoPreview.set('');
    this.registrationForm.patchValue({ photoDataUrl: '' });
  }

  invalid(controlName: ControlName): boolean {
    const control = this.registrationForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  formatDate(value: string): string {
    return this.dateTimeFormatter.format(new Date(value));
  }

  formatDuration(totalMinutes: number): string {
    if (totalMinutes < 60) {
      return `${totalMinutes} menit`;
    }

    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    return minutes === 0 ? `${hours} jam` : `${hours} jam ${minutes} menit`;
  }

  levelLabel(level: RiskLevel): string {
    switch (level) {
      case 'GREEN':
        return 'Green';
      case 'YELLOW':
        return 'Yellow';
      default:
        return 'Red';
    }
  }

  levelClass(level: RiskLevel): string {
    return level.toLowerCase();
  }

  visitStatusLabel(status: VisitStatus): string {
    return status === 'ACTIVE' ? 'Masih di area' : 'Selesai';
  }

  visitStatusClass(status: VisitStatus): string {
    return status === 'ACTIVE' ? 'active' : 'checked-out';
  }

  distributionWidth(value: number, total: number): string {
    if (total === 0 || value === 0) {
      return '0%';
    }
    return `${Math.max(12, Math.round((value / total) * 100))}%`;
  }

  maskNik(nik: string): string {
    if (nik.length < 8) {
      return nik;
    }
    return `${nik.slice(0, 4)} **** **** ${nik.slice(-4)}`;
  }

  initialsFor(name: string): string {
    const words = name.trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) {
      return 'VG';
    }
    if (words.length === 1) {
      return words[0].slice(0, 2).toUpperCase();
    }
    return `${words[0][0]}${words[1][0]}`.toUpperCase();
  }

  private setSuccess(message: string): void {
    this.successMessage.set(message);
    window.setTimeout(() => {
      if (this.successMessage() === message) {
        this.successMessage.set('');
      }
    }, 3500);
  }

  private resetForm(): void {
    this.registrationForm.reset({
      visitorName: '',
      nik: '',
      destination: '',
      purpose: '',
      visitTime: this.currentDateTimeLocal(),
      notes: '',
      photoDataUrl: ''
    });
    this.photoPreview.set('');
  }

  private currentDateTimeLocal(): string {
    const now = new Date();
    const localTime = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
    return localTime.toISOString().slice(0, 16);
  }

  private createMockPhoto(name: string): string {
    const initials = this.initialsFor(name || 'Vigi Gate');
    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg" width="180" height="180" viewBox="0 0 180 180">
        <defs>
          <linearGradient id="a" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#163f48" />
            <stop offset="100%" stop-color="#0f6c69" />
          </linearGradient>
        </defs>
        <rect width="180" height="180" rx="42" fill="url(#a)"/>
        <circle cx="138" cy="42" r="24" fill="#f0c987" opacity="0.92"/>
        <text x="50%" y="56%" dominant-baseline="middle" text-anchor="middle"
          font-size="56" font-family="Trebuchet MS, Segoe UI Variable, sans-serif"
          font-weight="700" fill="#fdf8ef">${initials}</text>
      </svg>
    `.trim();

    return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
  }

  private createDefaultPhotoPlaceholder(): string {
    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" viewBox="0 0 240 240">
        <rect width="240" height="240" rx="34" fill="#ffffff"/>
        <rect x="36" y="44" width="156" height="112" rx="18" fill="none" stroke="#111111" stroke-width="14"/>
        <circle cx="86" cy="88" r="14" fill="#111111"/>
        <path d="M48 156l40-34c8-7 18-7 26 0l16 14 34-30c10-9 21-9 31 0l32 30v20H48z" fill="#111111"/>
        <circle cx="190" cy="164" r="44" fill="#111111"/>
        <path d="M190 142v44M168 164h44" stroke="#ffffff" stroke-width="14" stroke-linecap="round"/>
      </svg>
    `.trim();

    return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
  }

  private extractError(error: HttpErrorResponse): string {
    const validationErrors = error.error?.errors as Record<string, string> | undefined;
    if (validationErrors && Object.keys(validationErrors).length > 0) {
      return Object.values(validationErrors).join(' | ');
    }
    return error.error?.message || 'Terjadi kendala saat menghubungi server.';
  }
}
