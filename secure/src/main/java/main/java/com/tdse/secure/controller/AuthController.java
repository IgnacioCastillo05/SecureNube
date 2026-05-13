package main.java.com.tdse.secure.controller;

import main.java.com.tdse.secure.service.AuthService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> payload) {
        String email    = payload.getOrDefault("email", "").trim();
        String password = payload.getOrDefault("password", "");

        if (email.isBlank() || password.isBlank()) {
            log.warn("Intento de registro con campos vacíos");
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Email y contraseña son requeridos"));
        }

        try {
            authService.register(email, password);
            return ResponseEntity.ok(Map.of(
                "status",  "registered",
                "message", "Cuenta creada. Revisa tu email para verificarla."
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(Map.of(
                "status",  "verified",
                "message", "Cuenta verificada. Ya puedes iniciar sesión."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(Authentication authentication) {
        log.info("Login exitoso: {}", authentication.getName());
        return ResponseEntity.ok(Map.of(
            "status", "authenticated",
            "user",   authentication.getName()
        ));
    }
}