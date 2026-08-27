import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './components/navbar/navbar.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, NavbarComponent, SidebarComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  get isLoggedIn(): boolean {
    return this.authService.isAuthenticated;
  }

  get isAdmin(): boolean {
    const user = this.authService.currentUserValue;
    return user?.role === 'ADMIN';
  }

  constructor(private authService: AuthService) {}
}
