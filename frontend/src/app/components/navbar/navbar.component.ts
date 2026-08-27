import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Observable } from 'rxjs';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {
  currentUser$: Observable<User | null>;
  isCommandPaletteOpen = false;

  constructor(private authService: AuthService, private router: Router) {
    this.currentUser$ = this.authService.currentUser$;
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.toggleCommandPalette();
    } else if (event.key === 'Escape' && this.isCommandPaletteOpen) {
      this.isCommandPaletteOpen = false;
    }
  }

  toggleCommandPalette(): void {
    this.isCommandPaletteOpen = !this.isCommandPaletteOpen;
  }

  navigateAndClose(path: string): void {
    this.isCommandPaletteOpen = false;
    this.router.navigate([path]);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
