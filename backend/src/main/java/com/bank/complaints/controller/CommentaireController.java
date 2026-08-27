package com.bank.complaints.controller;

import com.bank.complaints.dto.CommentaireDTO;
import com.bank.complaints.model.Commentaire;
import com.bank.complaints.model.Reclamation;
import com.bank.complaints.model.Utilisateur;
import com.bank.complaints.repository.CommentaireRepository;
import com.bank.complaints.repository.ReclamationRepository;
import com.bank.complaints.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/commentaires")
public class CommentaireController {

    @Autowired
    CommentaireRepository commentaireRepository;

    @Autowired
    ReclamationRepository reclamationRepository;

    @Autowired
    UtilisateurRepository utilisateurRepository;

    @GetMapping("/reclamation/{reclamationId}")
    public ResponseEntity<List<CommentaireDTO>> getCommentsByReclamation(@PathVariable Long reclamationId) {
        List<Commentaire> comments = commentaireRepository.findByReclamationIdOrderByDateCommentaireAsc(reclamationId);
        List<CommentaireDTO> dtos = comments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> addComment(@RequestBody CommentaireDTO requestDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur currentUser = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté non trouvé"));

        Reclamation reclamation = reclamationRepository.findById(requestDto.getReclamationId())
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));

        Commentaire comment = new Commentaire(requestDto.getContenu(), reclamation, currentUser);
        Commentaire saved = commentaireRepository.save(comment);

        return ResponseEntity.ok(convertToDto(saved));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommentaireDTO>> getAllComments() {
        List<Commentaire> comments = commentaireRepository.findAll();
        List<CommentaireDTO> dtos = comments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        return commentaireRepository.findById(id)
                .map(comment -> {
                    commentaireRepository.delete(comment);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private CommentaireDTO convertToDto(Commentaire comment) {
        CommentaireDTO dto = new CommentaireDTO();
        dto.setId(comment.getId());
        dto.setContenu(comment.getContenu());
        dto.setDateCommentaire(comment.getDateCommentaire());
        dto.setReclamationId(comment.getReclamation().getId());
        
        if (comment.getAgent() != null) {
            dto.setAgentId(comment.getAgent().getId());
            dto.setAgentNom(comment.getAgent().getNom());
            dto.setAgentPrenom(comment.getAgent().getPrenom());
            dto.setAgentRole(comment.getAgent().getRole().name());
        }
        return dto;
    }
}
