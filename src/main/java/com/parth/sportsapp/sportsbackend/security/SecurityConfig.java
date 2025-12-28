package com.parth.sportsapp.sportsbackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 1. DISABLE CSRF (for REST APIs)
        .csrf(csrf -> csrf.disable())

        // 2. ENABLE CORS (allow requests from frontend)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // 3. CONFIGURE URL PERMISSIONS (ORDER MATTERS!)
        .authorizeHttpRequests(auth -> auth
            // Public endpoints (no authentication needed)
            .requestMatchers("/api/auth/**").permitAll()    // Login, Register, Verify
            .requestMatchers("/hello").permitAll()          // Test endpoint

            // TEMPORARILY allow everything for development
            .anyRequest().permitAll()  // CHANGE THIS LATER to .authenticated()
        )

        // 4. DISABLE form login and HTTP basic (not needed for REST APIs)
        .formLogin(login -> login.disable())
        .httpBasic(basic -> basic.disable());

    return http.build();
  }

  // CORS Configuration (allows frontend to call backend)
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allow requests from anywhere (FOR DEVELOPMENT ONLY!)
    configuration.setAllowedOrigins(Arrays.asList("*"));

    // Allow all HTTP methods
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    // Allow all headers
    configuration.setAllowedHeaders(Arrays.asList("*"));

    // Allow credentials (cookies, authorization headers)
    configuration.setAllowCredentials(false); // Must be false when origin is "*"

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
