package com.bank.complaints.repository;

import com.bank.complaints.model.HistoriqueStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueStatutRepository extends JpaRepository<HistoriqueStatut, Long> {
    List<HistoriqueStatut> findByReclamationIdOrderByDateModificationAsc(Long reclamationId);
}
