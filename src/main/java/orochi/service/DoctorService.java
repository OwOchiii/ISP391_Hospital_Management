package orochi.service;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.repository.AppointmentRepository;
import orochi.repository.PatientRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DoctorService {
    private static final Logger logger = LoggerFactory.getLogger(DoctorService.class);

    @Getter
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    @Autowired
    public DoctorService(AppointmentRepository appointmentRepository,
                         PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Get all appointments for a doctor
     */
    public List<Appointment> getAppointments(Integer doctorId) {
        try {
            logger.info("Fetching all appointments for doctor with ID: {}", doctorId);
            return appointmentRepository.findByDoctorIdOrderByDateTimeDesc(doctorId);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch appointments for doctor with ID: {}", doctorId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get appointments for a doctor on a specific date
     */
    public List<Appointment> getAppointmentsByDate(Integer doctorId, LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusSeconds(1);

            logger.info("Fetching appointments for doctor ID: {} on date: {} (from {} to {})",
                    doctorId, date, startOfDay, endOfDay);

            return appointmentRepository.findByDoctorIdAndDateTimeBetweenOrderByDateTime(
                    doctorId, startOfDay, endOfDay);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch appointments for doctor ID: {} on date: {}", doctorId, date, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get appointments by status (pending, completed, cancelled, etc.)
     */
    public List<Appointment> getAppointmentsByStatus(Integer doctorId, String status) {
        try {
            logger.info("Fetching appointments for doctor ID: {} with status: {}", doctorId, status);
            return appointmentRepository.findByDoctorIdAndStatusOrderByDateTimeDesc(doctorId, status);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch appointments for doctor ID: {} with status: {}", doctorId, status, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get today's appointments for a doctor
     */
    public List<Appointment> getTodayAppointments(Integer doctorId) {
        try {
            logger.info("Fetching today's appointments for doctor ID: {}", doctorId);
            return appointmentRepository.findTodayAppointmentsForDoctor(doctorId);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch today's appointments for doctor ID: {}", doctorId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get upcoming appointments for a doctor
     */
    public List<Appointment> getUpcomingAppointments(Integer doctorId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            logger.info("Fetching upcoming appointments for doctor ID: {} after: {}", doctorId, now);
            return appointmentRepository.findUpcomingAppointmentsForDoctor(doctorId, now);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch upcoming appointments for doctor ID: {}", doctorId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get appointment details by appointmentId
     */
    public Optional<Appointment> getAppointmentDetails(Integer appointmentId, Integer doctorId) {
        try {
            logger.info("Fetching appointment details for appointment ID: {} and doctor ID: {}", appointmentId, doctorId);
            Appointment appointment = appointmentRepository.findByAppointmentIdAndDoctorId(appointmentId, doctorId);
            return Optional.ofNullable(appointment);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch appointment details for appointment ID: {} and doctor ID: {}",
                    appointmentId, doctorId, e);
            return Optional.empty();
        }
    }

    /**
     * Get patient details by patientId
     */
    public Optional<Patient> getPatientDetails(Integer patientId) {
        try {
            logger.info("Fetching patient details for patient ID: {}", patientId);
            return patientRepository.findById(patientId);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch patient details for patient ID: {}", patientId, e);
            return Optional.empty();
        }
    }

    /**
     * Get patients with appointments for this doctor
     */
    public List<Patient> getPatientsWithAppointments(Integer doctorId) {
        try {
            logger.info("Fetching patients with appointments for doctor ID: {}", doctorId);
            return patientRepository.findPatientsWithAppointmentsByDoctorId(doctorId);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch patients with appointments for doctor ID: {}", doctorId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Search patients by name
     */
    public List<Patient> searchPatientsByName(String name) {
        try {
            logger.info("Searching patients by name containing: {}", name);
            return patientRepository.findByFullNameContainingIgnoreCase(name);
        } catch (DataAccessException e) {
            logger.error("Failed to search patients by name: {}", name, e);
            return Collections.emptyList();
        }
    }

}