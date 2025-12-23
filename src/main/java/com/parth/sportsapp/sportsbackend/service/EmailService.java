package com.parth.sportsapp.sportsbackend.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  @Autowired
  private JavaMailSender mailSender;

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
}
