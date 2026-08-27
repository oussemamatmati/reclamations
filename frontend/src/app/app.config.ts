import { ApplicationConfig } from '@angular/core';
import { provideRouter, Routes } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';

import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { VerifyEmailComponent } from './components/verify-email/verify-email.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { ClaimListComponent } from './components/claim-list/claim-list.component';
import { ClaimCreateComponent } from './components/claim-create/claim-create.component';
import { ClaimDetailComponent } from './components/claim-detail/claim-detail.component';
import { UserCrudComponent } from './components/user-crud/user-crud.component';
import { CommentCrudComponent } from './components/comment-crud/comment-crud.component';
import { AdminStatsComponent } from './components/admin-stats/admin-stats.component';
import { authGuard } from './guards/auth.guard';
import { jwtInterceptor } from './interceptors/jwt.interceptor';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'reclamations', component: ClaimListComponent, canActivate: [authGuard] },
  { path: 'reclamations/nouvelle', component: ClaimCreateComponent, canActivate: [authGuard] },
  { path: 'reclamations/:id', component: ClaimDetailComponent, canActivate: [authGuard] },
  { path: 'admin/utilisateurs', component: UserCrudComponent, canActivate: [authGuard] },
  { path: 'admin/commentaires', component: CommentCrudComponent, canActivate: [authGuard] },
  { path: 'admin/statistiques', component: AdminStatsComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimations()
  ]
};
