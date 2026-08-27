import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit {
  currentUser: User | null = null;

  constructor(private authService: AuthService, private router: Router) { }

  ngOnInit(): void {
    this.currentUser = this.authService.currentUserValue;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
