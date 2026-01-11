package com.parth.sportsapp.sportsbackend.security;

import com.parth.sportsapp.sportsbackend.service.JwtUtil;
// 1. CHANGE THIS IMPORT to point to your custom model
import com.parth.sportsapp.sportsbackend.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
// 2. REMOVE this import so it doesn't conflict
// import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Autowired
  private JwtUtil jwtUtil;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      final String jwt = authHeader.substring(7);
      final String userEmail = jwtUtil.extractEmail(jwt);
      final String role = jwtUtil.extractRole(jwt);

      // 3. Extract the User ID from the token
      final String userIdString = jwtUtil.extractUserId(jwt);

      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

        if (jwtUtil.validateToken(jwt, userEmail)) {

          SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);

          // 4. Create YOUR Custom User Object
          User customUser = new User();
          customUser.setId(UUID.fromString(userIdString)); // Set the ID so controller can use it
          customUser.setEmail(userEmail);

          // Note: If your User model implements UserDetails, you might need to set password/authorities here too,
          // but for this specific controller check, setting the ID is the most important part.

          // 5. Pass 'customUser' as the Principal
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
              customUser, // <--- This is now an instance of com.parth...model.User
              null,
              Collections.singletonList(authority));

          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception e) {
      System.out.println("JWT Verification Failed: " + e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}
