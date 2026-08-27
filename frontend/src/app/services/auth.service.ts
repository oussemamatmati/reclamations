import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, map } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/auth';
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser$: Observable<User | null>;

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<User | null>(storedUser ? JSON.parse(storedUser) : null);
    this.currentUser$ = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  public get isAuthenticated(): boolean {
    return this.currentUserValue !== null;
  }

  public get userRole(): string | null {
    return this.currentUserValue ? this.currentUserValue.role : null;
  }

  login(email: string, motDePasse: string): Observable<User> {
    return this.http.post<any>(`${this.apiUrl}/login`, { email, motDePasse }).pipe(
      map(response => {
        // Map backend JwtResponse to our User model
        const role = response.roles[0].replace('ROLE_', ''); // e.g. "ROLE_ADMIN" -> "ADMIN"
        const user: User = {
          id: response.id,
          email: response.email,
          nom: response.nom,
          prenom: response.prenom,
          role: role,
          token: response.token
        };
        localStorage.setItem('currentUser', JSON.stringify(user));
        this.currentUserSubject.next(user);
        return user;
      })
    );
  }

  register(nom: string, prenom: string, email: string, motDePasse: string, telephone?: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/signup`, {
      nom,
      prenom,
      email,
      motDePasse,
      telephone,
      role: 'CLIENT' // Defaults to CLIENT for public signups
    }, { responseType: 'text' });
  }

  verifyEmail(code: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/verify-email?code=${encodeURIComponent(code)}`, {}, { responseType: 'text' });
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }
}
