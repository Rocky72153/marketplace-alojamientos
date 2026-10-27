package co.edu.unbosque.marketplace.security;

import co.edu.unbosque.marketplace.api.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Bean
    UserDetailsService userDetailsService(MarketplaceUserRepository users) {
        return email -> users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            ObjectMapper objectMapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                response, objectMapper, 401, "UNAUTHORIZED", "Sesión ausente o inválida.", request.getHeader(CORRELATION_ID_HEADER)))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                response, objectMapper, 403, "FORBIDDEN", "No tienes permiso para realizar esta operación.", request.getHeader(CORRELATION_ID_HEADER))))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/cities").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/listings/*/quotes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings", "/api/listings/*", "/api/listings/*/availability").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4173,http://127.0.0.1:4173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", CORRELATION_ID_HEADER,
                "X-Admin-Bootstrap", "Idempotency-Key"));
        configuration.setExposedHeaders(Arrays.asList(CORRELATION_ID_HEADER));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, ObjectMapper objectMapper,
            int status, String code, String message, String requestedCorrelationId) throws IOException {
        String correlationId = requestedCorrelationId == null || requestedCorrelationId.isBlank()
                ? UUID.randomUUID().toString() : requestedCorrelationId;
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiError(code, message, correlationId));
    }
}
