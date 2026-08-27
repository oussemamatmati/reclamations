import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ThreeBgComponent } from '../three-bg/three-bg.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ThreeBgComponent],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  nom = '';
  prenom = '';
  email = '';
  motDePasse = '';
  telephone = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  // Interactive UI States
  showPassword = false;

  // Password Strength State
  strengthScore = 0; // 0 to 4
  strengthLabel = '';
  strengthColor = '#e2e8f0';

  constructor(private authService: AuthService, private router: Router) {
    if (this.authService.isAuthenticated) {
      this.router.navigate(['/dashboard']);
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  // Password Strength Evaluator
  onPasswordInput(): void {
    const p = this.motDePasse;
    if (!p) {
      this.strengthScore = 0;
      this.strengthLabel = '';
      this.strengthColor = '#e2e8f0';
      return;
    }

    let score = 0;
    if (p.length >= 8) score++;
    if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score++;
    if (/[0-9]/.test(p)) score++;
    if (/[^A-Za-z0-9]/.test(p)) score++;

    this.strengthScore = score;

    switch (score) {
      case 1:
        this.strengthLabel = 'Faible';
        this.strengthColor = '#ef4444';
        break;
      case 2:
        this.strengthLabel = 'Moyen';
        this.strengthColor = '#f59e0b';
        break;
      case 3:
        this.strengthLabel = 'Bon';
        this.strengthColor = '#3b82f6';
        break;
      case 4:
        this.strengthLabel = 'Très Fort';
        this.strengthColor = '#10b981';
        break;
      default:
        this.strengthLabel = 'Trop court';
        this.strengthColor = '#ef4444';
        break;
    }
  }

  validateForm(): boolean {
    this.errorMessage = '';

    // Nom validation
    if (!this.nom || !this.nom.trim()) {
      this.errorMessage = 'Le nom est obligatoire.';
      return false;
    }
    if (!/^[a-zA-ZÀ-ÿ\s'-]{2,}$/.test(this.nom.trim())) {
      this.errorMessage = 'Le nom doit contenir au moins 2 lettres.';
      return false;
    }

    // Prénom validation
    if (!this.prenom || !this.prenom.trim()) {
      this.errorMessage = 'Le prénom est obligatoire.';
      return false;
    }
    if (!/^[a-zA-ZÀ-ÿ\s'-]{2,}$/.test(this.prenom.trim())) {
      this.errorMessage = 'Le prénom doit contenir au moins 2 lettres.';
      return false;
    }

    // Email validation
    if (!this.email || !this.email.trim()) {
      this.errorMessage = 'L\'adresse email est obligatoire.';
      return false;
    }
    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if (!emailPattern.test(this.email.trim())) {
      this.errorMessage = 'Veuillez saisir une adresse email valide.';
      return false;
    }

    // Password validation
    if (!this.motDePasse) {
      this.errorMessage = 'Le mot de passe est obligatoire.';
      return false;
    }
    if (this.motDePasse.length < 8) {
      this.errorMessage = 'Le mot de passe doit contenir au moins 8 caractères.';
      return false;
    }
    if (this.strengthScore < 2) {
      this.errorMessage = 'Veuillez choisir un mot de passe plus fort (ajoutez des chiffres ou des caractères spéciaux).';
      return false;
    }

    // Telephone validation
    if (this.telephone && this.telephone.trim()) {
      const phonePattern = /^[0-9]{8}$/;
      if (!phonePattern.test(this.telephone.trim())) {
        this.errorMessage = 'Le numéro de téléphone doit contenir exactement 8 chiffres.';
        return false;
      }
    }

    return true;
  }

  onSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.register(this.nom.trim(), this.prenom.trim(), this.email.trim(), this.motDePasse, this.telephone ? this.telephone.trim() : '').subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Inscription réussie ! Un code vous a été envoyé par email. Redirection...';
        setTimeout(() => {
          this.router.navigate(['/verify-email']);
        }, 1500);
      },
      error: (err) => {
        this.isLoading = false;
        console.error(err);
        if (err.status === 400) {
          this.errorMessage = 'Erreur : Cet email est déjà utilisé.';
        } else {
          this.errorMessage = 'Une erreur est survenue lors de l\'inscription. Veuillez réessayer.';
        }
      }
    });
  }
}
