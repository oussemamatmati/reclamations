package com.bank.complaints.controller;

import com.bank.complaints.model.Role;
import com.bank.complaints.model.Utilisateur;
import com.bank.complaints.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    @Autowired
    UtilisateurRepository utilisateurRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    // Get all agents (accessible by authenticated users, e.g., admins assigning claims)
    @GetMapping("/agents")
    public ResponseEntity<List<Utilisateur>> getAgents() {
        return ResponseEntity.ok(utilisateurRepository.findByRole(Role.AGENT));
    }

    // Admin CRUD operations
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Utilisateur>> getAllUsers() {
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Utilisateur> getUserById(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody Utilisateur user) {
        if (utilisateurRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("Erreur: Email déjà utilisé !");
        }
        user.setMotDePasse(passwordEncoder.encode(user.getMotDePasse()));
        return ResponseEntity.ok(utilisateurRepository.save(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Utilisateur userDetails) {
        return utilisateurRepository.findById(id)
                .map(user -> {
                    user.setNom(userDetails.getNom());
                    user.setPrenom(userDetails.getPrenom());
                    user.setEmail(userDetails.getEmail());
                    user.setTelephone(userDetails.getTelephone());
                    user.setRole(userDetails.getRole());
                    if (userDetails.getMotDePasse() != null && !userDetails.getMotDePasse().isEmpty()) {
                        user.setMotDePasse(passwordEncoder.encode(userDetails.getMotDePasse()));
                    }
                    return ResponseEntity.ok(utilisateurRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(user -> {
                    utilisateurRepository.delete(user);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
