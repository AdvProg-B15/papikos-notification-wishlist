package id.ac.ui.cs.advprog.papikos.notification.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String DECODE_URL = "http://localhost:8000/api/token/decode/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Send POST to Django decode endpoint
            HttpURLConnection conn = (HttpURLConnection) new URL(DECODE_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = "{\"token\": \"" + token + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                String result;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    result = br.lines().collect(Collectors.joining());
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode payload = mapper.readTree(result);

                String userId = payload.has("user_id") ? payload.get("user_id").asText() : null;
                String role = payload.has("role") ? payload.get("role").asText() : null;

                if (userId == null || role == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing user_id or role in token");
                    return;
                }

                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
                );

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // DEBUG: Print assigned authorities
                System.out.println("Authenticated user: " + userId);
                System.out.println("Granted authorities: " + authorities);

            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token from decode endpoint");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token verification failed: " + e.getMessage());
            return;
        }

        chain.doFilter(request, response);
    }
}
