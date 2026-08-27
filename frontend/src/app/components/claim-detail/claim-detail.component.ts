import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClaimService } from '../../services/claim.service';
import { CommentService } from '../../services/comment.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { Claim, StatusHistory } from '../../models/claim.model';
import { User } from '../../models/user.model';

interface Commentaire {
  id?: number;
  contenu: string;
  dateCommentaire?: string;
  reclamationId?: number;
  agentId?: number;
  agentNom?: string;
  agentPrenom?: string;
  agentRole?: string;
}

@Component({
  selector: 'app-claim-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './claim-detail.component.html',
  styleUrl: './claim-detail.component.css'
})
export class ClaimDetailComponent implements OnInit {
  claim: Claim | null = null;
  history: StatusHistory[] = [];
  comments: Commentaire[] = [];
  agents: User[] = [];

  newComment = '';
  selectedStatus: 'NOUVELLE' | 'EN_COURS' | 'RESOLUE' | 'REJETEE' = 'NOUVELLE';
  selectedAgentId?: number;

  isLoadingClaim = true;
  isSubmittingComment = false;
  isUpdatingStatus = false;
  isAssigning = false;
  errorMessage = '';
  successMessage = '';

  userRole = '';
  userId: number | undefined = undefined;

  // Satisfaction rating (stored in localStorage)
  satisfactionRating = 0;
  hoverRating = 0;
  feedbackText = '';
  isRated = false;
  ratingSubmitted = false;

