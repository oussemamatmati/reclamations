import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ThreeBgComponent } from '../three-bg/three-bg.component';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ThreeBgComponent],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.css'
})
export class VerifyEmailComponent implements OnInit {
  code = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  // 3D Tilt Card state
  cardTransform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
  glarePosition = '50% 50%';
  glareOpacity = 0;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const codeParam = this.route.snapshot.queryParams['code'];
    if (codeParam) {
      this.code = codeParam;
      this.verifyCode(this.code);
    }
  }

  // 3D Card Tilt Physics Calculation
  onMouseMove(event: MouseEvent): void {
    const card = event.currentTarget as HTMLElement;
    if (!card) return;

    const rect = card.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const rotateX = ((y - centerY) / centerY) * -10;
    const rotateY = ((x - centerX) / centerX) * 10;

    this.cardTransform = `perspective(1000px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale3d(1.02, 1.02, 1.02)`;

    const glareX = (x / rect.width) * 100;
    const glareY = (y / rect.height) * 100;
    this.glarePosition = `${glareX}% ${glareY}%`;
    this.glareOpacity = 0.35;
  }

  onMouseLeave(): void {
    this.cardTransform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
    this.glareOpacity = 0;
  }

  onSubmit(): void {
    if (!this.code || this.code.trim().length === 0) {
      this.errorMessage = 'Veuillez saisir votre code de vérification.';
      return;
    }
    this.verifyCode(this.code.trim());
  }

  private verifyCode(codeToVerify: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.verifyEmail(codeToVerify).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successMessage = response || 'Adresse email vérifiée avec succès ! Redirection vers la page de connexion...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        console.error(err);
        if (err.error && typeof err.error === 'string') {
          this.errorMessage = err.error;
        } else {
          this.errorMessage = 'Code de vérification invalide ou expiré.';
        }
      }
    });
  }
}
