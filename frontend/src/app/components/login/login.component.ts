import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ThreeBgComponent } from '../three-bg/three-bg.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ThreeBgComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  email = '';
  motDePasse = '';
  errorMessage = '';
  isUnverified = false;
  isLoading = false;

  // Interactive UI States
  showPassword = false;

  constructor(private authService: AuthService, private router: Router) {
    if (this.authService.isAuthenticated) {
      this.router.navigate(['/dashboard']);
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (!this.email || !this.motDePasse) {
      this.errorMessage = 'Veuillez remplir tous les champs.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.isUnverified = false;

    this.authService.login(this.email, this.motDePasse).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        console.error(err);
        if (err.status === 403) {
          this.errorMessage = typeof err.error === 'string' ? err.error : 'Veuillez vérifier votre adresse email avant de vous connecter.';
          this.isUnverified = true;
        } else if (err.status === 401) {
          this.errorMessage = 'Email ou mot de passe incorrect.';
        } else {
          this.errorMessage = 'Une erreur est survenue lors de la connexion. Veuillez réessayer.';
        }
      }
    });
  }
}
