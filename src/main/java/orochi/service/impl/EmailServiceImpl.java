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
import orochi.model.Receipt;
import orochi.model.Transaction;

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

        // 🔥 ADD STARTUP LOGGING FOR EMAIL CONFIGURATION
        logger.info("=== EMAIL SERVICE INITIALIZED ===");
        logger.info("TemplateEngine: {}", templateEngine != null ? "CONFIGURED" : "NULL");
        logger.info("JavaMailSender: {}", emailSender != null ? "CONFIGURED" : "NULL");

        if (emailSender != null) {
            try {
                // Test email configuration
                logger.info("Email sender class: {}", emailSender.getClass().getName());
            } catch (Exception e) {
                logger.error("Error checking email sender configuration: {}", e.getMessage());
            }
        }
    }

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            logger.info("=== SENDING SIMPLE EMAIL ===");
            logger.info("To: {}", to);
            logger.info("Subject: {}", subject);
            logger.info("Text length: {} characters", text != null ? text.length() : 0);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            logger.info("Attempting to send simple email...");
            emailSender.send(message);
            logger.info("✅ Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("❌ Failed to send simple email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendHtmlMessage(String to, String subject, String htmlContent) {
        try {
            logger.info("=== SENDING HTML EMAIL ===");
            logger.info("To: {}", to);
            logger.info("Subject: {}", subject);
            logger.info("HTML content length: {} characters", htmlContent != null ? htmlContent.length() : 0);

            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                logger.error("❌ HTML content is null or empty - cannot send email");
                throw new RuntimeException("HTML content is null or empty");
            }

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true indicates HTML content

            logger.info("Attempting to send HTML email...");
            emailSender.send(message);
            logger.info("✅ HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("❌ Failed to send HTML email to {}: {}", to, e.getMessage(), e);
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
            String patientPortalUrl = "http://localhost:8089/" + appointment.getPatientId();
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
     * Sends a receipt email to a patient after payment processing
     */
    @Override
    public void sendReceiptEmail(String to, Receipt receipt, Patient patient, Transaction transaction) {
        try {
            logger.info("=== SENDING RECEIPT EMAIL ===");
            logger.info("To: {}", to);
            logger.info("Receipt ID: {}", receipt != null ? receipt.getReceiptId() : "NULL");
            logger.info("Patient ID: {}", patient != null ? patient.getPatientId() : "NULL");
            logger.info("Transaction ID: {}", transaction != null ? transaction.getTransactionId() : "NULL");

            // 🔥 VALIDATE INPUT PARAMETERS
            if (to == null || to.trim().isEmpty()) {
                logger.error("❌ Email address is null or empty");
                throw new RuntimeException("Email address is required");
            }

            if (receipt == null) {
                logger.error("❌ Receipt object is null");
                throw new RuntimeException("Receipt object is required");
            }

            if (patient == null) {
                logger.error("❌ Patient object is null");
                throw new RuntimeException("Patient object is required");
            }

            if (transaction == null) {
                logger.error("❌ Transaction object is null");
                throw new RuntimeException("Transaction object is required");
            }

            // 🔥 VALIDATE PATIENT AND USER RELATIONSHIPS
            if (patient.getUser() == null) {
                logger.error("❌ Patient.User is null for Patient ID: {}", patient.getPatientId());
                throw new RuntimeException("Patient user information is missing");
            }

            if (patient.getUser().getFullName() == null || patient.getUser().getFullName().trim().isEmpty()) {
                logger.warn("⚠️ Patient full name is null or empty for Patient ID: {}", patient.getPatientId());
            }

            logger.info("✅ Input validation passed");
            logger.info("Patient Name: {}", patient.getUser().getFullName());
            logger.info("Receipt Number: {}", receipt.getReceiptNumber());
            logger.info("Receipt Amount: {}", receipt.getTotalAmount());
            logger.info("Transaction Method: {}", transaction.getMethod());
            logger.info("Transaction Status: {}", transaction.getStatus());

            String subject = "Payment Receipt - MediCare Plus";

            Context context = new Context();

            // 🔥 SET CORE DATA WITH NULL SAFETY
            context.setVariable("receipt", receipt);
            context.setVariable("patient", patient);
            context.setVariable("transaction", transaction);

            // 🔥 SET FORMATTED DATA FOR TEMPLATE CONVENIENCE
            try {
                // Format amounts for display
                if (receipt.getTotalAmount() != null) {
                    context.setVariable("formattedTotalAmount",
                        String.format("%,.0f VND", receipt.getTotalAmount().doubleValue()));
                }

                if (receipt.getTaxAmount() != null) {
                    context.setVariable("formattedTaxAmount",
                        String.format("%,.0f VND", receipt.getTaxAmount().doubleValue()));
                }

                if (receipt.getDiscountAmount() != null) {
                    context.setVariable("formattedDiscountAmount",
                        String.format("%,.0f VND", receipt.getDiscountAmount().doubleValue()));
                }

                // Calculate subtotal (total + discount - tax)
                if (receipt.getTotalAmount() != null && receipt.getTaxAmount() != null && receipt.getDiscountAmount() != null) {
                    double subtotal = receipt.getTotalAmount().doubleValue() + receipt.getDiscountAmount().doubleValue() - receipt.getTaxAmount().doubleValue();
                    context.setVariable("formattedSubtotal", String.format("%,.0f VND", subtotal));
                }

                // Format dates
                if (receipt.getIssuedDate() != null) {
                    context.setVariable("formattedIssuedDate", receipt.getIssuedDate().toString());
                } else {
                    context.setVariable("formattedIssuedDate", java.time.LocalDate.now().toString());
                }

                // Format payment time
                if (transaction.getTimeOfPayment() != null) {
                    context.setVariable("formattedPaymentTime",
                        transaction.getTimeOfPayment().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                }

                // Patient information
                context.setVariable("patientName", patient.getUser().getFullName());
                context.setVariable("patientEmail", patient.getUser().getEmail());
                context.setVariable("patientPhone", patient.getUser().getPhoneNumber());

                // Payment method normalization
                String paymentMethod = transaction.getMethod();
                if (paymentMethod != null) {
                    switch (paymentMethod.toLowerCase()) {
                        case "banking":
                        case "vnpay":
                        case "momo":
                        case "zalopay":
                            context.setVariable("displayPaymentMethod", "Online Banking");
                            break;
                        case "cash":
                            context.setVariable("displayPaymentMethod", "Cash");
                            break;
                        default:
                            context.setVariable("displayPaymentMethod", paymentMethod);
                    }
                } else {
                    context.setVariable("displayPaymentMethod", "Unknown");
                }

                logger.info("✅ Template variables set successfully");

            } catch (Exception dataException) {
                logger.error("Error setting template data: {}", dataException.getMessage(), dataException);
                // Continue with basic data
            }

            // Set hospital information variables
            Map<String, String> hospitalInfo = getHospitalInfo();
            for (Map.Entry<String, String> entry : hospitalInfo.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
                logger.debug("Hospital info - {}: {}", entry.getKey(), entry.getValue());
            }

            logger.info("Processing Thymeleaf template: email/receipt-email");

            // 🔥 PROCESS TEMPLATE WITH ENHANCED ERROR HANDLING
            String htmlContent = null;
            try {
                htmlContent = templateEngine.process("email/receipt-email", context);
                logger.info("✅ Template processed successfully");
                logger.info("Generated HTML content length: {} characters", htmlContent != null ? htmlContent.length() : 0);

                if (htmlContent == null || htmlContent.trim().isEmpty()) {
                    logger.error("❌ Template processing returned null or empty content");
                    throw new RuntimeException("Template processing failed - empty content");
                }

                // Log a snippet of the generated HTML for debugging
                if (htmlContent.length() > 200) {
                    logger.debug("HTML content preview: {}...", htmlContent.substring(0, 200));
                } else {
                    logger.debug("HTML content: {}", htmlContent);
                }

                // Send the HTML email
                logger.info("Sending HTML receipt email...");
                sendHtmlMessage(to, subject, htmlContent);
                logger.info("✅ Receipt email sent successfully to: {}", to);

            } catch (Exception templateException) {
                logger.error("❌ Template processing failed: {}", templateException.getMessage(), templateException);
                logger.error("Template: email/receipt-email");
                logger.error("Context variables count: {}", context.getVariableNames().size());
                logger.error("Available variables: {}", context.getVariableNames());

                // 🔥 CREATE ENHANCED FALLBACK HTML EMAIL INSTEAD OF PLAIN TEXT
                logger.info("Creating enhanced fallback HTML email...");
                String fallbackHtmlContent = createFallbackReceiptHtml(receipt, patient, transaction, hospitalInfo);

                logger.info("Sending fallback HTML email...");
                sendHtmlMessage(to, subject, fallbackHtmlContent);
                logger.info("✅ Fallback HTML receipt email sent successfully to: {}", to);
            }

        } catch (Exception e) {
            logger.error("❌ Failed to send receipt email to {}: {}", to, e.getMessage(), e);
            logger.error("Full stack trace:", e);

            // 🔥 ATTEMPT EMERGENCY FALLBACK EMAIL WITH BETTER FORMATTING
            try {
                logger.info("Attempting emergency fallback email...");
                String emergencySubject = "Payment Confirmation - MediCare Plus";
                String emergencyHtml = createEmergencyFallbackHtml(receipt, patient, transaction);

                sendHtmlMessage(to, emergencySubject, emergencyHtml);
                logger.info("✅ Emergency fallback email sent successfully to: {}", to);
            } catch (Exception fallbackException) {
                logger.error("❌ Even emergency fallback email failed: {}", fallbackException.getMessage(), fallbackException);
                // Don't throw exception to prevent receipt process from failing if email fails
            }
        }
    }

    /**
     * Create fallback HTML content when template processing fails
     */
    private String createFallbackReceiptHtml(Receipt receipt, Patient patient, Transaction transaction, Map<String, String> hospitalInfo) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'>");
        html.append("<title>Payment Receipt - MediCare Plus</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f9f9f9; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 0 20px rgba(0, 0, 0, 0.1); }");
        html.append(".header { background: linear-gradient(135deg, #007bff, #0056b3); color: #ffffff; padding: 25px; text-align: center; }");
        html.append(".content { padding: 30px; }");
        html.append(".receipt-box { border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 25px; }");
        html.append(".detail-row { display: flex; margin-bottom: 10px; }");
        html.append(".detail-label { width: 40%; font-weight: 600; color: #555; }");
        html.append(".detail-value { width: 60%; }");
        html.append(".summary { background-color: #f8f9fa; border-radius: 6px; padding: 15px; margin-top: 15px; }");
        html.append(".total { font-weight: 700; font-size: 18px; color: #007bff; }");
        html.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 14px; color: #666; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>Payment Receipt</h1>");
        html.append("<p>Thank you for your payment</p>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Dear ").append(patient.getUser().getFullName()).append(",</p>");
        html.append("<p>Thank you for your payment. Please find your receipt details below:</p>");

        html.append("<div class='receipt-box'>");
        html.append("<h3 style='color: #007bff; margin-top: 0;'>RECEIPT</h3>");
        html.append("<p><strong>Receipt #:</strong> ").append(receipt.getReceiptNumber() != null ? receipt.getReceiptNumber() : "N/A").append("</p>");

        html.append("<div class='detail-row'>");
        html.append("<div class='detail-label'>Patient Name:</div>");
        html.append("<div class='detail-value'>").append(patient.getUser().getFullName()).append("</div>");
        html.append("</div>");

        html.append("<div class='detail-row'>");
        html.append("<div class='detail-label'>Patient ID:</div>");
        html.append("<div class='detail-value'>").append(patient.getPatientId()).append("</div>");
        html.append("</div>");

        html.append("<div class='detail-row'>");
        html.append("<div class='detail-label'>Date:</div>");
        html.append("<div class='detail-value'>").append(receipt.getIssuedDate() != null ? receipt.getIssuedDate().toString() : java.time.LocalDate.now().toString()).append("</div>");
        html.append("</div>");

        html.append("<div class='detail-row'>");
        html.append("<div class='detail-label'>Transaction ID:</div>");
        html.append("<div class='detail-value'>").append(transaction.getTransactionId()).append("</div>");
        html.append("</div>");

        html.append("<div class='detail-row'>");
        html.append("<div class='detail-label'>Payment Method:</div>");
        html.append("<div class='detail-value'>").append(transaction.getMethod() != null ? transaction.getMethod() : "Unknown").append("</div>");
        html.append("</div>");

        html.append("<div class='summary'>");
        html.append("<div class='total'>Total Amount: ").append(String.format("%,.0f VND", receipt.getTotalAmount().doubleValue())).append("</div>");
        html.append("</div>");

        html.append("<p style='margin-top: 20px; font-style: italic;'>");
        html.append(receipt.getNotes() != null ? receipt.getNotes() : "Thank you for your payment. We appreciate your business.");
        html.append("</p>");

        html.append("</div>");

        html.append("<p>If you have any questions regarding this payment, please contact our billing department at ");
        html.append(hospitalInfo.get("hospitalPhone")).append(" or email us at ");
        html.append("<a href='mailto:").append(hospitalInfo.get("hospitalEmail")).append("'>").append(hospitalInfo.get("hospitalEmail")).append("</a>.</p>");

        html.append("<p>Thank you for choosing ").append(hospitalInfo.get("hospitalName")).append(" for your healthcare needs.</p>");
        html.append("<p>Best regards,<br>").append(hospitalInfo.get("hospitalName")).append(" Team</p>");

        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>").append(hospitalInfo.get("hospitalName")).append("</p>");
        html.append("<p>").append(hospitalInfo.get("hospitalAddress")).append("</p>");
        html.append("<p>Phone: ").append(hospitalInfo.get("hospitalPhone")).append(" | Email: ");
        html.append("<a href='mailto:").append(hospitalInfo.get("hospitalEmail")).append("'>").append(hospitalInfo.get("hospitalEmail")).append("</a></p>");
        html.append("</div>");

        html.append("</div></body></html>");

        return html.toString();
    }

    /**
     * Create emergency fallback HTML content
     */
    private String createEmergencyFallbackHtml(Receipt receipt, Patient patient, Transaction transaction) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'>");
        html.append("<title>Payment Confirmation - MediCare Plus</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f9f9f9; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 8px; }");
        html.append(".header { text-align: center; color: #007bff; margin-bottom: 30px; }");
        html.append(".content { line-height: 1.6; }");
        html.append(".highlight { background-color: #e7f3ff; padding: 15px; border-radius: 5px; margin: 20px 0; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>Payment Confirmation</h1>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Dear ").append(patient != null && patient.getUser() != null ? patient.getUser().getFullName() : "Valued Patient").append(",</p>");

        html.append("<p>We have successfully received your payment. Here are the basic details:</p>");

        html.append("<div class='highlight'>");
        if (receipt != null) {
            html.append("<p><strong>Receipt Number:</strong> ").append(receipt.getReceiptNumber() != null ? receipt.getReceiptNumber() : "Generated").append("</p>");
            html.append("<p><strong>Amount:</strong> ").append(String.format("%,.0f VND", receipt.getTotalAmount().doubleValue())).append("</p>");
            html.append("<p><strong>Date:</strong> ").append(receipt.getIssuedDate() != null ? receipt.getIssuedDate().toString() : java.time.LocalDate.now().toString()).append("</p>");
        }
        if (transaction != null) {
            html.append("<p><strong>Transaction ID:</strong> ").append(transaction.getTransactionId()).append("</p>");
        }
        html.append("</div>");

        html.append("<p>Due to a technical issue, we cannot send your detailed receipt at this moment. ");
        html.append("Please contact our support team if you need a detailed copy of your receipt.</p>");

        html.append("<p><strong>Contact Information:</strong><br>");
        html.append("Phone: (555) 123-4567<br>");
        html.append("Email: <a href='mailto:support@medicareplus.com'>support@medicareplus.com</a></p>");

        html.append("<p>Thank you for choosing MediCare Plus for your healthcare needs.</p>");
        html.append("<p>Best Regards,<br>MediCare Plus Team</p>");

        html.append("</div></div></body></html>");

        return html.toString();
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
