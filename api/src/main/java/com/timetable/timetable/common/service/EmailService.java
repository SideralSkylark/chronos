package com.timetable.timetable.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {
  private final JavaMailSender mailSender;

  @Value("${spring.mail.username:}")
  private String fromEmail;

  @Value("${app.email.from:noreply.timetable@gmail.com}")
  private String defaultFromEmail;

  public void sendVerificationCode(String toEmail, String code) {
    log.debug("Sending verification email to: {}", toEmail);
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(toEmail);
      message.setSubject("Timetable - Email Verification");
      message.setText(buildVerificationEmailText(code));
      message.setFrom(getFromEmail());

      mailSender.send(message);
      log.info("Verification email sent to: {}", toEmail);
    } catch (Exception e) {
      log.error("Failed to send verification email to: {}", toEmail, e);
    }
  }

  public void sendSimpleEmail(String toEmail, String subject, String body) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(toEmail);
      message.setSubject(subject);
      message.setText(body);
      message.setFrom(getFromEmail());

      mailSender.send(message);
      log.info("Email sent to: {}, subject: {}", toEmail, subject);
    } catch (Exception e) {
      log.error("Failed to send email to: {}, subject: {}", toEmail, subject, e);
    }
  }

  public void sendEmailWithAttachment(
      String toEmail, String subject, String body, MultipartFile attachment)
      throws MessagingException, IOException {
    log.debug("Sending email with atachement to: {}", toEmail);
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);

    helper.setTo(toEmail);
    helper.setSubject(subject);
    helper.setText(body, true); // true para HTML
    helper.setFrom(getFromEmail());

    String originalFilename = attachment.getOriginalFilename();
    String filename = originalFilename != null ? originalFilename : "attachment";

    helper.addAttachment(filename, new ByteArrayResource(attachment.getBytes()));

    mailSender.send(message);
    log.info("Email with attachment sent to: {}, subject: {}", toEmail, subject);
  }

  private String buildVerificationEmailText(String code) {
    return """
    Timetable - Email Verification

    Your verification code is: %s

    This code will expire in 10 minutes.

    Enter this code in the application to verify your email address.

    If you did not request this verification, please ignore this email.

    Best regards,
    Timetable Team
    """
        .formatted(code);
  }

  private String getFromEmail() {
    // Usa o email configurado ou o default
    return fromEmail != null && !fromEmail.isEmpty() ? fromEmail : defaultFromEmail;
  }
}
