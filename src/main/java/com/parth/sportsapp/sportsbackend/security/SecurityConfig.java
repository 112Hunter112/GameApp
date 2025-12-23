package com.parth.sportsapp.sportsbackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

 // this disables the intial log inscreenthat jwt prompts us with
 @Bean
 public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
   http
       .csrf(csrf -> csrf.disable())
       .authorizeHttpRequests(auth -> auth
           .requestMatchers("/hello").permitAll()
           .requestMatchers("/api/auth/**").permitAll() // <--- CHANGE THIS (add /api)
           .anyRequest().authenticated()
       )
       .formLogin(login -> login.disable())
       .httpBasic(basic -> basic.disable());

   return http.build();
 }
}
