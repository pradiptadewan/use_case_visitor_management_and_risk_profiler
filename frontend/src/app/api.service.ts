import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type RiskLevel = 'GREEN' | 'YELLOW' | 'RED';
export type VisitStatus = 'ACTIVE' | 'CHECKED_OUT';

export interface RegisterVisitorPayload {
  visitorName: string;
  nik: string;
  destination: string;
  purpose: string;
  visitTime: string;
  notes: string;
  photoDataUrl: string;
}

export interface VisitorLogResponse {
  id: number;
  visitorName: string;
  nik: string;
  destination: string;
  purpose: string;
  photoDataUrl: string;
  notes: string | null;
  visitTime: string;
  checkoutTime: string | null;
  riskScore: number;
  riskLevel: RiskLevel;
  riskSummary: string;
  riskFactors: string[];
  visitStatus: VisitStatus;
  durationMinutes: number;
}

export interface DashboardOverviewResponse {
  todayTotal: number;
  activeVisitors: number;
  checkedOutVisitors: number;
  averageRiskScore: number;
  busiestWindow: string;
  riskDistribution: {
    green: number;
    yellow: number;
    red: number;
  };
  activeLogs: VisitorLogResponse[];
  recentVisits: VisitorLogResponse[];
}

export interface SummaryReportResponse {
  generatedAt: string;
  headline: string;
  highlights: string[];
  recommendations: string[];
  narrative: string;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api';

  getOverview(): Observable<DashboardOverviewResponse> {
    return this.http.get<DashboardOverviewResponse>(`${this.baseUrl}/dashboard/overview`);
  }

  getTodayVisitors(): Observable<VisitorLogResponse[]> {
    return this.http.get<VisitorLogResponse[]>(`${this.baseUrl}/visitors/today`);
  }

  registerVisitor(payload: RegisterVisitorPayload): Observable<VisitorLogResponse> {
    return this.http.post<VisitorLogResponse>(`${this.baseUrl}/visitors`, payload);
  }

  checkoutVisitor(visitorId: number): Observable<VisitorLogResponse> {
    return this.http.patch<VisitorLogResponse>(`${this.baseUrl}/visitors/${visitorId}/checkout`, {});
  }

  getTodaySummary(): Observable<SummaryReportResponse> {
    return this.http.get<SummaryReportResponse>(`${this.baseUrl}/reports/today-summary`);
  }
}
