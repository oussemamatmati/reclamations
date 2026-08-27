package com.bank.complaints.repository;

import com.bank.complaints.model.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {
    List<Commentaire> findByReclamationIdOrderByDateCommentaireAsc(Long reclamationId);
}
