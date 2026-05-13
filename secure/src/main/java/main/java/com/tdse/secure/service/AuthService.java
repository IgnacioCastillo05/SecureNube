package main.java.com.tdse.secure.service;

import main.java.com.tdse.secure.model.AppUser;
import main.java.com.tdse.secure.model.VerificationToken;
import main.java.com.tdse.secure.repository.AppUserRepository;
import main.java.com.tdse.secure.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern SPECIAL   = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    private final AppUserRepository repo;
    private final VerificationTokenRepository tokenRepo;
    private final PasswordEncoder encoder;

    public AuthService(AppUserRepository repo,
                       VerificationTokenRepository tokenRepo,
                       PasswordEncoder encoder) {
        this.repo      = repo;
        this.tokenRepo = tokenRepo;
        this.encoder   = encoder;
    }

    public String register(String email, String password) {
        validatePassword(password);

        if (repo.existsByEmail(email)) {
            log.warn("Intento de registro con email ya existente: {}", email);
            throw new IllegalStateException("El usuario ya existe");
        }

        AppUser user = repo.save(new AppUser(email, encoder.encode(password)));

        String token = UUID.randomUUID().toString();
        tokenRepo.save(new VerificationToken(token, user, LocalDateTime.now().plusHours(24)));

        log.info("Usuario registrado, token de verificación generado para: {}", email);
        return token;
    }

    public void verifyEmail(String token) {
        VerificationToken vt = tokenRepo.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Token de verificación inválido"));

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepo.delete(vt);
            throw new IllegalArgumentException("El token de verificación ha expirado");
        }

        AppUser user = vt.getUser();
        user.verify();
        repo.save(user);
        tokenRepo.delete(vt);
        log.info("Email verificado exitosamente: {}", user.getEmail());
    }

    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        if (!UPPERCASE.matcher(password).matches()) {
            throw new IllegalArgumentException("La contraseña debe tener al menos una letra mayúscula");
        }
        if (!SPECIAL.matcher(password).matches()) {
            throw new IllegalArgumentException("La contraseña debe tener al menos un carácter especial (!@#$%...)");
        }
    }
}