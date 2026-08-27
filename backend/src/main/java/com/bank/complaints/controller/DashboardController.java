package com.bank.complaints.controller;

import com.bank.complaints.dto.ChatbotRequest;
import com.bank.complaints.dto.ChatbotResponse;
import com.bank.complaints.dto.DashboardStats;
import com.bank.complaints.dto.ReclamationDTO;
import com.bank.complaints.model.Priorite;
import com.bank.complaints.model.Reclamation;
import com.bank.complaints.model.Role;
import com.bank.complaints.model.Statut;
import com.bank.complaints.model.Utilisateur;
import com.bank.complaints.repository.ReclamationRepository;
import com.bank.complaints.repository.UtilisateurRepository;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    ReclamationRepository reclamationRepository;

    @Autowired
    UtilisateurRepository utilisateurRepository;

    @Autowired
    com.bank.complaints.repository.CommentaireRepository commentaireRepository;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
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

        long total = reclamations.size();

        // Group status counts and fill missing with 0
        Map<String, Long> statusCounts = reclamations.stream()
                .collect(Collectors.groupingBy(r -> r.getStatut().name(), Collectors.counting()));
        for (Statut s : Statut.values()) {
            statusCounts.putIfAbsent(s.name(), 0L);
        }

        // Group priority counts and fill missing with 0
        Map<String, Long> priorityCounts = reclamations.stream()
                .collect(Collectors.groupingBy(r -> r.getPriorite().name(), Collectors.counting()));
        for (Priorite p : Priorite.values()) {
            priorityCounts.putIfAbsent(p.name(), 0L);
        }

        // Group category counts
        Map<String, Long> categoryCounts = reclamations.stream()
                .filter(r -> r.getCategorie() != null)
                .collect(Collectors.groupingBy(r -> r.getCategorie().getNom(), Collectors.counting()));

        // Group agent workloads
        Map<String, Long> agentWorkload = new HashMap<>();
        long totalUsers = 0;
        long totalComments = 0;
        long totalClients = 0;
        long totalAgents = 0;
        long totalAdmins = 0;

        if (currentUser.getRole() == Role.ADMIN) {
            agentWorkload = reclamations.stream()
                    .filter(r -> r.getAgent() != null)
                    .collect(Collectors.groupingBy(r -> r.getAgent().getPrenom() + " " + r.getAgent().getNom(), Collectors.counting()));
            totalUsers = utilisateurRepository.count();
            totalComments = commentaireRepository.count();
            totalClients = utilisateurRepository.findByRole(Role.CLIENT).size();
            totalAgents = utilisateurRepository.findByRole(Role.AGENT).size();
            totalAdmins = utilisateurRepository.findByRole(Role.ADMIN).size();
        }

        // Fetch recent claims (limit 5)
        List<ReclamationDTO> recent = reclamations.stream()
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
                .limit(5)
                .map(this::convertToDto)
                .collect(Collectors.toList());

        DashboardStats stats = new DashboardStats(
                total,
                statusCounts,
                priorityCounts,
                categoryCounts,
                agentWorkload,
                recent,
                totalUsers,
                totalComments,
                totalClients,
                totalAgents,
                totalAdmins
        );

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/chatbot")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatbotResponse> askChatbot(@RequestBody ChatbotRequest request) {
        String question = request != null ? request.getQuestion() : null;
        if (question == null || question.isBlank()) {
            return ResponseEntity.ok(new ChatbotResponse("Posez une question sur vos réclamations ou sur le fonctionnement du site."));
        }

        List<Reclamation> reclamations = reclamationRepository.findAll();
        String answer = buildChatbotAnswer(question.trim(), reclamations);
        return ResponseEntity.ok(new ChatbotResponse(answer));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
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

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);

            document.add(new Paragraph("Export des réclamations", titleFont));
            document.add(new Paragraph("Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), textFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1, 3, 2, 2, 2, 2, 2});

            String[] headers = {"ID", "Titre", "Client", "Agent", "Catégorie", "Priorité", "Statut"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(BaseColor.DARK_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            for (Reclamation reclamation : reclamations.stream()
                    .sorted(Comparator.comparing(Reclamation::getId).reversed())
                    .collect(Collectors.toList())) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(reclamation.getId()), textFont)));
                table.addCell(new PdfPCell(new Phrase(reclamation.getTitre() != null ? reclamation.getTitre() : "", textFont)));
                table.addCell(new PdfPCell(new Phrase(reclamation.getClient() != null ? reclamation.getClient().getPrenom() + " " + reclamation.getClient().getNom() : "", textFont)));
                table.addCell(new PdfPCell(new Phrase(reclamation.getAgent() != null ? reclamation.getAgent().getPrenom() + " " + reclamation.getAgent().getNom() : "Non assigné", textFont)));
                table.addCell(new PdfPCell(new Phrase(reclamation.getCategorie() != null ? reclamation.getCategorie().getNom() : "", textFont)));
                table.addCell(new PdfPCell(new Phrase(reclamation.getPriorite() != null ? reclamation.getPriorite().name() : "", textFont)));
                table.addCell(new PdfPCell(new Phrase(reclamation.getStatut() != null ? reclamation.getStatut().name() : "", textFont)));
            }

            document.add(table);
            document.close();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reclamations-export.pdf")
                    .body(outputStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildChatbotAnswer(String question, List<Reclamation> reclamations) {
        String normalizedQuestion = stripAccents(question.toLowerCase(Locale.FRENCH));
        long total = reclamations.size();

        // 1. LOOKUP BY ID (#12, ID: 5, etc.)
        Matcher idMatcher = Pattern.compile("\\b(?:id|identifiant)\\s*[:#-]?\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE).matcher(question);
        if (idMatcher.find()) {
            long id = Long.parseLong(idMatcher.group(1));
            if (matchesAnyFuzzy(normalizedQuestion, "reclamation", "dossier", "claim")) {
                return reclamationRepository.findById(id)
                        .map(this::formatReclamationLookupAnswer)
                        .orElse("Aucune réclamation trouvée avec l’ID #" + id + ".");
            }

            if (matchesAnyFuzzy(normalizedQuestion, "utilisateur", "user", "client", "agent")) {
                return utilisateurRepository.findById(id)
                        .map(this::formatUserLookupAnswer)
                        .orElse("Aucun utilisateur trouvé avec l’ID #" + id + ".");
            }
        }

        // 2. LOOKUP BY PERSON NAME / EMAIL / USER REFERENCE
        String personReference = extractPersonReferenceFromQuestion(question);
        if (personReference != null) {
            List<Utilisateur> matchingUsers = findUsersByReference(personReference);
            if (matchingUsers.isEmpty()) {
                return "Je n’ai pas trouvé d’utilisateur correspondant à \"" + personReference + "\" dans la base de données.";
            }

            if (matchingUsers.size() > 1) {
                return "J’ai trouvé plusieurs correspondances pour \"" + personReference + "\" : "
                        + matchingUsers.stream()
                        .map(user -> user.getPrenom() + " " + user.getNom() + " (" + user.getRole() + ")")
                        .collect(Collectors.joining(", "))
                        + ". Merci de préciser le prénom ou l'adresse email.";
            }

            Utilisateur user = matchingUsers.get(0);
            List<Reclamation> userClaims;
            if (user.getRole() == Role.CLIENT) {
                userClaims = reclamations.stream()
                        .filter(r -> r.getClient() != null && r.getClient().getId().equals(user.getId()))
                        .sorted(Comparator.comparing(Reclamation::getId).reversed())
                        .collect(Collectors.toList());
            } else if (user.getRole() == Role.AGENT) {
                userClaims = reclamations.stream()
                        .filter(r -> r.getAgent() != null && r.getAgent().getId().equals(user.getId()))
                        .sorted(Comparator.comparing(Reclamation::getId).reversed())
                        .collect(Collectors.toList());
            } else {
                userClaims = reclamations;
            }

            if (userClaims.isEmpty()) {
                return "L'utilisateur " + user.getPrenom() + " " + user.getNom() + " (" + user.getRole() + ") n'a aucune réclamation associée.";
            }

            return formatUserClaimsSummary(user, userClaims);
        }

        // 3. GENERAL WEBSITE & ARCHITECTURE QUESTIONS (TYPO-TOLERANT)
        if (matchesAnyFuzzy(normalizedQuestion, "bct", "site", "plateforme", "application") && matchesAnyFuzzy(normalizedQuestion, "quoi", "qu'est-ce", "sert", "presentation", "bienvenue")) {
            return "🏦 **Plateforme de Gestion des Réclamations — Banque Centrale de Tunisie (BCT)**\n\n"
                    + "Cette application officielle permet aux clients de déposer et suivre leurs réclamations bancaires en temps réel. Les agents BCT enquêtent et mettent à jour le dossier, tandis que les administrateurs supervisent les équipes, gèrent les catégories et analysent les indicateurs de performance grâce au tableau de bord 3D.";
        }

        if (matchesAnyFuzzy(normalizedQuestion, "role", "roles", "droit", "droits", "permission", "permissions")) {
            return "👥 **Système de Rôles de la Plateforme BCT :**\n\n"
                    + "• **CLIENT** : Dépose de nouvelles réclamations, consulte ses dossiers et échange via les commentaires.\n"
                    + "• **AGENT** : Traite les réclamations assignées, met à jour les statuts (En cours, Résolue, Rejetée) et interagit avec le client.\n"
                    + "• **ADMIN** : Supervise l'ensemble de la banque, assigne les agents, gère les utilisateurs, modifie les catégories et consulte l'Assistant IA.";
        }

        if (matchesAnyFuzzy(normalizedQuestion, "deposer", "depose", "creer", "cree", "nouvelle", "formulaire") && matchesAnyFuzzy(normalizedQuestion, "reclamation", "reclamations", "dossier", "comment")) {
            return "📝 **Procédure pour déposer une Réclamation :**\n\n"
                    + "1. Accédez au menu **\"Déposer une réclamation\"** (ou cliquez sur le bouton **+**).\n"
                    + "2. Saisissez l'objet de votre demande.\n"
                    + "3. Sélectionnez la catégorie bancaire (*Virement, Carte, Compte, Crédit, etc.*).\n"
                    + "4. Définissez le niveau d'urgence estimé (*Faible, Moyenne, Élevée, Critique*).\n"
                    + "5. Rédigez la description détaillée et validez.";
        }

        if (matchesAnyFuzzy(normalizedQuestion, "statut", "statuts", "workflow", "etape", "etapes", "avancement")) {
            return "🔄 **Workflow des Statuts de Réclamation :**\n\n"
                    + "1. 🔵 **NOUVELLE** : Réclamation enregistrée, en attente de prise en charge par un agent BCT.\n"
                    + "2. 🟡 **EN_COURS** : Enquête bancaire en cours par l'agent assigné.\n"
                    + "3. 🟢 **RESOLUE** : Investigation terminée et solution transmise au client.\n"
                    + "4. 🔴 **REJETEE** : Dossier non recevable ou rejeté après examen approfondi.";
        }

        if (matchesAnyFuzzy(normalizedQuestion, "pdf", "export", "exporter", "imprimer", "telecharger")) {
            return "📄 **Export PDF des Données :**\n\n"
                    + "Vous pouvez générer un rapport PDF officiel à tout moment en cliquant sur le bouton **\"Exporter PDF\"** présent en haut du Tableau de bord ou de la liste des réclamations.";
        }

        // 4. UNASSIGNED CLAIMS (Non assignées / Sans agent)
        if (matchesAnyFuzzy(normalizedQuestion, "assigne", "assignee", "agent") && matchesAnyFuzzy(normalizedQuestion, "non", "sans", "pas", "attente")) {
            List<Reclamation> unassigned = reclamations.stream()
                    .filter(r -> r.getAgent() == null)
                    .collect(Collectors.toList());

            if (unassigned.isEmpty()) {
                return "✅ Excellente nouvelle : toutes les réclamations actuelles sont assignées à des agents BCT !";
            }

            StringBuilder reply = new StringBuilder();
            reply.append("⚠️ Il y a actuellement **").append(unassigned.size()).append(" réclamation(s) non assignée(s)** :\n");
            for (Reclamation r : unassigned.stream().limit(5).collect(Collectors.toList())) {
                reply.append("\n• Dossier #").append(r.getId()).append(" — ").append(r.getTitre())
                        .append(" (Statut: ").append(r.getStatut()).append(", Priorité: ").append(r.getPriorite()).append(")");
            }
            if (unassigned.size() > 5) {
                reply.append("\n\n... et ").append(unassigned.size() - 5).append(" autre(s) dossier(s).");
            }
            return reply.toString();
        }

        // 5. CRITICAL & URGENT CLAIMS
        if (matchesAnyFuzzy(normalizedQuestion, "critique", "urgent", "urgence", "fraude", "gravite", "elevee")) {
            List<Reclamation> criticalClaims = reclamations.stream()
                    .filter(r -> r.getPriorite() == Priorite.CRITIQUE || r.getPriorite() == Priorite.ELEVEE)
                    .sorted(Comparator.comparing(Reclamation::getId).reversed())
                    .collect(Collectors.toList());

            if (criticalClaims.isEmpty()) {
                return "🟢 Aucune réclamation à priorité Critique ou Élevée n'est signalée actuellement.";
            }

            StringBuilder reply = new StringBuilder();
            reply.append("🚨 Il y a **").append(criticalClaims.size()).append(" dossier(s) à priorité Élevée / Critique** :\n");
            for (Reclamation r : criticalClaims.stream().limit(5).collect(Collectors.toList())) {
                reply.append("\n• Dossier #").append(r.getId()).append(" [").append(r.getPriorite()).append("] — ").append(r.getTitre())
                        .append(" (Statut: ").append(r.getStatut()).append(")");
            }
            return reply.toString();
        }

        // 6. COMMENTS AND USERS AGGREGATIONS
        if (matchesAnyFuzzy(normalizedQuestion, "commentaire", "commentaires", "discussion", "discussions")) {
            long count = commentaireRepository.count();
            return "💬 Il y a **" + count + " commentaire(s)** enregistrés sur la plateforme, garantissant la transparence des échanges entre clients et agents BCT.";
        }

        if (matchesAnyFuzzy(normalizedQuestion, "utilisateur", "utilisateurs", "membre", "membres", "compte", "comptes")) {
            long totalU = utilisateurRepository.count();
            long clientsU = utilisateurRepository.findByRole(Role.CLIENT).size();
            long agentsU = utilisateurRepository.findByRole(Role.AGENT).size();
            long adminsU = utilisateurRepository.findByRole(Role.ADMIN).size();
            return "📊 **Statistiques des Utilisateurs BCT :**\n\n"
                    + "• Total Utilisateurs : **" + totalU + "**\n"
                    + "• Clients inscrits : **" + clientsU + "**\n"
                    + "• Agents BCT : **" + agentsU + "**\n"
                    + "• Administrateurs : **" + adminsU + "**";
        }

        // 7. STATS & CATEGORIES METRICS (TYPO-TOLERANT)
        Map<String, Long> statusCounts = reclamations.stream()
                .collect(Collectors.groupingBy(r -> r.getStatut().name(), Collectors.counting()));
        for (Statut s : Statut.values()) {
            statusCounts.putIfAbsent(s.name(), 0L);
        }

        Map<String, Long> categoryCounts = reclamations.stream()
                .filter(r -> r.getCategorie() != null)
                .collect(Collectors.groupingBy(r -> r.getCategorie().getNom(), Collectors.counting()));

        Map<String, Long> agentWorkload = reclamations.stream()
                .filter(r -> r.getAgent() != null)
                .collect(Collectors.groupingBy(r -> r.getAgent().getPrenom() + " " + r.getAgent().getNom(), Collectors.counting()));

        if (matchesAnyFuzzy(normalizedQuestion, "combien", "nombre", "total", "chiffre", "chiffres")) {
            if (matchesAnyFuzzy(normalizedQuestion, "nouvelle", "nouvelles", "neuf")) {
                return "🔵 Il y a **" + statusCounts.getOrDefault("NOUVELLE", 0L) + " réclamation(s) nouvelle(s)** en attente.";
            } else if (matchesAnyFuzzy(normalizedQuestion, "cours", "encours")) {
                return "🟡 Il y a **" + statusCounts.getOrDefault("EN_COURS", 0L) + " réclamation(s) en cours de traitement**.";
            } else if (matchesAnyFuzzy(normalizedQuestion, "resolu", "resolue", "resolues")) {
                return "🟢 Il y a **" + statusCounts.getOrDefault("RESOLUE", 0L) + " réclamation(s) résolue(s) avec succès**.";
            } else if (matchesAnyFuzzy(normalizedQuestion, "rejet", "rejete", "rejette", "refus")) {
                return "🔴 Il y a **" + statusCounts.getOrDefault("REJETEE", 0L) + " réclamation(s) rejetée(s)**.";
            } else {
                return "📊 La base contient un total de **" + total + " réclamation(s)** enregistrée(s).";
            }
        }

        if (matchesAnyFuzzy(normalizedQuestion, "categorie", "categories", "type", "types")) {
            if (categoryCounts.isEmpty()) {
                return "Aucune catégorie n'est actuellement liée aux réclamations.";
            }
            StringBuilder catReply = new StringBuilder("📁 **Répartition par Catégorie Bancaire :**\n\n");
            for (Map.Entry<String, Long> entry : categoryCounts.entrySet()) {
                catReply.append("• ").append(entry.getKey()).append(" : **").append(entry.getValue()).append(" dossier(s)**\n");
            }
            return catReply.toString();
        }

        if (matchesAnyFuzzy(normalizedQuestion, "agent", "agents", "charge", "travail", "occupe")) {
            if (agentWorkload.isEmpty()) {
                return "Aucun agent n'a encore été assigné à des réclamations.";
            }
            StringBuilder workReply = new StringBuilder("👨‍💼 **Charge de Travail des Agents BCT :**\n\n");
            for (Map.Entry<String, Long> entry : agentWorkload.entrySet()) {
                workReply.append("• ").append(entry.getKey()).append(" : **").append(entry.getValue()).append(" dossier(s)**\n");
            }
            return workReply.toString();
        }

        // DEFAULT COMPREHENSIVE ANSWER
        return "🤖 **Assistant IA BCT — Guide d'utilisation**\n\n"
                + "J'ai bien reçu votre message ! Je peux vous aider même en cas de petite faute de frappe. Posez-moi vos questions, par exemple :\n\n"
                + "1. 📊 **Données & Chiffres** : *\"Combien de réclamations en cours ?\"*, *\"Affichez la charge des agents\"*, *\"Dossiers non assignés\"*\n"
                + "2. 🔍 **Recherche par ID / Personne** : *\"Rechercher le dossier #2\"*, *\"Quelles sont les réclamations de Mohamed ?\"*\n"
                + "3. 📘 **Guide & Fonctionnement** : *\"Comment déposer une réclamation ?\"*, *\"Quels sont les différents rôles ?\"*, *\"Comment exporter en PDF ?\"*";
    }

    /* =========================================================================
       Fuzzy Matching Utilities & Typo Tolerance (Levenshtein Distance Engine)
       ========================================================================= */

    private boolean matchesAnyFuzzy(String normalizedText, String... targetKeywords) {
        if (normalizedText == null || normalizedText.isBlank()) return false;

        String[] tokens = normalizedText.split("[^a-z0-9]+");
        for (String target : targetKeywords) {
            String normTarget = stripAccents(target.toLowerCase(Locale.FRENCH));

            // Direct substring check
            if (normalizedText.contains(normTarget)) {
                return true;
            }

            // Token level Levenshtein fuzzy distance check
            for (String token : tokens) {
                if (token.isBlank()) continue;

                // Exact match
                if (token.equals(normTarget)) return true;

                // Prefix match if token is longer than 4 chars
                if (normTarget.length() >= 4 && (token.startsWith(normTarget) || normTarget.startsWith(token))) {
                    return true;
                }

                // Compute Levenshtein distance
                int maxAllowedDistance = normTarget.length() <= 4 ? 1 : 2;
                if (computeLevenshteinDistance(token, normTarget) <= maxAllowedDistance) {
                    return true;
                }
            }
        }
        return false;
    }

    private int computeLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(
                            dp[i - 1][j] + 1, // Deletion
                            Math.min(
                                    dp[i][j - 1] + 1, // Insertion
                                    dp[i - 1][j - 1] + cost // Substitution
                            )
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private String stripAccents(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    private String extractPersonReferenceFromQuestion(String question) {
        String[] keywords = {"utilisateur", "client", "agent", "user"};

        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile(keyword + "\\s+(?:de|du|d'|apres|après|avec|appelé|appele|nommé|nomme|:)?\\s*([a-zà-ÿA-ZÀ-Ÿ0-9' .-]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(question);
            if (matcher.find()) {
                String found = matcher.group(1).trim();
                found = found.replaceAll("[?.!]", "").trim();
                if (!found.isEmpty()) {
                    return found;
                }
            }
        }

        return null;
    }

    private List<Utilisateur> findUsersByReference(String reference) {
        String normalizedReference = normalizeText(reference);
        String[] tokens = normalizedReference.split("\\s+");

        return utilisateurRepository.findAll().stream()
                .filter(user -> {
                    String firstName = normalizeText(user.getPrenom());
                    String lastName = normalizeText(user.getNom());
                    String fullName = normalizeText(user.getPrenom() + " " + user.getNom());
                    String email = normalizeText(user.getEmail());

                    boolean nameMatch = false;
                    for (String token : tokens) {
                        if (token.isBlank()) {
                            continue;
                        }
                        if (firstName.equals(token) || lastName.equals(token) || fullName.contains(token)) {
                            nameMatch = true;
                            break;
                        }
                    }

                    return nameMatch
                            || fullName.equals(normalizedReference)
                            || fullName.contains(normalizedReference)
                            || normalizedReference.contains(fullName)
                            || email.equals(normalizedReference)
                            || email.contains(normalizedReference);
                })
                .collect(Collectors.toList());
    }

    private String normalizeText(String value) {
        return value == null ? "" : stripAccents(value.toLowerCase(Locale.FRENCH))
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String formatUserClaimsSummary(Utilisateur user, List<Reclamation> userClaims) {
        StringBuilder answer = new StringBuilder();
        answer.append("J’ai trouvé **")
                .append(userClaims.size())
                .append(" réclamation(s)** pour **")
                .append(user.getPrenom())
                .append(" ")
                .append(user.getNom())
                .append("** (")
                .append(user.getRole())
                .append(") :");

        for (Reclamation claim : userClaims) {
            answer.append("\n\n• **Réclamation #")
                    .append(claim.getId())
                    .append("** — ")
                    .append(claim.getTitre())
                    .append("\n  Statut : ").append(claim.getStatut())
                    .append(" | Priorité : ").append(claim.getPriorite());

            if (claim.getCategorie() != null) {
                answer.append(" | Catégorie : ").append(claim.getCategorie().getNom());
            }

            if (claim.getDescription() != null && !claim.getDescription().isBlank()) {
                answer.append("\n  Description : ").append(claim.getDescription());
            }

            if (claim.getDateCreation() != null) {
                answer.append("\n  Date : ").append(claim.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        }

        return answer.toString();
    }

    private String formatReclamationLookupAnswer(Reclamation reclamation) {
        StringBuilder details = new StringBuilder();
        details.append("Voici le dossier complet de la **Réclamation #")
                .append(reclamation.getId())
                .append("** :\n\n")
                .append("📌 **Titre** : ")
                .append(reclamation.getTitre())
                .append("\n🔄 **Statut** : ")
                .append(reclamation.getStatut())
                .append("\n⚡ **Priorité** : ")
                .append(reclamation.getPriorite());

        if (reclamation.getCategorie() != null) {
            details.append("\n📁 **Catégorie** : ").append(reclamation.getCategorie().getNom());
        }

        if (reclamation.getDescription() != null && !reclamation.getDescription().isBlank()) {
            details.append("\n📝 **Description** : ").append(reclamation.getDescription());
        }

        if (reclamation.getDateCreation() != null) {
            details.append("\n📅 **Date de création** : ").append(reclamation.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        if (reclamation.getClient() != null) {
            details.append("\n👤 **Client** : ")
                    .append(reclamation.getClient().getPrenom())
                    .append(" ")
                    .append(reclamation.getClient().getNom())
                    .append(" (")
                    .append(reclamation.getClient().getEmail())
                    .append(")");
        }

        if (reclamation.getAgent() != null) {
            details.append("\n👨‍💼 **Agent assigné** : ")
                    .append(reclamation.getAgent().getPrenom())
                    .append(" ")
                    .append(reclamation.getAgent().getNom());
        } else {
            details.append("\n👨‍💼 **Agent assigné** : *Non assigné*");
        }

        return details.toString();
    }

    private String formatUserLookupAnswer(Utilisateur utilisateur) {
        return "👤 **Utilisateur #" + utilisateur.getId() + "** :\n\n"
                + "• Nom complet : **" + utilisateur.getPrenom() + " " + utilisateur.getNom() + "**\n"
                + "• Email : **" + utilisateur.getEmail() + "**\n"
                + "• Rôle : **" + utilisateur.getRole() + "**";
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
