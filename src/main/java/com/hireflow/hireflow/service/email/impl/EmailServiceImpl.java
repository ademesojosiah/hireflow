package com.hireflow.hireflow.service.email.impl;

import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.service.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    @Async("emailAsyncExecutor")
    public void sendOtp(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Your HireFlow Verification Code");
            helper.setText(buildOtpEmailBody(otp), true);
            mailSender.send(message);
            log.info("OTP email sent to {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send OTP email to {}: {}", to, ex.getMessage());
            throw new CustomException("Failed to send verification email. Please try again.");
        }
    }

    @Override
    @Async("emailAsyncExecutor")
    public void sendCompanyWelcome(String to, String firstName, String companyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Welcome to HireFlow, " + companyName + "!");
            helper.setText(buildWelcomeEmailBody(firstName, companyName), true);
            mailSender.send(message);
            log.info("Welcome email sent to {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send welcome email to {}: {}", to, ex.getMessage());
            throw new CustomException("Failed to send welcome email.");
        }
    }

    private String buildWelcomeEmailBody(String firstName, String companyName) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px;">
                    <h2 style="color: #2563EB;">Welcome to HireFlow, %s!</h2>
                    <p>Your company <strong>%s</strong> has been successfully registered on HireFlow.</p>
                    <p>You can now create job listings, manage candidates, and run AI-powered screening.</p>
                    <p style="color: #6B7280; font-size: 12px;">
                      Need help? Reply to this email and our team will assist you.
                    </p>
                  </body>
                </html>
                """.formatted(firstName, companyName);
    }

    private String buildOtpEmailBody(String otp) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px;">
                    <h2 style="color: #2563EB;">HireFlow Email Verification</h2>
                    <p>Your one-time verification code is:</p>
                    <div style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #1E40AF; padding: 16px 0;">
                      %s
                    </div>
                    <p>This code expires in <strong>10 minutes</strong>.</p>
                    <p style="color: #6B7280; font-size: 12px;">
                      If you did not create a HireFlow account, you can safely ignore this email.
                    </p>
                  </body>
                </html>
                """.formatted(otp);
    }
}
