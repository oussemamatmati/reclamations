package com.bank.complaints.dto;

public class LoginRequest {
    private String email;
    private String motDePasse;

    // Constructors
    public LoginRequest() {}

    public LoginRequest(String email, String motDePasse) {
        this.email = email;
        this.motDePasse = motDePasse;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }
}
