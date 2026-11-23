package com.quickmart.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JsonSecurityErrorHandler errors) throws Exception {
        http
                // CSRF attacks ride on cookies the browser sends automatically. We use an Authorization
                // header that JavaScript on another site can't attach, so CSRF tokens add nothing.
                // If tokens ever move into cookies, turn this back on.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // Spring's session-based /logout filter; ours is POST /api/auth/logout (revokes the refresh token).
                .logout(AbstractHttpConfigurer::disable)
                // No HttpSession, no JSESSIONID: every request proves itself with its token.
                // Any instance behind the load balancer can serve any request.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // First match wins, so specific rules go before broad ones.
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                        .requestMatchers("/health", "/health/**", "/error").permitAll()

                        // Admin read access to any customer's data lives under ONE prefix (AdminController).
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Role here, OWNERSHIP in the service (ProductService / StockService):
                        // the URL can't tell whether product 7 or store 3 belongs to this vendor.
                        .requestMatchers("/api/products/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/stores/*/inventory").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/stores/*/inventory/*").hasRole("VENDOR")

                        .requestMatchers("/api/categories/**", "/metrics/**").hasRole("ADMIN")
                        .requestMatchers("/api/payments/pay-list").hasRole("ADMIN")
                        // Customer data: the services filter every query by the token's customer id.
                        .requestMatchers("/api/orders/**", "/api/payments/**", "/api/addresses/**").hasRole("CUSTOMER")

                        // Default deny: a new endpoint is closed until someone opens it on purpose.
                        // The opposite (anyRequest().authenticated()) lets any logged-in user call it.
                        .anyRequest().denyAll())
                // Reads "Authorization: Bearer <jwt>", verifies it with our JwtDecoder (JwtConfig).
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors));
        return http.build();
    }

    // Token claim "role": "CUSTOMER" -> authority ROLE_CUSTOMER, which hasRole("CUSTOMER") checks.
    // The default converter reads "scope"/"scp" and would give every user zero roles.
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(JwtClaims.ROLE);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    // Strength 12 = 2^12 rounds, roughly 200-300 ms per hash on a laptop. Slow on purpose:
    // a leaked password table costs an attacker that much per guess. Login pays it once.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
