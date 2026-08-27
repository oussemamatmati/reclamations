package com.bank.complaints.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "commentaire")
public class Commentaire {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "date_commentaire", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateCommentaire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reclamation_id", nullable = false)
    private Reclamation reclamation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id", nullable = false)
    private Utilisateur agent; // Author of the comment (referencing utilisateur table via agent_id)

    // Constructors
    public Commentaire() {}

    public Commentaire(String contenu, Reclamation reclamation, Utilisateur agent) {
        this.contenu = contenu;
        this.reclamation = reclamation;
        this.agent = agent;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateCommentaire() {
        return dateCommentaire;
    }

    public void setDateCommentaire(LocalDateTime dateCommentaire) {
        this.dateCommentaire = dateCommentaire;
    }

    public Reclamation getReclamation() {
        return reclamation;
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
    }

    public Utilisateur getAgent() {
        return agent;
    }

    public void setAgent(Utilisateur agent) {
        this.agent = agent;
    }
}
