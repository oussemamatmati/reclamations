import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Comment } from '../models/comment.model';

@Injectable({
  providedIn: 'root'
})
export class CommentService {
  private apiUrl = 'http://localhost:8081/api/commentaires';

  constructor(private http: HttpClient) {}

  getComments(claimId: number): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.apiUrl}/reclamation/${claimId}`);
  }

  addComment(claimId: number, contenu: string): Observable<Comment> {
    return this.http.post<Comment>(this.apiUrl, {
      reclamationId: claimId,
      contenu: contenu
    });
  }

  getAllComments(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  deleteComment(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
