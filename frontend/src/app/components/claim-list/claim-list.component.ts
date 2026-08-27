import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClaimService } from '../../services/claim.service';
import { AuthService } from '../../services/auth.service';
import { Claim } from '../../models/claim.model';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-claim-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './claim-list.component.html',
  styleUrl: './claim-list.component.css'
})
export class ClaimListComponent implements OnInit {
  claims: Claim[] = [];
  filteredClaims: Claim[] = [];
  currentUser: User | null = null;
  isLoading = true;
  errorMessage = '';

  // View Mode: 'grid' or 'table'
  viewMode: 'grid' | 'table' = 'grid';

  // Filters
  searchTerm = '';
  statusFilter = '';
  priorityFilter = '';
  startDateFilter = '';
  endDateFilter = '';

  // 3D Card Tilt Transforms
  cardTransforms: { [key: number]: string } = {};

  constructor(private claimService: ClaimService, private authService: AuthService) {
    this.currentUser = this.authService.currentUserValue;
  }

  ngOnInit(): void {
    this.loadClaims();
  }

  toggleViewMode(mode: 'grid' | 'table'): void {
    this.viewMode = mode;
  }

  onCardMouseMove(event: MouseEvent, claimId?: number): void {
    if (!claimId) return;
    const card = event.currentTarget as HTMLElement;
    if (!card) return;

    const rect = card.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const rotateX = ((y - centerY) / centerY) * -8;
    const rotateY = ((x - centerX) / centerX) * 8;

    this.cardTransforms[claimId] = `perspective(1000px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale3d(1.02, 1.02, 1.02)`;
  }

  onCardMouseLeave(claimId?: number): void {
    if (!claimId) return;
    this.cardTransforms[claimId] = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
  }

  loadClaims(): void {
    this.claimService.getClaims().subscribe({
      next: (data) => {
        this.claims = data;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
        this.errorMessage = 'Impossible de charger la liste des réclamations.';
      }
    });
  }

  applyFilters(): void {
    let start = this.startDateFilter ? new Date(this.startDateFilter + 'T00:00:00') : null;
    let end = this.endDateFilter ? new Date(this.endDateFilter + 'T23:59:59') : null;

    this.filteredClaims = this.claims.filter(claim => {
      const matchesSearch = !this.searchTerm || 
        claim.titre.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        claim.description.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (claim.id && claim.id.toString().includes(this.searchTerm));

      const matchesStatus = !this.statusFilter || claim.statut === this.statusFilter;
      const matchesPriority = !this.priorityFilter || claim.priorite === this.priorityFilter;

      let matchesDate = true;
      if (claim.dateCreation) {
        const cDate = new Date(claim.dateCreation);
        if (start && cDate < start) matchesDate = false;
        if (end && cDate > end) matchesDate = false;
      }

      return matchesSearch && matchesStatus && matchesPriority && matchesDate;
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.statusFilter = '';
    this.priorityFilter = '';
    this.startDateFilter = '';
    this.endDateFilter = '';
    this.applyFilters();
  }

  exportPdf(): void {
    this.claimService.exportPdf().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'reclamations-export.pdf';
        anchor.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de télécharger le PDF des données.';
      }
    });
  }
}
