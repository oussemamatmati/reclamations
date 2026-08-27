import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ClaimService } from '../../services/claim.service';
import { Categorie, Claim } from '../../models/claim.model';

@Component({
  selector: 'app-claim-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './claim-create.component.html',
  styleUrl: './claim-create.component.css'
})
export class ClaimCreateComponent implements OnInit {
  titre = '';
  description = '';;
  priorite: 'FAIBLE' | 'MOYENNE' | 'ELEVEE' | 'CRITIQUE' = 'MOYENNE';
  categorieId?: number;

  categories: Categorie[] = [];
  errorMessage = '';
  isLoading = false;

  // Success screen
  showSuccessScreen = false;
  createdClaimId?: number;
  createdAt = '';

  constructor(private claimService: ClaimService, private router: Router) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  selectCategory(id: number): void {
    this.categorieId = id;
  }

  selectPriority(p: 'FAIBLE' | 'MOYENNE' | 'ELEVEE' | 'CRITIQUE'): void {
    this.priorite = p;
  }

  getCategoryIcon(name: string): string {
    const n = name ? name.toLowerCase() : '';
    if (n.includes('virement') || n.includes('transfert')) return 'swap_horiz';
    if (n.includes('carte')) return 'credit_card';
    if (n.includes('compte')) return 'account_balance_wallet';
    if (n.includes('crédit') || n.includes('pret')) return 'request_quote';
    if (n.includes('service') || n.includes('client')) return 'support_agent';
    if (n.includes('chèque')) return 'payments';
    if (n.includes('autre')) return 'help_outline';
    return 'account_balance';
  }

  loadCategories(): void {
    this.claimService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
        if (data.length > 0 && !this.categorieId) {
          this.categorieId = data[0].id;
        }
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger la liste des catégories bancaires.';
      }
    });
  }

  onSubmit(): void {
    if (!this.titre || !this.description || !this.categorieId) {
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const newClaim: Partial<Claim> = {
      titre: this.titre,
      description: this.description,
      priorite: this.priorite,
      categorieId: this.categorieId
    };

    this.claimService.createClaim(newClaim).subscribe({
      next: (created: any) => {
        this.isLoading = false;
        this.createdClaimId = created?.id;
        this.createdAt = new Date().toLocaleDateString('fr-FR', {
          day: '2-digit', month: 'long', year: 'numeric',
          hour: '2-digit', minute: '2-digit'
        });
        this.showSuccessScreen = true;
      },
      error: (err) => {
        this.isLoading = false;
        console.error(err);
        this.errorMessage = 'Une erreur est survenue lors de l\'enregistrement du dossier. Réessayez.';
      }
    });
  }

  goToClaimDetail(): void {
    if (this.createdClaimId) {
      this.router.navigate(['/reclamations', this.createdClaimId]);
    } else {
      this.router.navigate(['/reclamations']);
    }
  }

  resetForm(): void {
    this.titre = '';
    this.description = '';
    this.priorite = 'MOYENNE';
    this.categorieId = this.categories[0]?.id;
    this.showSuccessScreen = false;
    this.createdClaimId = undefined;
    this.createdAt = '';
  }
}
