package id.ac.ui.cs.advprog.papikos.notification.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.papikos.notification.dto.VerifyTokenResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List; // Import List

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticationFilter.class);

    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authVerifyUrl;

    @Value("${internal.token.secret}")
    String internalTokenSecret;

    private final ObjectMapper objectMapper;

    public TokenAuthenticationFilter(RestTemplate restTemplate,  ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String internalToken = request.getHeader("X-Internal-Token");

        if (internalToken != null) {
            if (!internalToken.equals(internalTokenSecret)) {
                logger.warn("Invalid internal token provided: {}", internalToken);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Authentication Failed: Invalid internal token.");
                return; // Stop filter chain
            } else {
                logger.info("Valid internal token provided for request URI: {}", request.getRequestURI());
                // Set a dummy authentication for internal requests
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "internal-service", // Principal
                        null,               // Credentials
                        Collections.singletonList(new SimpleGrantedAuthority("INTERNAL")) // Authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } else {
            String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7); // Remove "Bearer " prefix

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                try {
                    logger.info("Verifying token with auth server at URL: {}", authVerifyUrl);

                    ResponseEntity<String> verificationResponse = restTemplate.exchange(
                            authVerifyUrl + "/api/v1/verify",
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

                    VerifyTokenResponse verifyTokenResponse = objectMapper.readValue(
                            verificationResponse.getBody(),
                            VerifyTokenResponse.class
                    );

                    if (verificationResponse.getStatusCode().is2xxSuccessful() && verifyTokenResponse != null && verifyTokenResponse.data != null) {
                        logger.info("Token verified successfully for request URI: {}", request.getRequestURI());
                        logger.info("User ID from token: {}, Role from token: {}", verifyTokenResponse.data.userId, verifyTokenResponse.data.role);

                        String roleFromAuth = verifyTokenResponse.data.role;
                        List<SimpleGrantedAuthority> authorities;

                        if (roleFromAuth != null && !roleFromAuth.isEmpty()) {
                            authorities = Collections.singletonList(new SimpleGrantedAuthority(roleFromAuth));
                            logger.info("Setting authority for user {}", verifyTokenResponse.data.userId);
                        } else {
                            authorities = Collections.emptyList(); // Tidak ada role, tidak ada authority spesifik
                            logger.warn("Role from auth service is null or empty for user ID: {}. Assigning no specific authorities.", verifyTokenResponse.data.userId);
                        }

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                verifyTokenResponse.data.userId, // Principal (User ID)
                                null,                // Credentials (biasanya null untuk token-based auth)
                                authorities          // Authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        String responseBody = verificationResponse.getBody() != null ? verificationResponse.getBody() : "No response body";
                        logger.warn("Token verification failed with status: {}. Response: {}", verificationResponse.getStatusCode(), responseBody);
                        if (verifyTokenResponse == null || verifyTokenResponse.data == null) {
                            logger.warn("VerifyTokenResponse or its data field was null.");
                        }
                        SecurityContextHolder.clearContext();
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Authentication Failed: Token verification unsuccessful or invalid response structure");
                        return; // Stop filter chain
                    }
                } catch (HttpClientErrorException e) {
                    logger.warn("Client error during token verification: Status {}, Body {}", e.getStatusCode(), e.getResponseBodyAsString());
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Authentication Failed: Invalid token or authentication service error.");
                    return;
                } catch (RestClientException e) {
                    logger.error("Error connecting to authentication service: {}", e.getMessage(), e);
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("Authentication Failed: Could not connect to authentication service.");
                    return;
                } catch (IOException e) { // Lebih spesifik untuk objectMapper.readValue
                    logger.error("Error parsing token verification response: {}", e.getMessage(), e);
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("Authentication Failed: Error processing authentication service response.");
                    return;
                }
            } else {
                logger.debug("No Bearer token found in Authorization header for request URI: {}", request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response); // Continue filter chain
    }
}
