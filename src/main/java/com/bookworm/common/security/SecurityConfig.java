package com.bookworm.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless REST API
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Exception handling for 401 & 403
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // Role Based Access Control (RBAC) URL authorization rules
                .authorizeHttpRequests(auth -> auth
                        // 1. Public Authentication & Documentation Endpoints
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // 2. Public Catalog Browsing & Search Endpoints
                        .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/authors/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/publishers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/recommendations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/shipping/edd").permitAll()

                        // 3. Guest & Customer Shopping Cart Endpoints (Supports X-Session-Token header)
                        .requestMatchers("/cart", "/cart/items/**").permitAll()

                        // 4. Customer Protected Endpoints (Requires ROLE_CUSTOMER, ROLE_MEMBER, or ROLE_ADMIN)
                        .requestMatchers("/users/**").hasAnyRole("CUSTOMER", "MEMBER", "ADMIN")
                        .requestMatchers("/wishlist/**").hasAnyRole("CUSTOMER", "MEMBER", "ADMIN")
                        .requestMatchers("/orders/**").hasAnyRole("CUSTOMER", "MEMBER", "ADMIN")
                        .requestMatchers("/payments/**").hasAnyRole("CUSTOMER", "MEMBER", "ADMIN")
                        .requestMatchers("/coupons/**").hasAnyRole("CUSTOMER", "MEMBER", "ADMIN")
                        .requestMatchers("/loyalty/**").hasAnyRole("CUSTOMER", "MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/books/*/reviews").hasAnyRole("CUSTOMER", "MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/cart/merge").hasAnyRole("CUSTOMER", "MEMBER", "ADMIN")
                        .requestMatchers("/shipping/track/**").hasAnyRole("CUSTOMER", "MEMBER", "ADMIN")

                        // 5. Administrator Protected Endpoints (Requires ROLE_ADMIN or specific Admin roles)
                        .requestMatchers("/admin/**", "/api/v1/admin/**").hasAnyRole("ADMIN", "STORE_ADMIN", "CATALOG_ADMIN")

                        // 6. Any other request must be authenticated
                        .anyRequest().authenticated()
                )

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080", "https://bookworm.example.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Session-Token", "Idempotency-Key", "X-User-Id"));
        configuration.setExposedHeaders(List.of("Authorization", "Idempotency-Key", "X-Session-Token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
