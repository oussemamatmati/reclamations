package com.bank.complaints.dto;

import java.time.LocalDateTime;

public class CommentaireDTO {
    private Long id;
    private String contenu;
    private LocalDateTime dateCommentaire;
    private Long reclamationId;
    private Long agentId;
    private String agentNom;
    private String agentPrenom;
    private String agentRole;

    // Constructors
    public CommentaireDTO() {}

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

    public Long getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(Long reclamationId) {
        this.reclamationId = reclamationId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getAgentNom() {
        return agentNom;
    }

    public void setAgentNom(String agentNom) {
        this.agentNom = agentNom;
    }

    public String getAgentPrenom() {
        return agentPrenom;
    }

    public void setAgentPrenom(String agentPrenom) {
        this.agentPrenom = agentPrenom;
    }

    public String getAgentRole() {
        return agentRole;
    }

    public void setAgentRole(String agentRole) {
        this.agentRole = agentRole;
    }
}
