package main.java.com.tdse.secure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final int MAX_ATTEMPTS    = 5;
    private static final long WINDOW_SECONDS = 60;

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
        "/api/hello",
        "/api/auth/login",
        "/api/auth/register"
    );

    private final Map<String, Deque<Long>> attemptsByIp = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !RATE_LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        long now  = Instant.now().getEpochSecond();

        Deque<Long> q = attemptsByIp.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        while (!q.isEmpty() && (now - q.peekFirst()) > WINDOW_SECONDS) {
            q.pollFirst();
        }

        if (q.size() >= MAX_ATTEMPTS) {
            log.warn("Rate limit excedido para IP {} en {}", ip, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Demasiados intentos. Espera un momento.\"}");
            return;
        }

        q.addLast(now);
        filterChain.doFilter(request, response);
    }
}