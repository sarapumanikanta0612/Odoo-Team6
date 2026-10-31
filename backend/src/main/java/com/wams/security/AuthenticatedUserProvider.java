package com.wams.security;

import com.wams.entity.User;
import com.wams.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the authenticated {@link User} from the Spring Security context.
 *
 * <p>The authenticated principal name is populated by {@link JwtAuthenticationFilter}
 * from the JWT {@code sub} claim (the user's email). This provider is the single
 * trusted place that maps that email back to a persisted User row — callers must
 * never rely on request parameters or request body fields to determine identity.
 *
 * <p>Any state where there is no authentication, the authentication is anonymous,
 * or the backing User row cannot be found results in {@link AccessDeniedException}
 * so the caller is treated identically to a logged-out request.
 */
@Component
public class AuthenticatedUserProvider {

    private final UserRepository userRepository;

    public AuthenticatedUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @return the current authenticated and active user
     * @throws AccessDeniedException if the request is unauthenticated, anonymous,
     *                               or the backing user has been deleted or
     *                               deactivated
     */
    @Transactional(readOnly = true)
    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("No authenticated user");
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            throw new AccessDeniedException("No authenticated user");
        }
        User user = userRepository.findByEmailIgnoreCase(name)
                .orElseThrow(() -> new AccessDeniedException("Authenticated principal is not a known user"));
        if (!user.isActive()) {
            throw new AccessDeniedException("User account is disabled");
        }
        return user;
    }
}
