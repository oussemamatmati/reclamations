import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CommentService } from '../../services/comment.service';

@Component({
  selector: 'app-comment-crud',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './comment-crud.component.html',
  styleUrl: './comment-crud.component.css'
})
export class CommentCrudComponent implements OnInit {
  comments: any[] = [];
  filteredComments: any[] = [];
  isLoading = true;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';

  constructor(private commentService: CommentService) {}

  ngOnInit(): void {
    this.loadComments();
  }

  loadComments(): void {
    this.isLoading = true;
    this.commentService.getAllComments().subscribe({
      next: (data) => {
        this.comments = data;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger les commentaires.';
        this.isLoading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredComments = this.comments.filter(c => {
      if (!this.searchTerm) return true;
      const term = this.searchTerm.toLowerCase();
      return (
        (c.contenu && c.contenu.toLowerCase().includes(term)) ||
        (c.agentNom && c.agentNom.toLowerCase().includes(term)) ||
        (c.agentPrenom && c.agentPrenom.toLowerCase().includes(term)) ||
        (c.reclamationId && c.reclamationId.toString().includes(term))
      );
    });
  }

  deleteComment(id: number): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce commentaire ?')) return;

    this.commentService.deleteComment(id).subscribe({
      next: () => {
        this.successMessage = 'Commentaire supprimé avec succès.';
        this.loadComments();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de supprimer ce commentaire.';
      }
    });
  }

  getRoleBadgeClass(role?: string): string {
    if (role === 'ADMIN') return 'badge-role-admin';
    if (role === 'AGENT') return 'badge-role-agent';
    return 'badge-role-client';
  }
}
