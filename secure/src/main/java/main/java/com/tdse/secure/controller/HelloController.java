package main.java.com.tdse.secure.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {

    // GET /api/hello  — requiere autenticación
    @GetMapping("/hello")
    public Map<String, String> hello(Authentication authentication) {
        return Map.of(
            "message", "Hola desde el API seguro de Spring!",
            "user", authentication.getName()
        );
    }
}


//Abrir en: http://localhost:4567/hello
