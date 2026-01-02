package com.parth.sportsapp.sportsbackend.security;

import com.parth.sportsapp.sportsbackend.service.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Autowired
  private JwtUtil jwtUtil;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");

    // 1. Check if token exists
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      final String jwt = authHeader.substring(7);
      final String userEmail = jwtUtil.extractEmail(jwt);
      final String role = jwtUtil.extractRole(jwt); // <--- Extracts "ROLE_VENUE_OWNER"

      // 2. If user is present and not already authenticated
      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

        // 3. Validate Token
        if (jwtUtil.validateToken(jwt, userEmail)) {

          // 4. Create Authority (Role)
          SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);

          // 5. Create UserDetails (Spring Internal User)
          UserDetails userDetails = new User(userEmail, "", Collections.singletonList(authority));

          // 6. Set Authentication
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
              userDetails, null, userDetails.getAuthorities());

          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          // 7. Success! User is logged in for this request
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception e) {
      // Token invalid or expired
      System.out.println("JWT Verification Failed: " + e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}
