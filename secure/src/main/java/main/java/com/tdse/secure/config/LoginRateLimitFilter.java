package main.java.com.tdse.secure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, Deque<Long>> attemptsByIp = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // En HTTP Basic, el "login" ocurre cuando intentan acceder a un endpoint protegido.
        // Limitamos /api/hello para mitigar fuerza bruta.
        return !(request.getRequestURI().equals("/api/hello")
                && request.getMethod().equalsIgnoreCase("GET"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        long now = Instant.now().getEpochSecond();

        Deque<Long> q = attemptsByIp.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        // Limpia intentos fuera de la ventana
        while (!q.isEmpty() && (now - q.peekFirst()) > WINDOW_SECONDS) {
            q.pollFirst();
        }

        // Si excede límite, bloquear
        if (q.size() >= MAX_ATTEMPTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many authentication attempts. Try again later.\"}");
            return;
        }

        // Registrar intento (cada request a /api/hello cuenta como intento)
        q.addLast(now);

        filterChain.doFilter(request, response);
    }
}