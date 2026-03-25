package main.java.com.tdse.secure.service;


import main.java.com.tdse.secure.model.AppUser;
import main.java.com.tdse.secure.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository repo;
    private final PasswordEncoder encoder;

    public AuthService(AppUserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public void register(String email, String password) {
        if (repo.existsByEmail(email)) {
            throw new IllegalStateException("El usuario ya existe");
        }
        // La contraseña se guarda hasheada con BCrypt, nunca en texto plano
        String hash = encoder.encode(password);
        repo.save(new AppUser(email, hash));
    }
}
