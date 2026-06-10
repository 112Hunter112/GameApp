package com.parth.sportsapp.sportsbackend.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@Service
public class EmailService {

  @Autowired
  private JavaMailSender mailSender;

  // Ideally, inject your frontend URL from application.properties
  // @Value("${app.frontend.url}")
  // private String frontendUrl;
  private final String frontendUrl = "http://localhost:3000";

  public void sendMailWithAttachment(String to, String subject, String verificationLink) {


    try {
      MimeMessage message = mailSender.createMimeMessage();

      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      helper.setTo(to);
      helper.setSubject(subject);

      String htmlContent = "<h3>Welcome to DuoSport!</h3>"
          + "<p>Please verify your account by clicking the link below:</p>"
          + "<a href=\"" + verificationLink + "\">Verify Account</a>";


      helper.setText(htmlContent, true);

      mailSender.send(message);

    } catch (MessagingException e) {
      throw new RuntimeException("Failed to send email", e);
    }

  }

  /**
   * Password reset: emails a 6-digit code. The code itself is generated and
   * hashed by PasswordResetService; this method only delivers it.
   */
  @Async
  public void sendPasswordResetCode(String to, String firstName, String code) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setSubject("Your Sportsman password reset code");

      String safeName = StringEscapeUtils.escapeHtml4(firstName == null ? "there" : firstName);
      String safeCode = StringEscapeUtils.escapeHtml4(code);
      String html = "<h3>Hi " + safeName + ",</h3>"
          + "<p>Use this code to reset your Sportsman password:</p>"
          + "<p style=\"font-size:28px;font-weight:bold;letter-spacing:6px\">" + safeCode + "</p>"
          + "<p>The code expires in 15 minutes. If you didn't ask for this, you can ignore this email — "
          + "your password stays unchanged.</p>";

      helper.setText(html, true);
      mailSender.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException("Failed to send email", e);
    }
  }

  /**
   * Sends an Invite to an external user to join the app
   * Used by MatchService when a user logs a game against a non-user.
   */
  @Async
  public void sendInvite(String toEmail, String inviterName) {
    // Escape ALL user-controlled values before placing them in HTML / URLs.
    // inviterName comes from a user's profile and must never be trusted as markup.
    String safeName = StringEscapeUtils.escapeHtml4(inviterName == null ? "A player" : inviterName);
    // Subject is plain text (not HTML) but we still strip control chars by escaping.
    String subject = safeName + " challenged you on DuoSport!";

    // URL-encode the email so it can't break out of the href attribute or the query string.
    String encodedEmail = URLEncoder.encode(toEmail == null ? "" : toEmail, StandardCharsets.UTF_8);
    String registerUrl = frontendUrl + "/register?email=" + encodedEmail;
    String safeHref = StringEscapeUtils.escapeHtml4(registerUrl);

    String htmlContent = "<div style='font-family: Arial, sans-serif;'>"
        + "<h2>Game Result Logged!</h2>"
        + "<p>Hi there,</p>"
        + "<p><strong>" + safeName + "</strong> has logged a match result against you on DuoSport.</p>"
        + "<p>They claimed they won!</p>"
        + "<p>To confirm (or dispute) this score and track your own stats, create your free account:</p>"
        + "<br>"
        + "<a href=\"" + safeHref + "\" "
        + "style='background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>"
        + "View Match &amp; Sign Up</a>"
        + "<br><br>"
        + "<p>See you on the court,<br>The DuoSport Team</p>"
        + "</div>";

    sendHtmlEmail(toEmail, subject, htmlContent);
  }

  /**
   * Generic Helper to send HTML emails
   */
  private void sendHtmlEmail(String to, String subject, String htmlContent) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      // 'true' indicates multipart message (needed for HTML)
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true); // true = isHtml

      mailSender.send(message);

    } catch (MessagingException e) {
      // In production, log this error instead of crashing
      throw new RuntimeException("Failed to send email to " + to, e);
    }
  }
}
