package orochi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import orochi.config.CustomUserDetails;
import orochi.dto.AppointmentDTO;
import orochi.model.Appointment;
import orochi.repository.AppointmentRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    public Appointment saveAppointment(AppointmentDTO appointmentDTO) {
        try {
            logger.info("Saving appointment for doctorId: {}", appointmentDTO.getDoctorId());

            // Get authenticated user's PatientID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer patientId = userDetails.getPatientId();
            if (patientId == null) {
                throw new RuntimeException("Patient ID not found for user: " + userDetails.getUsername());
            }

            // Create Appointment entity
            Appointment appointment = new Appointment();
            appointment.setDoctorId(appointmentDTO.getDoctorId());
            appointment.setPatientId(patientId);
            appointment.setRoomId(null); // Explicitly set to null
            appointment.setDescription(appointmentDTO.getDescription());
            appointment.setDateTime(LocalDateTime.parse(appointmentDTO.getDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            appointment.setStatus("Scheduled");
            appointment.setEmail(appointmentDTO.getEmail());
            appointment.setPhoneNumber(appointmentDTO.getPhoneNumber());

            Appointment savedAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment saved with ID: {}", savedAppointment.getAppointmentId());
            return savedAppointment;
        } catch (Exception e) {
            logger.error("Error saving appointment: {}", e.getMessage());
            throw new RuntimeException("Failed to save appointment: " + e.getMessage());
        }
    }
}