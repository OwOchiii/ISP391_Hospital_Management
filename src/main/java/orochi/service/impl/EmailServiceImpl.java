package orochi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import orochi.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.time.Year;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final TemplateEngine templateEngine;
    private final JavaMailSender emailSender;

    @Autowired
    public EmailServiceImpl(TemplateEngine templateEngine, JavaMailSender emailSender) {
        this.templateEngine = templateEngine;
        this.emailSender = emailSender;
    }

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendHtmlMessage(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true indicates HTML content

            emailSender.send(message);
            logger.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetUrl) {
        String subject = "Password Reset Request";

        Context context = new Context();
        // Set up the variables used in the template
        context.setVariable("resetLink", resetUrl);
        context.setVariable("userEmail", to);
        context.setVariable("userName", to.substring(0, to.indexOf('@')));
        context.setVariable("expirationTime", "1 hour");

        // Add the missing expirationDate variable (1 hour from now)
        java.util.Date expirationDate = new java.util.Date(System.currentTimeMillis() + 3600000); // 1 hour in milliseconds
        context.setVariable("expirationDate", expirationDate);

        // Add other variables used in template
        context.setVariable("requestIp", "Unknown");
        context.setVariable("supportPhone", "+1 (555) 123-4567");
        context.setVariable("hospitalAddress", "123 Healthcare Ave, Medical City, MC 12345");
        context.setVariable("baseUrl", "https://your-app-url.com");

        String htmlContent = templateEngine.process("email/password-reset.html", context);
        sendHtmlMessage(to, subject, htmlContent);
    }
}