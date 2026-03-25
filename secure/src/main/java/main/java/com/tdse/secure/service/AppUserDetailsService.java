package main.java.com.tdse.secure.service;


import main.java.com.tdse.secure.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repo;

    public AppUserDetailsService(AppUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repo.findByEmail(email)
            .map(u -> User.withUsername(u.getEmail())
                .password(u.getPasswordHash())
                .roles("USER")
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }
}