  // Quick message templates
  quickSnippets = [
    'Bonjour, voici des précisions complémentaires.',
    'Dossier mis à jour avec succès.',
    'Merci pour votre retour rapide.'
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private claimService: ClaimService,
    private commentService: CommentService,
    private userService: UserService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser) {
      this.userRole = currentUser.role;
      this.userId = currentUser.id ?? undefined;
    }

    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/reclamations']);
      return;
    }

    this.loadClaim(id);
    this.loadHistory(id);
    this.loadComments(id);

    if (this.isAdminOrAgent()) {
      this.loadAgents();
    }

    // Load saved rating from localStorage
    const saved = localStorage.getItem(`bct_rating_${id}`);
    if (saved) {
      const parsed = JSON.parse(saved);
      this.satisfactionRating = parsed.rating;
      this.feedbackText = parsed.feedback || '';
      this.isRated = true;
      this.ratingSubmitted = true;
    }
  }

  isAdmin(): boolean { return this.userRole === 'ADMIN'; }
  isAgent(): boolean { return this.userRole === 'AGENT'; }
  isClient(): boolean { return this.userRole === 'CLIENT'; }
  isAdminOrAgent(): boolean { return this.isAdmin() || this.isAgent(); }

  isMine(comment: Commentaire): boolean {
    if (comment.agentId !== undefined && this.userId !== undefined) {
      return comment.agentId === this.userId;
    }
    return false;
  }

  isStaff(comment: Commentaire): boolean {
    return comment.agentRole === 'AGENT' || comment.agentRole === 'ADMIN';
  }

  // ─── Rating ──────────────────────────────────────
  starArray(): number[] { return [1, 2, 3, 4, 5]; }

  setHoverRating(star: number): void { this.hoverRating = star; }
  clearHoverRating(): void { this.hoverRating = 0; }
  setRating(star: number): void { if (!this.ratingSubmitted) this.satisfactionRating = star; }

  starClass(star: number): string {
    const active = this.hoverRating > 0 ? this.hoverRating : this.satisfactionRating;
    return star <= active ? 'star-active' : 'star-inactive';
  }

  getRatingLabel(): string {
    const labels: { [k: number]: string } = {
      1: 'Très insatisfait',
      2: 'Insatisfait',
      3: 'Neutre',
      4: 'Satisfait',
      5: 'Très satisfait !'
    };
    return labels[this.satisfactionRating] || 'Sélectionnez une note';
  }

  submitRating(): void {
    if (!this.satisfactionRating || !this.claim?.id) return;
    localStorage.setItem(`bct_rating_${this.claim.id}`, JSON.stringify({
      rating: this.satisfactionRating,
      feedback: this.feedbackText
    }));
    this.isRated = true;
    this.ratingSubmitted = true;
  }

  // ─── Attestation PDF ────────────────────────────
  downloadAttestation(): void {
    if (!this.claim) return;
    const c = this.claim;
    const today = new Date().toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });

    const printWindow = window.open('', '_blank', 'width=800,height=900');
    if (!printWindow) return;

    printWindow.document.write(`
      <!DOCTYPE html>
      <html lang="fr">
      <head>
        <meta charset="UTF-8">
        <title>Attestation de Dépôt BCT — Dossier #${c.id}</title>
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          body { font-family: Arial, sans-serif; padding: 40px; color: #1e293b; background: #fff; }
          .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #1e3a8a; padding-bottom: 20px; margin-bottom: 30px; }
          .header-left h1 { font-size: 22px; color: #1e3a8a; font-weight: 900; }
          .header-left p { font-size: 12px; color: #64748b; margin-top: 4px; }
          .header-right { text-align: right; }
          .header-right .ref { font-size: 26px; font-weight: 900; color: #1e3a8a; }
          .header-right .ref-label { font-size: 11px; color: #64748b; text-transform: uppercase; letter-spacing: 1px; }
          .stamp-band { background: linear-gradient(90deg, #1e3a8a, #2563eb); color: #fff; padding: 10px 20px; border-radius: 6px; font-size: 13px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; margin-bottom: 30px; text-align: center; }
          h2 { font-size: 18px; color: #1e3a8a; margin-bottom: 20px; border-left: 4px solid #06b6d4; padding-left: 12px; }
          table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
          th { background: #f1f5f9; padding: 10px 14px; text-align: left; font-size: 12px; color: #475569; text-transform: uppercase; letter-spacing: 0.5px; }
          td { padding: 12px 14px; border-bottom: 1px solid #e2e8f0; font-size: 14px; }
          td:first-child { font-weight: 700; color: #0f172a; width: 220px; }
          .status-badge { display: inline-block; padding: 4px 14px; border-radius: 999px; font-size: 12px; font-weight: 700; }
          .NOUVELLE { background: #dbeafe; color: #1d4ed8; }
          .EN_COURS { background: #fef3c7; color: #d97706; }
          .RESOLUE { background: #d1fae5; color: #065f46; }
          .REJETEE { background: #fee2e2; color: #991b1b; }
          .footer { margin-top: 40px; border-top: 1px solid #e2e8f0; padding-top: 20px; font-size: 11px; color: #94a3b8; display: flex; justify-content: space-between; }
          .footer .seal { text-align: right; }
          .notice { background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 8px; padding: 14px 18px; font-size: 12px; color: #0369a1; margin-bottom: 20px; }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="header-left">
            <h1>🏦 Banque Centrale de Tunisie</h1>
            <p>Direction du Traitement des Réclamations Bancaires</p>
          </div>
          <div class="header-right">
            <div class="ref-label">Référence Dossier</div>
            <div class="ref">BCT-${String(c.id).padStart(6, '0')}</div>
          </div>
        </div>

        <div class="stamp-band">✓ Attestation Officielle de Dépôt de Réclamation</div>

        <div class="notice">
          Ce document atteste que la réclamation ci-dessous a été officiellement enregistrée dans le Système de Gestion des Réclamations de la Banque Centrale de Tunisie (BCT). Il peut être utilisé comme preuve de dépôt auprès de tout établissement bancaire.
        </div>

        <h2>Informations du Dossier</h2>
        <table>
          <thead><tr><th>Champ</th><th>Valeur</th></tr></thead>
          <tbody>
            <tr><td>N° de Dossier</td><td><strong>BCT-${String(c.id).padStart(6, '0')}</strong></td></tr>
            <tr><td>Objet de la Réclamation</td><td>${c.titre}</td></tr>
            <tr><td>Catégorie</td><td>${c.categorieNom || 'Non spécifiée'}</td></tr>
            <tr><td>Niveau de Priorité</td><td>${c.priorite}</td></tr>
            <tr><td>Statut Actuel</td><td><span class="status-badge ${c.statut}">${c.statut.replace('_', ' ')}</span></td></tr>
            <tr><td>Date de Soumission</td><td>${new Date(c.dateCreation || '').toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}</td></tr>
            <tr><td>Agent Assigné</td><td>${c.agentPrenom ? c.agentPrenom + ' ' + c.agentNom : 'En attente d\'assignation'}</td></tr>
          </tbody>
        </table>

        <div class="footer">
          <div>
            <strong>Généré le :</strong> ${today}<br>
            <span>Système de Réclamations BCT v2.0</span>
          </div>
          <div class="seal">
            <strong>Cachet Électronique BCT</strong><br>
            <span>Document certifié — www.bct.gov.tn</span>
          </div>
        </div>
      </body>
      </html>
    `);

    printWindow.document.close();
    printWindow.focus();
    setTimeout(() => {
      printWindow.print();
      printWindow.close();
    }, 500);
  }

  // ─── Snippets ────────────────────────────────────
  insertSnippet(text: string): void {
    this.newComment = this.newComment ? this.newComment + ' ' + text : text;
  }

  onCommentKeydown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      this.submitComment();
    }
  }


  setStatusDirectly(status: 'NOUVELLE' | 'EN_COURS' | 'RESOLUE' | 'REJETEE'): void {
    this.selectedStatus = status;
    this.updateStatus();
  }

  // ─── Data Loading ─────────────────────────────────
  loadClaim(id: number): void {
    this.isLoadingClaim = true;
    this.claimService.getClaimById(id).subscribe({
      next: (data) => {
        this.claim = data;
        this.selectedStatus = data.statut;
        this.selectedAgentId = data.agentId;
        this.isLoadingClaim = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger le dossier.';
        this.isLoadingClaim = false;
      }
    });
  }

  getWorkflowStepState(step: number): 'completed' | 'current' | 'pending' | 'rejected' {
    if (!this.claim) return 'pending';
    const statut = this.claim.statut;

    if (statut === 'REJETEE') {
      if (step === 1) return 'completed';
      if (step === 4) return 'rejected';
      return 'completed';
    }
    if (statut === 'NOUVELLE') {
      if (step === 1) return 'current';
      return 'pending';
    }
    if (statut === 'EN_COURS') {
      if (step <= 2) return 'completed';
      if (step === 3) return 'current';
      return 'pending';
    }
    if (statut === 'RESOLUE') return 'completed';
    return 'pending';
  }

  loadHistory(id: number): void {
    this.claimService.getStatusHistory(id).subscribe({
      next: (data) => this.history = data,
      error: () => console.error('Impossible de charger l\'historique.')
    });
  }

  loadComments(id: number): void {
    this.commentService.getComments(id).subscribe({
      next: (data) => {
        this.comments = data;
        this.scrollToBottom();
      },
      error: () => console.error('Impossible de charger les commentaires.')
    });
  }

  loadAgents(): void {
    this.userService.getAgents().subscribe({
      next: (data) => this.agents = data,
      error: () => console.error('Impossible de charger les agents.')
    });
  }

  updateStatus(): void {
    if (!this.claim?.id) return;
    this.isUpdatingStatus = true;
    this.claimService.updateStatus(this.claim.id, this.selectedStatus).subscribe({
      next: (updated) => {
        this.claim = updated;
        this.selectedStatus = updated.statut;
        this.isUpdatingStatus = false;
        this.successMessage = 'Statut mis à jour avec succès.';
        this.loadHistory(updated.id!);
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.isUpdatingStatus = false;
        this.errorMessage = 'Erreur lors de la mise à jour du statut.';
      }
    });
  }

  assignAgent(): void {
    if (!this.claim?.id) return;
    this.isAssigning = true;
    this.claimService.assignAgent(this.claim.id, this.selectedAgentId).subscribe({
      next: (updated) => {
        this.claim = updated;
        this.isAssigning = false;
        this.successMessage = 'Agent assigné avec succès.';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.isAssigning = false;
        this.errorMessage = 'Erreur lors de l\'assignation de l\'agent.';
      }
    });
  }

  submitComment(): void {
    if (!this.newComment.trim() || !this.claim?.id) return;
    this.isSubmittingComment = true;
    this.commentService.addComment(this.claim.id, this.newComment.trim()).subscribe({
      next: (comment) => {
        this.comments.push(comment);
        this.newComment = '';
        this.isSubmittingComment = false;
        this.scrollToBottom();
      },
      error: () => {
        this.isSubmittingComment = false;
        this.errorMessage = 'Erreur lors de l\'envoi du commentaire.';
      }
    });
  }

  scrollToBottom(): void {
    setTimeout(() => {
      const thread = document.querySelector('.chat-thread');
      if (thread) {
        thread.scrollTop = thread.scrollHeight;
      }
    }, 100);
  }

  getStatusClass(statut: string): string {
    const map: { [k: string]: string } = {
      'NOUVELLE': 'badge-nouvelle',
      'EN_COURS': 'badge-encours',
      'RESOLUE': 'badge-resolue',
      'REJETEE': 'badge-rejetee'
    };
    return map[statut] || '';
  }

  getPriorityClass(priorite: string): string {
    const map: { [k: string]: string } = {
      'FAIBLE': 'badge-faible',
      'MOYENNE': 'badge-moyenne',
      'ELEVEE': 'badge-elevee',
      'CRITIQUE': 'badge-critique'
    };
    return map[priorite] || '';
  }

  goBack(): void {
    this.router.navigate(['/reclamations']);
  }
}
