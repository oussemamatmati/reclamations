export interface User {
  id?: number;
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  role: 'ADMIN' | 'AGENT' | 'CLIENT';
  token?: string;
  motDePasse?: string;
}
