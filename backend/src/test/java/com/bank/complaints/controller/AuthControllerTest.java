package com.bank.complaints.controller;

import com.bank.complaints.dto.SignupRequest;
import com.bank.complaints.model.Role;
import com.bank.complaints.model.Utilisateur;
import com.bank.complaints.repository.UtilisateurRepository;
import com.bank.complaints.security.JwtUtils;
import com.bank.complaints.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerUserShouldSaveUserAndSendVerificationEmail() {
        SignupRequest request = new SignupRequest();
        request.setNom("Doe");
        request.setPrenom("Jane");
        request.setEmail("jane@example.com");
        request.setMotDePasse("123456");
        request.setTelephone("0123456789");
        request.setRole(Role.CLIENT);

        when(utilisateurRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(encoder.encode("123456")).thenReturn("encoded-password");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() != null);
        assertTrue(response.getBody().toString().contains("code"));
        verify(emailService).sendVerificationEmail(any(Utilisateur.class), any(String.class));
    }
}
