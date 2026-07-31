package com.platform.security.config;

import com.platform.security.MatrixPermissionEvaluator;
import com.platform.security.RolePermissionLookupPort;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.KeycloakAdminClientImpl;
import com.platform.security.integration.keycloak.KeycloakTokenClient;
import com.platform.security.integration.keycloak.KeycloakTokenClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@EnableMethodSecurity
@Import(ResourceServerConfig.class)
public class SecurityAutoConfiguration {

    /**
     * Only wired once the consuming module (module-user) exposes a RolePermissionLookupPort bean.
     * If it's absent, hasPermission(...) expressions simply have no evaluator - fails loud in tests
     * rather than silently allowing everything.
     */
    @Bean
    @ConditionalOnBean(RolePermissionLookupPort.class)
    @ConditionalOnMissingBean(MethodSecurityExpressionHandler.class)
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RolePermissionLookupPort lookupPort) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(new MatrixPermissionEvaluator(lookupPort));
        return handler;
    }

    /**
     * Client-credentials grant support for service-account calls (e.g. KeycloakAdminClient) -
     * distinct from the request-bound managers Spring Boot autoconfigures for oauth2Login.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        return manager;
    }

    @Bean
    public KeycloakAdminClient keycloakAdminClient(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${platform.keycloak.admin-base-uri}") String adminBaseUri) {
        return new KeycloakAdminClientImpl(restClientBuilder, authorizedClientManager, adminBaseUri);
    }

    @Bean
    public KeycloakTokenClient keycloakTokenClient(RestClient.Builder restClientBuilder,
                                                     ClientRegistrationRepository clientRegistrationRepository) {
        return new KeycloakTokenClientImpl(restClientBuilder, clientRegistrationRepository);
    }
}
