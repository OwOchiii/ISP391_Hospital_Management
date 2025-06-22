package orochi.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.Users;
import orochi.repository.AppointmentRepository;
import orochi.repository.PatientRepository;
import orochi.repository.ReceptionistRepository;
import orochi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import orochi.service.UserService;

import java.util.List;
import java.util.Optional;

@Service("receptionistService")
public class ReceptionistService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceptionistService.class);

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ReceptionistRepository receptionistRepository;

    public ReceptionistService(
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            ReceptionistRepository receptionistRepository) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.receptionistRepository = receptionistRepository;
    }

    // Fetch all appointments for scheduling purposes
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Register a new patient
    public Users registerPatient(Users user) {
        Users patientRole = receptionistRepository.findByEmail("PATIENT")
                .orElseThrow(() -> new RuntimeException("Patient role not found"));
        user.setRoleId(patientRole.getRoleId());
        user.setGuest(false);
        user.setStatus("Active");
        return userRepository.save(user);
    }

    // Fetch all patients for registration management
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    // Check if the user is a Receptionist
    public boolean isReceptionist(Users user) {
        return user.getRole().getRoleName().equals("RECEPTIONIST");
    }

    // Update appointment status
    public boolean updateAppointmentStatus(Integer appointmentId, String status) {
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        if (appointmentOptional.isPresent()) {
            Appointment appointment = appointmentOptional.get();
            if (!isValidAppointmentStatus(status)) {
                throw new IllegalArgumentException("Invalid appointment status: " + status);
            }
            appointment.setStatus(status);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

    private boolean isValidAppointmentStatus(String status) {
        return status != null && (status.equals("Scheduled") || status.equals("Completed") ||
                status.equals("Cancel") || status.equals("Pending"));
    }

    // Fetch all Receptionists with pagination and filtering
    public Page<Users> getAllReceptionists(Pageable pageable) {
        return receptionistRepository.findAllReceptionistsFiltered(null, null, pageable);
    }

    public Page<Users> getAllReceptionists(String search, String statusFilter, Pageable pageable) {
        LOGGER.info("Search parameter: {}, StatusFilter: {}", search, statusFilter);
        Page<Users> receptionistPage = receptionistRepository.findAllReceptionistsFiltered(search, statusFilter, pageable);
        LOGGER.info("Number of receptionists returned: {}", receptionistPage.getTotalElements());
        return receptionistPage;
    }

    // Triển khai các phương thức từ UserService chưa có
    @Override
    public Integer getTotalUsers() {
        throw new UnsupportedOperationException("Not implemented in ReceptionistService");
    }

    @Override
    public Integer getGuestUsers() {
        throw new UnsupportedOperationException("Not implemented in ReceptionistService");
    }

    @Override
    public Integer getNewUsersToday() {
        throw new UnsupportedOperationException("Not implemented in ReceptionistService");
    }

    @Override
    public Integer getGrowthPercentage() {
        throw new UnsupportedOperationException("Not implemented in ReceptionistService");
    }

    @Override
    public Users registerNewUser(Users user) {
        throw new UnsupportedOperationException("Not implemented in ReceptionistService");
    }

    @Override
    public String generatePasswordResetToken(String email) {
        throw new UnsupportedOperationException("Not implemented in ReceptionistService");
    }

    @Override
    public boolean validatePasswordResetToken(String token, String email) {
        throw new UnsupportedOperationException("Not implemented in ReceptionistService");
    }

    @Override
    public void resetPassword(String token, String email, String newPassword) {
        throw new UnsupportedOperationException("Not implemented in ReceptionistService");
    }

    @Override
    public Optional<Users> findById(Integer userId) {
        return receptionistRepository.findById(userId);
    }

    @Override
    public Users save(Users user) {
        return receptionistRepository.save(user);
    }
}