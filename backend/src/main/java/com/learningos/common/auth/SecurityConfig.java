package com.learningos.common.auth;

import com.learningos.config.AuthProperties;
import com.learningos.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AppProperties appProperties,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health/**", "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().access((authentication, context) -> {
                            String environment = appProperties.environment();
                            boolean productionLike = "prod".equalsIgnoreCase(environment)
                                    || "production".equalsIgnoreCase(environment)
                                    || "staging".equalsIgnoreCase(environment);
                            if (!productionLike) {
                                return new org.springframework.security.authorization.AuthorizationDecision(true);
                            }
                            Authentication currentAuthentication = authentication.get();
                            return new org.springframework.security.authorization.AuthorizationDecision(
                                    currentAuthentication instanceof JwtAuthenticationToken
                                            && currentAuthentication.isAuthenticated()
                            );
                        })
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties authProperties, AppProperties appProperties) {
        NimbusJwtDecoder decoder;
        if (!authProperties.jwkSetUri().isBlank()) {
            decoder = NimbusJwtDecoder.withJwkSetUri(authProperties.jwkSetUri()).build();
        } else {
            byte[] secret = hmacSecret(authProperties, appProperties);
            decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret, "HmacSHA256")).build();
        }
        decoder.setJwtValidator(jwtValidator(authProperties));
        return decoder;
    }

    private byte[] hmacSecret(AuthProperties authProperties, AppProperties appProperties) {
        if (authProperties.jwtSecret().isBlank()) {
            if (isProductionLike(appProperties.environment())) {
                throw new IllegalStateException("AUTH_JWT_SECRET or AUTH_JWK_SET_URI is required in production-like environments");
            }
            return "local-development-only-secret-32b".getBytes(StandardCharsets.UTF_8);
        }
        byte[] secret = authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("AUTH_JWT_SECRET must be at least 32 bytes for HS256");
        }
        return secret;
    }

    private OAuth2TokenValidator<Jwt> jwtValidator(AuthProperties authProperties) {
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(authProperties.issuer());
        if (authProperties.audience().isBlank()) {
            return issuerValidator;
        }
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (jwt.getAudience().contains(authProperties.audience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "The required audience is missing",
                    null
            ));
        };
        return new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);
    }

    private boolean isProductionLike(String environment) {
        return "prod".equalsIgnoreCase(environment)
                || "production".equalsIgnoreCase(environment)
                || "staging".equalsIgnoreCase(environment);
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        authenticationConverter.setPrincipalClaimName("sub");
        return authenticationConverter;
    }
}
