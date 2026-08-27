export interface Comment {
  id?: number;
  contenu: string;
  dateCommentaire?: string;
  reclamationId: number;
  agentId?: number;
  agentNom?: string;
  agentPrenom?: string;
  agentRole?: string;
}
