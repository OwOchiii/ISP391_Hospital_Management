package orochi.service;

import orochi.model.Patient;
import orochi.model.Appointment;
import orochi.model.Receipt;
import orochi.model.Transaction;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
    void sendPasswordResetEmail(String to, String resetUrl);
    void sendHtmlMessage(String to, String subject, String htmlContent);
    void sendPatientRegistrationEmail(String to, Patient patient,String unhashedPassword);
    void sendProfileUpdateEmail(String to, Patient patient);
    void sendAppointmentUpdateEmail(String to, Appointment appointment, String reason);
    void sendReceiptEmail(String to, Receipt receipt, Patient patient, Transaction transaction);
}