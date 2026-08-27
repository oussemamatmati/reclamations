package com.bank.complaints.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reclamation")
public class Reclamation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_creation", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('FAIBLE','MOYENNE','ELEVEE','CRITIQUE') DEFAULT 'MOYENNE'")
    private Priorite priorite = Priorite.MOYENNE;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('NOUVELLE','EN_COURS','RESOLUE','REJETEE') DEFAULT 'NOUVELLE'")
    private Statut statut = Statut.NOUVELLE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private Utilisateur client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    // Constructors
    public Reclamation() {}

    public Reclamation(String titre, String description, Priorite priorite, Statut statut, Utilisateur client, Categorie categorie) {
        this.titre = titre;
        this.description = description;
        this.priorite = priorite;
        this.statut = statut;
        this.client = client;
        this.categorie = categorie;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Priorite getPriorite() {
        return priorite;
    }

    public void setPriorite(Priorite priorite) {
        this.priorite = priorite;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public Utilisateur getClient() {
        return client;
    }

    public void setClient(Utilisateur client) {
        this.client = client;
    }

    public Utilisateur getAgent() {
        return agent;
    }

    public void setAgent(Utilisateur agent) {
        this.agent = agent;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }
}
