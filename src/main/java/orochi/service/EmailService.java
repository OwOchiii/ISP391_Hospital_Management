package orochi.service;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
    void sendPasswordResetEmail(String to, String resetUrl);
    void sendHtmlMessage(String to, String subject, String htmlContent);
}