import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClaimService } from '../../services/claim.service';
import { AuthService } from '../../services/auth.service';
import { DashboardStats } from '../../models/claim.model';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  stats?: DashboardStats;
  currentUser: User | null = null;
  isLoading = true;
  errorMessage = '';
  chatbotQuestion = '';
  chatbotLoading = false;
  isChatOpen = false;

  // Quick suggestion prompts for the AI Chatbot
  quickQuestions = [
    'Quels sont les statuts actuels ?',
    'Combien de réclamations en cours ?',
    'Catégories les plus fréquentes'
  ];

  // 3D Tilt Card Transforms for Dashboard Stat Cards
  cardTransforms: { [key: string]: string } = {};

  chatMessages: Array<{ role: 'user' | 'assistant'; text: string }> = [
    {
      role: 'assistant',
      text: 'Bonjour ! Je suis votre Assistant IA BCT. Comment puis-je vous aider aujourd’hui sur vos données de réclamations ?'
    }
  ];

  constructor(private claimService: ClaimService, private authService: AuthService) {
    this.currentUser = this.authService.currentUserValue;
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.claimService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
        this.errorMessage = 'Impossible de charger les statistiques du tableau de bord.';
      }
    });
  }

  onCardMouseMove(event: MouseEvent, cardKey: string): void {
    const card = event.currentTarget as HTMLElement;
    if (!card) return;

    const rect = card.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const rotateX = ((y - centerY) / centerY) * -8;
    const rotateY = ((x - centerX) / centerX) * 8;

    this.cardTransforms[cardKey] = `perspective(1000px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale3d(1.03, 1.03, 1.03)`;
  }

  onCardMouseLeave(cardKey: string): void {
    this.cardTransforms[cardKey] = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
  }

  getMapKeys(map?: { [key: string]: any }): string[] {
    return map ? Object.keys(map) : [];
  }

  getPercentage(count: number): number {
    if (!this.stats || this.stats.totalReclamations === 0) return 0;
    return Math.round((count / this.stats.totalReclamations) * 100);
  }

  toggleChat(): void {
    this.isChatOpen = !this.isChatOpen;
  }

  sendQuickQuestion(question: string): void {
    this.chatbotQuestion = question;
    this.askChatbot();
  }

  askChatbot(): void {
    const question = this.chatbotQuestion.trim();

    if (!question) {
      this.chatMessages.push({
        role: 'assistant',
        text: 'Posez une question pour lancer l’assistant sur vos données de site.'
      });
      return;
    }

    this.chatMessages.push({ role: 'user', text: question });
    this.chatbotQuestion = '';
    this.chatbotLoading = true;

    this.claimService.askChatbot(question).subscribe({
      next: (response) => {
        this.chatMessages.push({ role: 'assistant', text: response.answer });
        this.chatbotLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.chatMessages.push({
          role: 'assistant',
          text: 'L’assistant n’a pas pu répondre pour le moment. Vérifiez votre connexion ou vos droits d’accès.'
        });
        this.chatbotLoading = false;
      }
    });
  }
}
