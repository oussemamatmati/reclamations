package com.bank.complaints.controller;

import com.bank.complaints.dto.JwtResponse;
import com.bank.complaints.dto.LoginRequest;
import com.bank.complaints.dto.SignupRequest;
import com.bank.complaints.model.Role;
import com.bank.complaints.model.Utilisateur;
import com.bank.complaints.repository.UtilisateurRepository;
import com.bank.complaints.security.JwtUtils;
import com.bank.complaints.security.UserDetailsImpl;
import com.bank.complaints.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UtilisateurRepository utilisateurRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Utilisateur user = utilisateurRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.isEmailVerified()) {
            return ResponseEntity.status(403).body("Veuillez vérifier votre adresse email avant de vous connecter.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getMotDePasse()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();        
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt, 
                                                 userDetails.getId(), 
                                                 userDetails.getUsername(), 
                                                 userDetails.getNom(), 
                                                 userDetails.getPrenom(), 
                                                 roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (utilisateurRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Erreur: Cet email est déjà utilisé !");
        }

        // Create new user's account (Defaulting to CLIENT role if not specified)
        Role userRole = signUpRequest.getRole() != null ? signUpRequest.getRole() : Role.CLIENT;
        
        Utilisateur user = new Utilisateur(
                signUpRequest.getNom(),
                signUpRequest.getPrenom(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getMotDePasse()),
                signUpRequest.getTelephone(),
                userRole
        );

        String verificationCode = String.format("%06d", (int)(Math.random() * 1000000));
        user.setVerificationCode(verificationCode);
        Utilisateur savedUser = utilisateurRepository.save(user);

        emailService.sendVerificationEmail(savedUser, verificationCode);

        return ResponseEntity.ok("Utilisateur inscrit avec succès ! Un email de vérification contenant un code a été envoyé.");
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String code) {
        return utilisateurRepository.findByVerificationCode(code)
                .map(user -> {
                    user.setEmailVerified(true);
                    user.setVerificationCode(null);
                    utilisateurRepository.save(user);
                    return ResponseEntity.ok("Votre adresse email a été vérifiée avec succès.");
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Code de vérification invalide ou expiré."));
    }
}
