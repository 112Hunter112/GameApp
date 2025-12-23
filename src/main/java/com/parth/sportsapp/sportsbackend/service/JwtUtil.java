package com.parth.sportsapp.sportsbackend.service;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.Key;
import java.util.*;
import java.security.SecureRandom;
import java.util.function.Function;

// use this to create the JWT token after the sign in
@Service
public class JwtUtil {


  @Value("${jwt.secret}")
  private String SECRET_KEY;

  @Value("${jwt.expiration}")
  private long EXPIRATION_TIME;

  public String generateToken(String email) {
    Map<String, Object> claims = new HashMap<String, Object>(); // this is to store extra info,
    // such as postion of user and other metadata, so we don't retrieve from db
    return createToken(claims, email);
  }

  public String createToken(Map<String, Object> claims, String subject) {

    return Jwts.builder()
        .setClaims(claims).setSubject(subject)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis()+ EXPIRATION_TIME))
        .signWith(getSigningKey(),SignatureAlgorithm.HS256) // 256-bit encryption
        .compact();
  }

  // I need to turn the secret key using hashing and other tools
  public Key getSigningKey() {

    //convert SECRET_KEY to bytes and also hash it
    byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  // 9. Validate token (check email matches and not expired)
  public Boolean validateToken(String token, String email) {
    final String tokenEmail = extractEmail(token);
    return (tokenEmail.equals(email) && !isTokenExpired(token));
  }

}
