package orochi.service;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Appointment;
import orochi.model.MedicalOrder;
import orochi.model.Patient;
import orochi.repository.*;

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
    private final DoctorRepository doctorRepository;
    @Getter
    private final MedicalOrderRepository medicalOrderRepository;
    @Getter
    private final PrescriptionRepository prescriptionRepository;
    @Getter
    private final PatientContactRepository patientContactRepository;

    @Autowired
    public DoctorService(AppointmentRepository appointmentRepository,
                         PatientRepository patientRepository,
                         DoctorRepository doctorRepository, MedicalOrderRepository medicalOrderRepository, PrescriptionRepository prescriptionRepository, PatientContactRepository patientContactRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.medicalOrderRepository = medicalOrderRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.patientContactRepository = patientContactRepository;
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
     * This method allows access to both the assigned doctor and doctors from medical order departments
     */
    public Optional<Appointment> getAppointmentDetails(Integer appointmentId, Integer doctorId) {
        try {
            logger.info("Fetching appointment details for appointment ID: {} and doctor ID: {}", appointmentId, doctorId);

            // First, try to find by direct doctor assignment
            Appointment appointment = appointmentRepository.findByAppointmentIdAndDoctorId(appointmentId, doctorId);

            if (appointment != null) {
                logger.info("Found appointment by direct doctor assignment");
                return Optional.of(appointment);
            }

            logger.info("Doctor is not directly assigned to appointment. Checking medical order associations...");

            // Try both methods to check for medical order access
            boolean hasMedicalOrderAccess = medicalOrderRepository.existsByAppointmentIdAndDoctorId(appointmentId, doctorId);
            boolean hasMedicalOrderAccessAlt = medicalOrderRepository.checkDoctorHasAccessToAppointment(appointmentId, doctorId);

            logger.info("Medical order access check results - Method 1: {}, Method 2: {}",
                    hasMedicalOrderAccess, hasMedicalOrderAccessAlt);

            // Use either method result (prefer the alternative if available)
            if (hasMedicalOrderAccessAlt || hasMedicalOrderAccess) {
                logger.info("Doctor has access through medical order association");
                appointment = appointmentRepository.findById(appointmentId).orElse(null);

                if (appointment != null) {
                    logger.info("Successfully retrieved appointment through medical order association");
                } else {
                    logger.warn("Appointment ID {} not found in database", appointmentId);
                }

                return Optional.ofNullable(appointment);
            }

            // As a last resort, check if there are any medical orders for this appointment
            List<MedicalOrder> ordersForAppointment = medicalOrderRepository.findByAppointmentIdOrderByOrderDate(appointmentId);
            if (ordersForAppointment != null && !ordersForAppointment.isEmpty()) {
                logger.info("Found {} medical orders for appointment ID {}, but none are associated with doctor ID {}",
                        ordersForAppointment.size(), appointmentId, doctorId);

                // Log doctor IDs of existing orders for debugging
                for (MedicalOrder order : ordersForAppointment) {
                    logger.info("Medical order ID {} for appointment ID {} is associated with doctor ID {}",
                            order.getOrderId(), appointmentId, order.getDoctorId());
                }
            } else {
                logger.info("No medical orders found for appointment ID {}", appointmentId);
            }

            logger.warn("Appointment not found or access denied for appointment ID: {} and doctor ID: {}",
                    appointmentId, doctorId);
            return Optional.empty();
        } catch (Exception e) {
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

    public List<Appointment> searchAppointmentsByPatientName(Integer doctorId, String patientName) {
        try {
            logger.info("Searching appointments for doctor ID: {} with patient name containing: {}", doctorId, patientName);
            return appointmentRepository.findByDoctorIdAndPatientUserFullNameContainingIgnoreCase(doctorId, patientName);
        } catch (DataAccessException e) {
            logger.error("Failed to search appointments for doctor ID: {} with patient name: {}", doctorId, patientName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get the count of active doctors in the system
     * @return the number of active doctors
     */
    public Integer getActiveDoctors() {
        try {
            // Count all doctors in the Doctor table
            return Math.toIntExact(doctorRepository.count());
        } catch (Exception e) {
            logger.error("Error getting active doctors count", e);
            return 0;
        }
    }

    /**
     * Get the count of doctors who are currently online (have a schedule now)
     * @return the number of online doctors
     */
    public Integer getOnlineDoctors() {
        try {
            // For now, since we don't have a ScheduleRepository yet, we'll return a placeholder value
            // In a real implementation, you would query the Schedule table to find doctors with
            // current date matching ScheduleDate and current time between startTime and endTime
            return 5; // Placeholder value

            /*
             * The real implementation would be something like:
             *
             * LocalDate today = LocalDate.now();
             * LocalTime currentTime = LocalTime.now();
             *
             * return Math.toIntExact(scheduleRepository.countByScheduleDateAndStartTimeBeforeAndEndTimeAfter(
             *     today, currentTime, currentTime));
             */
        } catch (Exception e) {
            logger.error("Error getting online doctors count", e);
            return 0;
        }
    }

    /**
     * Get patients with appointments for a doctor with pagination
     * @param doctorId The doctor's ID
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return Page of patients
     */
    public Page<Patient> findByDoctor(Integer doctorId, int page, int size) {
        try {
            logger.info("Fetching paginated patients for doctor ID: {}, page: {}, size: {}", doctorId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            // Use findAll to get all patients, not just those with appointments
            return patientRepository.findAll(pageable);
            // Alternatively, if you want to filter by assigned doctor:
            // return patientRepository.findByAssignedDoctorIdPaginated(doctorId, pageable);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch paginated patients for doctor ID: {}", doctorId, e);
            return Page.empty();
        }
    }

    /**
     * Find all patients with pagination
     */
    public Page<Patient> findAllPatients(int page, int size) {
        try {
            logger.info("Fetching all patients with pagination, page: {}, size: {}", page, size);
            return patientRepository.findAll(PageRequest.of(page, size));
        } catch (DataAccessException e) {
            logger.error("Failed to fetch all patients with pagination", e);
            return Page.empty();
        }
    }

    public List<Patient> findAllPatients() {
        try {
            logger.info("Fetching all patients without pagination");
            return patientRepository.findAll();
        } catch (DataAccessException e) {
            logger.error("Failed to fetch all patients", e);
            return Collections.emptyList();
        }
    }

    /**
     * Find patients by status with pagination
     */
    public Page<Patient> findAllPatientsByStatus(String status, int page, int size) {
        try {
            logger.info("Fetching patients with status: {}, page: {}, size: {}", status, page, size);
            Pageable pageable = PageRequest.of(page, size);
            return patientRepository.findByStatus(status, pageable);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch patients by status: {}", status, e);
            return Page.empty();
        }
    }

    public Page<Patient> findByDoctorAndStatus(Integer doctorId, String status, int page, int size) {
        try {
            logger.info("Fetching patients for doctor ID: {} with status: {}, page: {}, size: {}",
                    doctorId, status, page, size);
            PageRequest pageRequest = PageRequest.of(page, size);
            // Use methods that don't require appointments
            return patientRepository.findByStatus(status, pageRequest);
            // Alternatively, if you want to filter by assigned doctor:
            // return patientRepository.findByAssignedDoctorIdAndStatus(doctorId, status, pageRequest);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch patients for doctor ID: {} with status: {}", doctorId, status, e);
            return Page.empty();
        }
    }
}
