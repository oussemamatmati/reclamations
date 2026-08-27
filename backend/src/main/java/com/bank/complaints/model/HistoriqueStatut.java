package com.bank.complaints.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_statut")
public class HistoriqueStatut {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut")
    private Statut ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut")
    private Statut nouveauStatut;

    @Column(name = "date_modification", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reclamation_id", nullable = false)
    private Reclamation reclamation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur; // User who made the change

    // Constructors
    public HistoriqueStatut() {}

    public HistoriqueStatut(Statut ancienStatut, Statut nouveauStatut, Reclamation reclamation, Utilisateur utilisateur) {
        this.ancienStatut = ancienStatut;
        this.nouveauStatut = nouveauStatut;
        this.reclamation = reclamation;
        this.utilisateur = utilisateur;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Statut getAncienStatut() {
        return ancienStatut;
    }

    public void setAncienStatut(Statut ancienStatut) {
        this.ancienStatut = ancienStatut;
    }

    public Statut getNouveauStatut() {
        return nouveauStatut;
    }

    public void setNouveauStatut(Statut nouveauStatut) {
        this.nouveauStatut = nouveauStatut;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public Reclamation getReclamation() {
        return reclamation;
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }
}
