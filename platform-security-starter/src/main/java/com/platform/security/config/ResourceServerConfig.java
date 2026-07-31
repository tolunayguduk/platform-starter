package com.platform.security.config;

import com.platform.security.util.KeycloakRoleMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Single stateless resource-server chain: the React SPA (a separate origin) authenticates via
 * /api/auth/login (KeycloakTokenClient's ROPC grant, see AuthController) and calls every other
 * endpoint with Authorization: Bearer <access_token>. No session, no cookies, no CSRF - there is
 * no browser-hosted login page left to protect against forgery.
 *
 * issuer-uri (spring.security.oauth2.resourceserver.jwt.issuer-uri) is the ONLY thing that
 * changes when switching from a plain Keycloak realm to any other OIDC provider - nothing here
 * is Keycloak-specific except the "realm_access.roles" claim extraction below.
 */
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable()) // stateless token auth, no session to forge
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter())));
        return http.build();
    }

    /**
     * Allowed origins are config-driven (platform.web.allowed-origins) so the React dev server
     * port, a deployed frontend domain, etc. never require touching this class.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${platform.web.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Keycloak puts roles under realm_access.roles rather than the standard "scope" claim -
     * this converter maps them to Spring GrantedAuthority so the rest of Spring Security
     * (and MatrixPermissionEvaluator) works unmodified.
     */
    private JwtAuthenticationConverter keycloakJwtConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<org.springframework.security.core.GrantedAuthority> authorities = new ArrayList<>(defaultConverter.convert(jwt));
            authorities.addAll(KeycloakRoleMapper.realmRoleAuthorities(jwt));
            return authorities;
        });
        return converter;
    }
}
