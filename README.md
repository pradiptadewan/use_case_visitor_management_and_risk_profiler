# Vigi-Gate: Smart Visitor & Risk Profiler

Vigi-Gate adalah sistem registrasi tamu digital untuk kompleks perumahan atau perkantoran. Aplikasi ini mencatat data tamu, menghitung risk score otomatis, menampilkan tamu yang masih berada di area secara real-time, dan menghasilkan summary kunjungan harian dengan simulasi logic AI.

## Stack

- Backend: Spring Boot 3.5, Java 17, Spring Web, Spring Data JPA
- Frontend: Angular standalone
- Database: MySQL Laragon

## Fitur Utama

- Smart Registration
  - Input nama, NIK, tujuan, keperluan, jam kunjungan, catatan, dan foto mock/upload.
- Risk Scoring Engine
  - Skor dihitung dari jam kunjungan dan frekuensi kunjungan sebelumnya.
  - Status akhir: `GREEN`, `YELLOW`, `RED`.
- Real-time Log
  - Dashboard menampilkan total kunjungan hari ini, tamu aktif, distribusi risiko, dan daftar tamu yang sedang berada di area.
- AI Summary Report
  - Tombol `Generate Summary` memanggil backend untuk membuat ringkasan aktivitas, highlight, dan rekomendasi operasional.

## Aturan Risk Score

- Green
  - Jam kunjungan normal dan frekuensi datang masih rendah.
- Yellow
  - Datang mendekati jam rawan atau sudah beberapa kali berkunjung.
- Red
  - Datang larut malam atau terlalu sering muncul dalam 7 hari terakhir.

## Struktur Project

- `src/main/java/...`
  - API Spring Boot, entity visitor log, service risk scoring, summary report, demo seeder.
- `frontend/`
  - Dashboard Angular untuk registrasi tamu, active log, dan summary report.
- `AI_PROMPTS.txt`
  - Contoh prompt AI agent yang dipakai saat membangun fitur utama.

## Cara Menjalankan

### 1. Jalankan MySQL dari Laragon

- Pastikan service MySQL Laragon aktif pada port `3306`.
- Konfigurasi default backend:
  - Database: `vigigate_db`
  - Username: `root`
  - Password: kosong
- Database akan dibuat otomatis oleh JPA karena URL memakai `createDatabaseIfNotExist=true`.

Jika username/password MySQL berbeda, jalankan backend dengan environment variable:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="password-anda"
```

### 2. Jalankan Backend

Gunakan PowerShell dari folder project:

```powershell
$env:MAVEN_USER_HOME="d:\Tes-Berijalan\backend\.m2"
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

Backend akan aktif di:

- `http://localhost:8080`

Endpoint utama:

- `GET /api/dashboard/overview`
- `POST /api/visitors`
- `GET /api/visitors/active`
- `GET /api/visitors/today`
- `PATCH /api/visitors/{id}/checkout`
- `GET /api/reports/today-summary`

### 3. Jalankan Frontend Angular

Buka terminal baru:

```powershell
cd frontend
npm.cmd install
npm.cmd start
```

Frontend akan aktif di:

- `http://localhost:4200`

## Data Demo

Saat backend pertama kali dijalankan, aplikasi akan mengisi beberapa data awal otomatis jika tabel masih kosong.

Tujuannya:

- Dashboard langsung terisi untuk presentasi.
- Status Green, Yellow, dan Red lebih mudah didemokan.
- Tombol summary langsung menghasilkan insight yang relevan.

Jika ingin mematikan seed demo:

```powershell
$env:APP_SEED_DEMO_DATA="false"
./mvnw.cmd spring-boot:run
```

## Skenario Demo Saat Interview

1. Tunjukkan dashboard awal yang sudah berisi data seed.
2. Jelaskan rules risk scoring berdasarkan jam kunjungan dan frekuensi.
3. Tambahkan tamu baru pada jam normal untuk menghasilkan `Green`.
4. Tambahkan tamu yang sama pada jam malam atau beberapa kali untuk memunculkan `Yellow` atau `Red`.
5. Tunjukkan daftar tamu aktif di `Active Visitor Log`.
6. Klik `Checkout` untuk memperbarui log real-time.
7. Klik `Generate Summary` untuk menampilkan ringkasan harian berbasis simulasi AI.

## Verifikasi Yang Sudah Dilakukan

- Backend unit test: `PASS`
- Frontend Angular build: `PASS`
- Startup backend dan akses endpoint `GET /api/dashboard/overview`: `HTTP 200`

## Catatan

- File `.mvn/maven.config` sudah diarahkan ke local repository workspace supaya dependency Maven tidak menulis ke home directory.
- Jika port `8080` atau `4200` sedang dipakai aplikasi lain, ubah port sebelum demo.
