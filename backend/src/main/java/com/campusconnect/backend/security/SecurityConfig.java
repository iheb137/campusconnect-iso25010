package com.campusconnect.backend.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.campusconnect.backend.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private static final AuthenticationEntryPoint ENTRY_POINT = (request, response, ex) ->
            writeError(response, HttpStatus.UNAUTHORIZED, "Authentification requise");

    private static final AccessDeniedHandler ACCESS_DENIED_HANDLER = (request, response, ex) ->
            writeError(response, HttpStatus.FORBIDDEN, "Accès refusé");

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/health", "/error").permitAll()
                        // Modération : modérateurs et administrateurs
                        .requestMatchers("/api/admin/reports", "/api/admin/requests/**").hasAnyRole("MODERATOR", "ADMIN")
                        // Administration (comptes, matières, audit, statistiques) : administrateurs seulement
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/requests").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/requests/*/offers").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/requests/*/reports").hasRole("STUDENT")
                        .requestMatchers("/api/dashboard").hasRole("STUDENT")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(ENTRY_POINT)
                        .accessDeniedHandler(ACCESS_DENIED_HANDLER))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(ENTRY_POINT)
                        .accessDeniedHandler(ACCESS_DENIED_HANDLER))
                .addFilterAfter(new DisabledAccountFilter(userRepository), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message
                + "\",\"fieldErrors\":{}}");
    }
}