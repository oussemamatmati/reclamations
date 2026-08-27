package com.bank.complaints.service;

import com.bank.complaints.model.Reclamation;
import com.bank.complaints.model.Statut;
import com.bank.complaints.model.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String fromEmail;

    public void sendVerificationEmail(Utilisateur user, String verificationCode) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            logger.warn("Skipping verification email for {} because no sender email is configured", user.getEmail());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("Code de vérification de votre compte");
        message.setText("Bonjour " + user.getPrenom() + " " + user.getNom() + ",\n\n"
                + "Merci pour votre inscription. Veuillez utiliser le code de vérification suivant :\n\n"
                + "CODE: " + verificationCode + "\n\n"
                + "Ce code est valide pendant 24 heures.\n\n"
                + "Cordialement,\n"
                + "L'équipe de gestion des réclamations");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            logger.warn("Unable to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public void sendReclamationResolvedEmail(Reclamation reclamation, Utilisateur actor) {
        if (reclamation == null || reclamation.getClient() == null || reclamation.getClient().getEmail() == null) {
            return;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            logger.warn("Skipping resolved complaint email for {} because no sender email is configured", reclamation.getClient().getEmail());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(reclamation.getClient().getEmail());
        message.setSubject("Votre réclamation a été résolue");
        message.setText("Bonjour " + reclamation.getClient().getPrenom() + " " + reclamation.getClient().getNom() + ",\n\n"
                + "Nous vous informons que votre réclamation \"" + reclamation.getTitre() + "\" a été marquée comme "
                + Statut.RESOLUE.name().replace("_", " ") + ".\n"
                + "Une action a été effectuée par " + (actor != null ? actor.getPrenom() + " " + actor.getNom() : "l'équipe") + ".\n\n"
                + "Cordialement,\n"
                + "L'équipe de gestion des réclamations");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            logger.warn("Unable to send resolved complaint email to {}: {}", reclamation.getClient().getEmail(), e.getMessage());
        }
    }
}
