package co.edu.unbosque.marketplace.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MarketplaceUserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, MarketplaceUserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            UUID userId = jwtService.subject(authorization.substring("Bearer ".length()).trim());
            if (userId != null) {
                userRepository.findById(userId).filter(MarketplaceUser::isEnabled)
                        .filter(MarketplaceUser::isAccountNonLocked)
                        .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())));
            }
        }
        filterChain.doFilter(request, response);
    }
}
