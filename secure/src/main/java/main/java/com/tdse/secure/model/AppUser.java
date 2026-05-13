package main.java.com.tdse.secure.model;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean verified = false;

    protected AppUser() {}

    public AppUser(String email, String passwordHash) {
        this.email        = email;
        this.passwordHash = passwordHash;
        this.verified     = false;
    }

    public Long getId()             { return id; }
    public String getEmail()        { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isVerified()     { return verified; }
    public void verify()            { this.verified = true; }
}