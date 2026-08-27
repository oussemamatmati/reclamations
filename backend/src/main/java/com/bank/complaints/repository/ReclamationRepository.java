package com.bank.complaints.repository;

import com.bank.complaints.model.Reclamation;
import com.bank.complaints.model.Statut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findByClientId(Long clientId);
    List<Reclamation> findByAgentId(Long agentId);
    List<Reclamation> findByStatut(Statut statut);
}
