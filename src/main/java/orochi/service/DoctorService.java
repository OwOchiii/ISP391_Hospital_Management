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
import orochi.model.Doctor;
import orochi.model.DoctorForm;
import orochi.model.Patient;
import orochi.model.Users;
import orochi.repository.AppointmentRepository;
import orochi.repository.DoctorRepository;
import orochi.repository.PatientContactRepository;
import orochi.repository.PatientRepository;
import orochi.repository.UserRepository;

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
    private final UserRepository userRepository;
    @Getter
    private final PatientContactRepository patientContactRepository;

    @Autowired
    public DoctorService(AppointmentRepository appointmentRepository,
                         PatientRepository patientRepository,
                         DoctorRepository doctorRepository,
                         UserRepository userRepository,
                         PatientContactRepository patientContactRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
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
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
            logger.info("Fetching today's appointments for doctor ID: {} (from {} to {})", doctorId, startOfDay, endOfDay);
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



    public List<Doctor> getAllDoctors() {
        try {
            logger.info("Fetching all doctors");
            return doctorRepository.findAll();
        } catch (DataAccessException e) {
            logger.error("Error fetching all doctors", e);
            return Collections.emptyList();
        }
    }

    public Doctor getDoctorById(int id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found for ID: " + id));
    }

    public Doctor saveDoctor(Doctor doctor) {
        try {
            logger.info("Saving doctor {}", doctor);
            return doctorRepository.save(doctor);
        } catch (DataAccessException e) {
            logger.error("Error saving doctor {}", doctor, e);
            throw e;
        }
    }

    public void toggleDoctorLock(int id) {
        Doctor doctor = getDoctorById(id);
        Users user = doctor.getUser();
        String newStatus = "LOCKED".equals(user.getStatus()) ? "ACTIVE" : "LOCKED";
        user.setStatus(newStatus);
        userRepository.save(user);
        logger.info("Doctor user {} status toggled to {}", user.getUserId(), newStatus);
    }

    // --------------------------------
    // Admin search & filter methods
    // --------------------------------
    public long getActiveDoctors() {
        try {
            logger.info("Counting active doctors");
            return doctorRepository.count();
        } catch (DataAccessException e) {
            logger.error("Error counting doctors", e);
            return 0;
        }
    }

    public int getOnlineDoctors() {
        try {
            // TODO: thay bằng logic thực tế (vd: đếm bác sĩ có lịch hẹn sắp tới,…)
            return 0;
        } catch (Exception e) {
            logger.error("Error getting online doctors", e);
            return 0;
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

    public List<Doctor> searchDoctors(String search, String statusFilter) {
        String trimmed = (search != null && !search.isBlank()) ? search.trim() : null;
        String status = (statusFilter != null && !statusFilter.isBlank()) ? statusFilter.trim() : null;
        try {
            logger.info("Searching doctors with keyword='{}' and status='{}'", trimmed, status);
            return doctorRepository.searchDoctors(trimmed, status);
        } catch (DataAccessException e) {
            logger.error("Error searching doctors with keyword='{}' and status='{}'", trimmed, status, e);
            return Collections.emptyList();
        }
    }

    public DoctorForm loadForm(int doctorId) {
        Doctor d = getDoctorById(doctorId);
        Users u = d.getUser();
        DoctorForm form = new DoctorForm();
        form.setDoctorId(d.getDoctorId());
        form.setUserId(u.getUserId());
        form.setFullName(u.getFullName());
        form.setEmail(u.getEmail());
        form.setPhoneNumber(u.getPhoneNumber());
        form.setStatus(u.getStatus());
        form.setBioDescription(d.getBioDescription());
        return form;
    }

    public void saveFromForm(DoctorForm form) {
        // 1) xử lý Users
        Users u;
        if (form.getUserId() != null) {
            u = userRepository.findById(form.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + form.getUserId()));
        } else {
            u = new Users();
            // thiết lập các mặc định: roleId, isGuest, createdAt,...
            u.setRoleId(2);           // giả sử 2 = ROLE_DOCTOR
            u.setGuest(false);
            u.setCreatedAt(LocalDateTime.now());
        }
        u.setFullName(form.getFullName());
        u.setEmail(form.getEmail());
        u.setPhoneNumber(form.getPhoneNumber());
        u.setStatus(form.getStatus());
        userRepository.save(u);

        // 2) xử lý Doctor
        Doctor d;
        if (form.getDoctorId() != null) {
            d = getDoctorById(form.getDoctorId());
        } else {
            d = new Doctor();
        }
        d.setUserId(u.getUserId());
        d.setBioDescription(form.getBioDescription());
        doctorRepository.save(d);
    }

    /**
     * Get doctor by ID
     * @param doctorId The doctor's ID
     * @return The doctor object
     * @throws IllegalArgumentException if doctor not found
     */
    public Doctor getDoctorById(Integer doctorId) {
        logger.info("Getting doctor by ID: {}", doctorId);
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with ID: " + doctorId));
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

    /**
     * Toggle the lock status of a doctor's account
     * @param doctorId The doctor's ID
     */
    public void toggleDoctorLock(Integer doctorId) {
        try {
            logger.info("Toggling lock status for doctor ID: {}", doctorId);

            Doctor doctor = getDoctorById(doctorId);
            Users user = doctor.getUser();

            if (user == null) {
                logger.error("Cannot toggle lock status: No user found for doctor ID: {}", doctorId);
                throw new IllegalStateException("No user account found for this doctor");
            }

            // Toggle between Active and Inactive status (according to SQL schema constraint)
            if ("Active".equals(user.getStatus())) {
                user.setStatus("Inactive");
                logger.info("Doctor ID: {} has been deactivated (set to Inactive)", doctorId);
            } else {
                user.setStatus("Active");
                logger.info("Doctor ID: {} has been activated (set to Active)", doctorId);
            }

            userRepository.save(user);
        } catch (Exception e) {
            logger.error("Error toggling lock status for doctor ID: {}", doctorId, e);
            throw new RuntimeException("Failed to toggle doctor lock status: " + e.getMessage(), e);
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
}