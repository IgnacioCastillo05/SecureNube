package main.java.com.tdse.secure.controller;


import main.java.com.tdse.secure.service.AuthService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/register  — público, no requiere autenticación
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> payload) {
        String email    = payload.getOrDefault("email", "").trim();
        String password = payload.getOrDefault("password", "");

        if (email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Email y contraseña son requeridos"));
        }

        try {
            authService.register(email, password);
            return ResponseEntity.ok(Map.of("status", "registered"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/auth/login  — requiere HTTP Basic (Spring valida las credenciales)
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
            "status", "authenticated",
            "user", authentication.getName()
        ));
    }
}
