package com.bank.complaints.repository;

import com.bank.complaints.model.Utilisateur;
import com.bank.complaints.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByVerificationCode(String verificationCode);
    Boolean existsByEmail(String email);
    List<Utilisateur> findByRole(Role role);
}
