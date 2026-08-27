package com.bank.complaints;

import com.bank.complaints.model.Categorie;
import com.bank.complaints.model.Role;
import com.bank.complaints.model.Utilisateur;
import com.bank.complaints.repository.CategorieRepository;
import com.bank.complaints.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class ComplaintsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComplaintsApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(
            UtilisateurRepository utilisateurRepository,
            CategorieRepository categorieRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            // Seed Categories if empty
            if (categorieRepository.count() == 0) {
                List<Categorie> categories = Arrays.asList(
                        new Categorie("Carte Bancaire", "Réclamation liée à une carte bancaire"),
                        new Categorie("Compte Bancaire", "Problème concernant un compte bancaire"),
                        new Categorie("Virement", "Erreur ou retard de virement"),
                        new Categorie("Crédit", "Réclamation concernant un crédit"),
                        new Categorie("Chèque", "Problème de chèque"),
                        new Categorie("Banque en ligne", "Application ou site web bancaire"),
                        new Categorie("Autres", "Autre type de réclamation")
                );
                categorieRepository.saveAll(categories);
                System.out.println("Inserted default categories.");
            } else {
                boolean hasAutres = categorieRepository.findAll().stream()
                        .anyMatch(c -> "Autres".equalsIgnoreCase(c.getNom()));
                if (!hasAutres) {
                    categorieRepository.save(new Categorie("Autres", "Autre type de réclamation"));
                    System.out.println("Inserted 'Autres' category.");
                }
            }

            // Seed Users if empty
            if (utilisateurRepository.count() == 0) {
                Utilisateur admin = new Utilisateur(
                        "System", "Admin", "admin@banque.com",
                        passwordEncoder.encode("admin123"), "00000000", Role.ADMIN
                );
                admin.setEmailVerified(true);
                Utilisateur agent = new Utilisateur(
                        "Ben Ali", "Ahmed", "agent@banque.com",
                        passwordEncoder.encode("agent123"), "11111111", Role.AGENT
                );
                agent.setEmailVerified(true);
                Utilisateur client = new Utilisateur(
                        "Trabelsi", "Mohamed", "client@gmail.com",
                        passwordEncoder.encode("client123"), "22222222", Role.CLIENT
                );
                client.setEmailVerified(true);
                utilisateurRepository.saveAll(Arrays.asList(admin, agent, client));
                System.out.println("Inserted default seed users (admin@banque.com / admin123, agent@banque.com / agent123, client@gmail.com / client123).");
            }
        };
    }
}
