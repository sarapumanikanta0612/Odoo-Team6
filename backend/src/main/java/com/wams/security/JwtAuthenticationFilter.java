package com.wams.security;

import com.wams.entity.User;
import com.wams.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates bearer tokens against both their signature/expiry and the
 * current persisted User row. A token stops working immediately when its User
 * is disabled, deleted, renamed, or assigned a different role.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        // Preserve authentication installed by another trusted mechanism (for
        // example Spring Security's test support) rather than clearing it.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER.length());
        try {
            Claims claims = jwtService.parse(token);
            User user = currentUserFor(claims);
            if (user != null) {
                var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                var authentication = new UsernamePasswordAuthenticationToken(
                        user.getEmail(), null, List.of(authority));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException exception) {
            // Invalid/expired token: continue unauthenticated. Protected routes
            // are answered by RestSecurityErrorHandler; public routes stay public.
        }

        chain.doFilter(request, response);
    }

    /**
     * Match both immutable id and subject email. This prevents a deleted user's
     * old token from authenticating a later account that reuses the same email.
     */
    private User currentUserFor(Claims claims) {
        String email = claims.getSubject();
        Object rawUserId = claims.get("uid");
        if (email == null || email.isBlank() || !(rawUserId instanceof Number number)) {
            return null;
        }
        return userRepository.findById(number.longValue())
                .filter(User::isActive)
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .orElse(null);
    }
}
