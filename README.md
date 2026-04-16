# Vigi-Gate

Sistem registrasi tamu digital dengan risk scoring, live log tamu aktif, dan summary kunjungan harian.

## Stack

- Backend: Spring Boot 3.5, Java 17
- Frontend: Angular
- Database: MySQL Laragon

## Fitur Utama

- Registrasi tamu: nama, NIK, tujuan, keperluan, waktu kunjungan, catatan, foto
- Risk scoring: `GREEN`, `YELLOW`, `RED`
- Live log: daftar tamu yang masih aktif di area
- Riwayat: kunjungan yang sudah checkout
- Summary harian: headline, highlights, recommendations, narrative

## Menjalankan Project

### 1. Jalankan MySQL

Gunakan MySQL dari Laragon dengan konfigurasi default:

- Host: `localhost`
- Port: `3306`
- Database: `vigigate_db`
- Username: `root`
- Password: kosong

Jika username/password berbeda:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="password-anda"
```

### 2. Jalankan Backend

```powershell
$env:MAVEN_USER_HOME="d:\Tes-Berijalan\backend\.m2"
./mvnw.cmd spring-boot:run
```

Backend: `http://localhost:8080`

### 3. Jalankan Frontend

```powershell
cd frontend
npm.cmd install
npm.cmd start
```

Frontend: `http://localhost:4200`

## Endpoint Utama

- `GET /api/dashboard/overview`
- `POST /api/visitors`
- `GET /api/visitors/today`
- `PATCH /api/visitors/{id}/checkout`
- `GET /api/reports/today-summary`

## Catatan

- Prompt AI agent yang digunakan ada di [AI_PROMPTS.txt](/d:/Tes-Berijalan/backend/AI_PROMPTS.txt:1).
