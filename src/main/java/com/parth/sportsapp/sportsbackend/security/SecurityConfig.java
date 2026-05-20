package com.parth.sportsapp.sportsbackend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
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
  private JwtAuthenticationFilter jwtAuthenticationFilter; // <--- Inject the filter

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 1. DISABLE CSRF (for REST APIs)
        .csrf(csrf -> csrf.disable())

        // 2. ENABLE CORS (allow requests from frontend)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // 3. CONFIGURE URL PERMISSIONS (ORDER MATTERS!)
        .authorizeHttpRequests(auth -> auth
            // --- NEW: Allow Swagger UI & API Docs ---
            .requestMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**"
            ).permitAll()

            // --- NEW: Allow Public Venue Search (GET only) ---
            // TODO REVERT these soAPI are not open on the web
            .requestMatchers(HttpMethod.GET, "/api/venues/search", "/api/venues/nearby").permitAll()

            // Public Auth endpoints
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/hello").permitAll()

            // ALL OTHER REQUESTS
            // currently allowing all for dev, but standard security is: .anyRequest().authenticated()
            .anyRequest().authenticated()
        )

        // 4. DISABLE form login and HTTP basic
        .formLogin(login -> login.disable())
        .httpBasic(basic -> basic.disable())

        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);



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

    // Allow credentials must be false when origin is "*"
    configuration.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
