package com.parth.sportsapp.sportsbackend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
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
public class SecurityConfig {

  /**
   * Comma-separated allowed origins from env (e.g. "https://app.example.com,https://staging.example.com").
   * Defaults to typical local dev origins. NEVER deploy with the default in prod.
   */
  @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8081,http://localhost:19006}")
  private String allowedOriginsRaw;

  @Bean
  public PasswordEncoder passwordEncoder() {
    // BCrypt strength 12 = ~250ms/hash on a modern server. Slows down brute force.
    return new BCryptPasswordEncoder(12);
  }

  // Prevents Spring Boot from auto-creating an inMemoryUserDetailsManager
  // and printing a generated security password on startup.
  @Bean
  public UserDetailsService userDetailsService() {
    return username -> {
      throw new org.springframework.security.core.userdetails.UsernameNotFoundException("JWT-only app");
    };
  }

  @Autowired
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CSRF: REST + JWT does not need CSRF tokens (no browser-session cookies trusted for auth).
        .csrf(csrf -> csrf.disable())

        // CORS handled centrally below.
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // No sessions — every request is authenticated solely by its JWT.
        // Closes the door on session fixation and cookie-based replay.
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Security response headers (frame-deny prevents clickjacking, etc.).
        // ASVS V14.4 — HTTP security response headers (defense in depth).
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny())            // clickjacking: no framing
            .contentTypeOptions(ct -> {})                   // X-Content-Type-Options: nosniff
            .xssProtection(xss -> {})                       // legacy XSS filter header
            .cacheControl(cc -> {})                         // no-store on authenticated responses
            // HSTS: force HTTPS for a year. Only takes effect once served over TLS,
            // so it's harmless over plain HTTP in local dev.
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31_536_000))
            // CSP for a JSON API: block framing, plugins, and base-tag hijacking.
            // We intentionally omit default-src 'self' so the Swagger UI dev tool
            // (which uses inline assets) keeps working; the API returns JSON that
            // browsers never execute as a page anyway.
            .contentSecurityPolicy(csp -> csp.policyDirectives(
                "frame-ancestors 'none'; object-src 'none'; base-uri 'none'"))
            // Don't leak the requested URL to third parties via the Referer header.
            .referrerPolicy(ref -> ref.policy(
                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
            // Disable powerful browser features the API never uses.
            .permissionsPolicyHeader(pp -> pp.policy(
                "geolocation=(), camera=(), microphone=(), payment=(), usb=()"))
        )

        .authorizeHttpRequests(auth -> auth
            // Swagger / OpenAPI docs
            .requestMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**"
            ).permitAll()

            // Actuator health is public for load balancers; everything else under /actuator stays auth'd.
            .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

            // Public discovery endpoints
            .requestMatchers(HttpMethod.GET, "/api/venues/search", "/api/venues/nearby",
                "/api/venues/discover").permitAll()

            // Public booking discovery: availability grid + venue booking rules,
            // so players can browse times before signing in. Writing requires auth.
            .requestMatchers(HttpMethod.GET, "/api/courts/*/availability").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/venues/*/booking-policy").permitAll()

            // Public auth endpoints (login, register, verify)
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/hello").permitAll()

            .anyRequest().authenticated()
        )

        .formLogin(login -> login.disable())
        .httpBasic(basic -> basic.disable())

        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();

    // Reject wildcard origins outright. If someone sets allowed-origins=* we still refuse.
    if (origins.contains("*")) {
      throw new IllegalStateException(
          "app.cors.allowed-origins must not be '*'. Set explicit origins.");
    }

    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
