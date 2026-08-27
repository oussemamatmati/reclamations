import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-user-crud',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-crud.component.html',
  styleUrl: './user-crud.component.css'
})
export class UserCrudComponent implements OnInit {
  users: User[] = [];
  filteredUsers: User[] = [];
  isLoading = true;
  errorMessage = '';
  successMessage = '';

  // Filter properties
  searchTerm = '';
  roleFilter = '';

  // Form properties
  showFormModal = false;
  isEditMode = false;
  formUser: Partial<User> = {};
  userPassword = '';
  passwordConfirmation = '';
  formError = '';

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading = true;
    this.userService.getUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger la liste des utilisateurs.';
        this.isLoading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredUsers = this.users.filter(user => {
      const matchesSearch = !this.searchTerm ||
        user.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        user.prenom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        user.email.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesRole = !this.roleFilter || user.role === this.roleFilter;

      return matchesSearch && matchesRole;
    });
  }

  openAddModal(): void {
    this.isEditMode = false;
    this.formUser = {
      nom: '',
      prenom: '',
      email: '',
      telephone: '',
      role: 'CLIENT'
    };
    this.userPassword = '';
    this.passwordConfirmation = '';
    this.formError = '';
    this.showFormModal = true;
  }

  openEditModal(user: User): void {
    this.isEditMode = true;
    this.formUser = { ...user };
    this.userPassword = '';
    this.passwordConfirmation = '';
    this.formError = '';
    this.showFormModal = true;
  }

  closeModal(): void {
    this.showFormModal = false;
  }

  onSubmit(): void {
    this.formError = '';

    if (!this.formUser.nom || !this.formUser.prenom || !this.formUser.email) {
      this.formError = 'Veuillez remplir tous les champs obligatoires.';
      return;
    }

    if (!this.isEditMode) {
      if (!this.userPassword) {
        this.formError = 'Le mot de passe est obligatoire pour la création.';
        return;
      }
      if (this.userPassword !== this.passwordConfirmation) {
        this.formError = 'Les mots de passe ne correspondent pas.';
        return;
      }
    } else {
      if (this.userPassword && this.userPassword !== this.passwordConfirmation) {
        this.formError = 'Les mots de passe ne correspondent pas.';
        return;
      }
    }

    const payload: User = {
      id: this.formUser.id,
      nom: this.formUser.nom,
      prenom: this.formUser.prenom,
      email: this.formUser.email,
      telephone: this.formUser.telephone,
      role: this.formUser.role || 'CLIENT',
      ...(this.userPassword ? { motDePasse: this.userPassword } : {})
    };

    if (this.isEditMode && payload.id) {
      this.userService.updateUser(payload.id, payload).subscribe({
        next: () => {
          this.successMessage = 'Utilisateur mis à jour avec succès.';
          this.loadUsers();
          this.closeModal();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          console.error(err);
          this.formError = err.error || 'Erreur lors de la mise à jour.';
        }
      });
    } else {
      // In Spring Boot, we set the password in 'motDePasse' or 'password' depending on Utilisateur entity fields.
      // Wait, let's verify if the backend Utilisateur model uses 'motDePasse'.
      // Let's set it as 'motDePasse'. In UtilisateurController, we saw:
      // user.setMotDePasse(passwordEncoder.encode(user.getMotDePasse()));
      // Yes, it uses motDePasse! But let's check if the dto or model uses that field name.
      // Yes, in payload we pass motDePasse. Let's make sure it's mapped.
      this.userService.createUser(payload).subscribe({
        next: () => {
          this.successMessage = 'Utilisateur créé avec succès.';
          this.loadUsers();
          this.closeModal();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          console.error(err);
          this.formError = err.error || 'Erreur lors de la création.';
        }
      });
    }
  }

  deleteUser(id?: number): void {
    if (!id || !confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ?')) return;

    this.userService.deleteUser(id).subscribe({
      next: () => {
        this.successMessage = 'Utilisateur supprimé avec succès.';
        this.loadUsers();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de supprimer cet utilisateur.';
      }
    });
  }

  getRoleBadgeClass(role?: string): string {
    if (role === 'ADMIN') return 'badge-role-admin';
    if (role === 'AGENT') return 'badge-role-agent';
    return 'badge-role-client';
  }
}
