export interface Claim {
  id?: number;
  titre: string;
  description: string;
  dateCreation?: string;
  priorite: 'FAIBLE' | 'MOYENNE' | 'ELEVEE' | 'CRITIQUE';
  statut: 'NOUVELLE' | 'EN_COURS' | 'RESOLUE' | 'REJETEE';
  
  clientId?: number;
  clientNom?: string;
  clientPrenom?: string;
  clientEmail?: string;
  
  agentId?: number;
  agentNom?: string;
  agentPrenom?: string;
  
  categorieId: number;
  categorieNom?: string;
}

export interface Categorie {
  id: number;
  nom: string;
  description?: string;
}

export interface StatusHistory {
  id: number;
  ancienStatut?: 'NOUVELLE' | 'EN_COURS' | 'RESOLUE' | 'REJETEE';
  nouveauStatut: 'NOUVELLE' | 'EN_COURS' | 'RESOLUE' | 'REJETEE';
  dateModification: string;
  utilisateur: {
    id: number;
    nom: string;
    prenom: string;
    role: string;
  };
}

export interface DashboardStats {
  totalReclamations: number;
  statutCounts: { [key: string]: number };
  prioriteCounts: { [key: string]: number };
  categorieCounts: { [key: string]: number };
  agentWorkload: { [key: string]: number };
  recentReclamations: Claim[];
  totalUsers?: number;
  totalComments?: number;
  totalClients?: number;
  totalAgents?: number;
  totalAdmins?: number;
}
