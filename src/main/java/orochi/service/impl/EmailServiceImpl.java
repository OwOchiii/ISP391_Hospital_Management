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
import orochi.model.Patient;
import orochi.model.PatientContact;
import orochi.model.Appointment;

import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        context.setVariable("expirationTime", "10 minutes");

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

    @Override
    public void sendPatientRegistrationEmail(String to, Patient patient,String unhashedPassword) {
        try {
            String subject = "Registration Confirmation - MediCare Plus";

            Context context = new Context();

            // Set patient data in the context
            context.setVariable("patient", patient);
            context.setVariable("unhashedPassword", unhashedPassword);
            // Set hospital information variables
            Map<String, String> hospitalInfo = getHospitalInfo();
            for (Map.Entry<String, String> entry : hospitalInfo.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }

            // Set the patient portal URL
            String patientPortalUrl = "http://localhost:8080/patient/dashboard?patientId=" + patient.getPatientId();
            context.setVariable("patientPortalUrl", patientPortalUrl);

            // Process the template
            String htmlContent = templateEngine.process("email/patient-register-email", context);

            // Log the HTML content for debugging
            logger.debug("Generated HTML email content:");
            logger.debug(htmlContent);

            // Send the email
            sendHtmlMessage(to, subject, htmlContent);

            logger.info("Patient registration email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send patient registration email to {}: {}", to, e.getMessage(), e);
            // Don't throw exception to prevent registration process from failing if email fails
        }
    }

    /**
     * Sends an email notification to a patient when their profile is updated by a receptionist
     */
    @Override
    public void sendProfileUpdateEmail(String to, Patient patient) {
        try {
            String subject = "Your Profile Has Been Updated - MediCare Plus";

            Context context = new Context();

            // Set patient data in the context
            context.setVariable("patient", patient);

            // Set hospital information variables
            Map<String, String> hospitalInfo = getHospitalInfo();
            for (Map.Entry<String, String> entry : hospitalInfo.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }

            // Set the patient portal URL
            String patientPortalUrl = "http://localhost:8080/patient/dashboard?patientId=" + patient.getPatientId();
            context.setVariable("patientPortalUrl", patientPortalUrl);

            // Add current date for the email
            context.setVariable("currentDate", java.time.LocalDate.now().toString());

            // Process the template
            String htmlContent = templateEngine.process("email/patient-profile-update", context);

            // If template processing fails, send a simple plain text email
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                String plainTextContent = "Dear " + patient.getUser().getFullName() + ",\n\n" +
                        "Your patient profile has been updated as per your request by our receptionist team. " +
                        "You can login to your patient portal to view the updated information.\n\n" +
                        "Thank you for choosing MediCare Plus for your healthcare needs.\n\n" +
                        "Best Regards,\n" +
                        "MediCare Plus Team";
                sendSimpleMessage(to, subject, plainTextContent);
                return;
            }

            // Log the HTML content for debugging
            logger.debug("Generated HTML email content:");
            logger.debug(htmlContent);

            // Send the email
            sendHtmlMessage(to, subject, htmlContent);

            logger.info("Profile update email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send profile update email to {}: {}", to, e.getMessage(), e);
            // Don't throw exception to prevent update process from failing if email fails
        }
    }

    /**
     * Sends an email notification to a patient when their appointment is updated due to doctor's schedule conflicts
     */
    @Override
    public void sendAppointmentUpdateEmail(String to, Appointment appointment, String reason) {
        try {
            String subject = "Your Appointment Has Been Updated - MediCare Plus";

            Context context = new Context();

            // Set appointment data in the context
            context.setVariable("appointment", appointment);
            context.setVariable("patient", appointment.getPatient());
            context.setVariable("doctor", appointment.getDoctor());
            context.setVariable("updateReason", reason);

            // Set hospital information variables
            Map<String, String> hospitalInfo = getHospitalInfo();
            for (Map.Entry<String, String> entry : hospitalInfo.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }

            // Set the patient portal URL
            String patientPortalUrl = "http://localhost:8080/patient/appointments?patientId=" + appointment.getPatientId();
            context.setVariable("patientPortalUrl", patientPortalUrl);

            // Add current date for the email
            context.setVariable("currentDate", java.time.LocalDate.now().toString());

            // Format appointment date/time for display
            String formattedDate = appointment.getDateTime().toLocalDate().toString();
            String formattedTime = formatTimeForDisplay(appointment.getDateTime().toLocalTime().toString());
            context.setVariable("appointmentDate", formattedDate);
            context.setVariable("appointmentTime", formattedTime);

            // Process the template
            String htmlContent = templateEngine.process("email/appointment-update-email", context);

            // If template processing fails, send a simple plain text email
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                String plainTextContent = "Dear " + appointment.getPatient().getUser().getFullName() + ",\n\n" +
                        "Your appointment scheduled with Dr. " + appointment.getDoctor().getUser().getFullName() +
                        " for " + formattedDate + " at " + formattedTime +
                        " has been updated due to " + reason + ".\n\n" +
                        "New appointment details:\n" +
                        "Date: " + formattedDate + "\n" +
                        "Time: " + formattedTime + "\n" +
                        "Doctor: Dr. " + appointment.getDoctor().getUser().getFullName() + "\n\n" +
                        "You can login to your patient portal to view the updated appointment details.\n\n" +
                        "If you have any questions or need to reschedule, please contact our reception desk at " +
                        hospitalInfo.get("hospitalPhone") + ".\n\n" +
                        "Thank you for your understanding.\n\n" +
                        "Best Regards,\n" +
                        "MediCare Plus Team";
                sendSimpleMessage(to, subject, plainTextContent);
                return;
            }

            // Log the HTML content for debugging
            logger.debug("Generated HTML email content for appointment update:");
            logger.debug(htmlContent);

            // Send the email
            sendHtmlMessage(to, subject, htmlContent);

            logger.info("Appointment update email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send appointment update email to {}: {}", to, e.getMessage(), e);
            // Don't throw exception to prevent update process from failing if email fails
        }
    }

    /**
     * Format time string for better readability in emails
     */
    private String formatTimeForDisplay(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            String period = hour >= 12 ? "PM" : "AM";
            int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);

            return String.format("%d:%02d %s", displayHour, minute, period);
        } catch (Exception e) {
            logger.warn("Error formatting time: {}", e.getMessage());
            return timeStr;
        }
    }

    /**
     * Helper method to get hospital information for email templates
     */
    private Map<String, String> getHospitalInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("hospitalName", "MediCare Plus");
        info.put("hospitalTagline", "Your Health, Our Priority");
        info.put("hospitalPhone", "(555) 123-4567");
        info.put("hospitalEmail", "support@medicareplus.com");
        info.put("hospitalHours", "Monday - Friday, 8:00 AM - 6:00 PM");
        info.put("hospitalAddress", "123 Medical Center Drive, Healthcare City, HC 12345");
        info.put("privacyPolicyUrl", "https://medicareplus.com/privacy");
        info.put("termsOfServiceUrl", "https://medicareplus.com/terms");
        info.put("contactUsUrl", "https://medicareplus.com/contact");
        info.put("unsubscribeUrl", "https://medicareplus.com/unsubscribe");
        info.put("privacyEmail", "privacy@medicareplus.com");
        info.put("emailDisclaimer", "This email contains confidential patient information. If you received this email in error, please delete it immediately and notify us at privacy@medicareplus.com. This communication is protected under HIPAA regulations.");
        info.put("welcomeMessage", "Welcome to MediCare Plus! Your patient registration has been successfully completed. We're excited to have you as part of our healthcare family.");
        return info;
    }
}
