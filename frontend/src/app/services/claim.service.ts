import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Claim, Categorie, StatusHistory, DashboardStats } from '../models/claim.model';
import { ChatbotRequest, ChatbotResponse } from '../models/chatbot.model';

@Injectable({
  providedIn: 'root'
})
export class ClaimService {
  private apiUrl = 'http://localhost:8081/api/reclamations';
  private categoriesUrl = 'http://localhost:8081/api/categories';
  private dashboardUrl = 'http://localhost:8081/api/dashboard';

  constructor(private http: HttpClient) {}

  getClaims(): Observable<Claim[]> {
    return this.http.get<Claim[]>(this.apiUrl);
  }

  getClaimById(id: number): Observable<Claim> {
    return this.http.get<Claim>(`${this.apiUrl}/${id}`);
  }

  createClaim(claim: Partial<Claim>): Observable<Claim> {
    return this.http.post<Claim>(this.apiUrl, claim);
  }

  assignAgent(claimId: number, agentId?: number): Observable<Claim> {
    let params = new HttpParams();
    if (agentId) {
      params = params.set('agentId', agentId.toString());
    }
    return this.http.put<Claim>(`${this.apiUrl}/${claimId}/assigner`, {}, { params });
  }

  updateStatus(claimId: number, statut: 'NOUVELLE' | 'EN_COURS' | 'RESOLUE' | 'REJETEE'): Observable<Claim> {
    const params = new HttpParams().set('statut', statut);
    return this.http.put<Claim>(`${this.apiUrl}/${claimId}/statut`, {}, { params });
  }

  getStatusHistory(claimId: number): Observable<StatusHistory[]> {
    return this.http.get<StatusHistory[]>(`${this.apiUrl}/${claimId}/historique`);
  }

  getCategories(): Observable<Categorie[]> {
    return this.http.get<Categorie[]>(this.categoriesUrl);
  }

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.dashboardUrl}/stats`);
  }

  askChatbot(question: string): Observable<ChatbotResponse> {
    const payload: ChatbotRequest = { question };
    return this.http.post<ChatbotResponse>(`${this.dashboardUrl}/chatbot`, payload);
  }

  exportPdf(): Observable<Blob> {
    return this.http.get(`${this.dashboardUrl}/export/pdf`, { responseType: 'blob' });
  }
}
