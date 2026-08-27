package com.bank.complaints.controller;

import com.bank.complaints.dto.ReclamationDTO;
import com.bank.complaints.model.*;
import com.bank.complaints.repository.*;
import com.bank.complaints.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reclamations")
public class ReclamationController {

    @Autowired
    ReclamationRepository reclamationRepository;

    @Autowired
    UtilisateurRepository utilisateurRepository;

    @Autowired
    CategorieRepository categorieRepository;

    @Autowired
    HistoriqueStatutRepository historiqueStatutRepository;

    @Autowired
    EmailService emailService;

    // Get complaints based on the user's role
    @GetMapping
    public ResponseEntity<List<ReclamationDTO>> getReclamations() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur currentUser = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté non trouvé"));

        List<Reclamation> reclamations;

        if (currentUser.getRole() == Role.CLIENT) {
            reclamations = reclamationRepository.findByClientId(currentUser.getId());
        } else if (currentUser.getRole() == Role.AGENT) {
            reclamations = reclamationRepository.findByAgentId(currentUser.getId());
        } else {
            reclamations = reclamationRepository.findAll();
        }

        List<ReclamationDTO> dtos = reclamations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Get details of a single complaint
    @GetMapping("/{id}")
    public ResponseEntity<ReclamationDTO> getReclamationById(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur currentUser = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté non trouvé"));

        Reclamation rec = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));

        // Security check: Client can only view their own; Agent can only view their assigned ones (unless they are Admin)
        if (currentUser.getRole() == Role.CLIENT && !rec.getClient().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }
        if (currentUser.getRole() == Role.AGENT && (rec.getAgent() == null || !rec.getAgent().getId().equals(currentUser.getId()))) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(convertToDto(rec));
    }

    // Create a new complaint (Clients only)
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createReclamation(@RequestBody ReclamationDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur currentClient = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté non trouvé"));

        Categorie cat = categorieRepository.findById(dto.getCategorieId())
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));

        Reclamation rec = new Reclamation();
        rec.setTitre(dto.getTitre());
        rec.setDescription(dto.getDescription());
        rec.setPriorite(dto.getPriorite() != null ? dto.getPriorite() : Priorite.MOYENNE);
        rec.setStatut(Statut.NOUVELLE);
        rec.setClient(currentClient);
        rec.setCategorie(cat);

        Reclamation saved = reclamationRepository.save(rec);
        
        // Log the creation state in the history
        HistoriqueStatut hist = new HistoriqueStatut(null, Statut.NOUVELLE, saved, currentClient);
        historiqueStatutRepository.save(hist);

        return ResponseEntity.ok(convertToDto(saved));
    }

    // Assign an Agent (Admins only)
    @PutMapping("/{id}/assigner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignAgent(@PathVariable Long id, @RequestParam(required = false) Long agentId) {
        Reclamation rec = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));

        Utilisateur agent = null;
        if (agentId != null) {
            agent = utilisateurRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
            if (agent.getRole() != Role.AGENT) {
                return ResponseEntity.badRequest().body("Erreur: L'utilisateur assigné doit avoir le rôle AGENT");
            }
        }

        rec.setAgent(agent);
        // Automatically move to EN_COURS when assigned to an agent, if it was NOUVELLE
        Statut oldStatut = rec.getStatut();
        if (agent != null && rec.getStatut() == Statut.NOUVELLE) {
            rec.setStatut(Statut.EN_COURS);
            
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Utilisateur currentAdmin = utilisateurRepository.findByEmail(email).get();
            
            HistoriqueStatut hist = new HistoriqueStatut(oldStatut, Statut.EN_COURS, rec, currentAdmin);
            historiqueStatutRepository.save(hist);
        }

        Reclamation updated = reclamationRepository.save(rec);
        return ResponseEntity.ok(convertToDto(updated));
    }

    // Update Status (Agents or Admins)
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam Statut statut) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur currentUser = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté non trouvé"));

        Reclamation rec = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));

        // Only Admin or the assigned Agent can update the status
        if (currentUser.getRole() == Role.AGENT && (rec.getAgent() == null || !rec.getAgent().getId().equals(currentUser.getId()))) {
            return ResponseEntity.status(403).body("Erreur: Vous n'êtes pas l'agent assigné à cette réclamation");
        }
        if (currentUser.getRole() == Role.CLIENT) {
            return ResponseEntity.status(403).body("Erreur: Les clients ne peuvent pas modifier le statut");
        }

        Statut oldStatut = rec.getStatut();
        rec.setStatut(statut);
        Reclamation updated = reclamationRepository.save(rec);

        // Add to history
        HistoriqueStatut hist = new HistoriqueStatut(oldStatut, statut, updated, currentUser);
        historiqueStatutRepository.save(hist);

        if (statut == Statut.RESOLUE && oldStatut != Statut.RESOLUE && updated.getClient() != null) {
            emailService.sendReclamationResolvedEmail(updated, currentUser);
        }

        return ResponseEntity.ok(convertToDto(updated));
    }

    // Get Status Timeline History
    @GetMapping("/{id}/historique")
    public ResponseEntity<List<HistoriqueStatut>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(historiqueStatutRepository.findByReclamationIdOrderByDateModificationAsc(id));
    }

    private ReclamationDTO convertToDto(Reclamation rec) {
        ReclamationDTO dto = new ReclamationDTO();
        dto.setId(rec.getId());
        dto.setTitre(rec.getTitre());
        dto.setDescription(rec.getDescription());
        dto.setDateCreation(rec.getDateCreation());
        dto.setPriorite(rec.getPriorite());
        dto.setStatut(rec.getStatut());

        if (rec.getClient() != null) {
            dto.setClientId(rec.getClient().getId());
            dto.setClientNom(rec.getClient().getNom());
            dto.setClientPrenom(rec.getClient().getPrenom());
            dto.setClientEmail(rec.getClient().getEmail());
        }

        if (rec.getAgent() != null) {
            dto.setAgentId(rec.getAgent().getId());
            dto.setAgentNom(rec.getAgent().getNom());
            dto.setAgentPrenom(rec.getAgent().getPrenom());
        }

        if (rec.getCategorie() != null) {
            dto.setCategorieId(rec.getCategorie().getId());
            dto.setCategorieNom(rec.getCategorie().getNom());
        }

        return dto;
    }
}
