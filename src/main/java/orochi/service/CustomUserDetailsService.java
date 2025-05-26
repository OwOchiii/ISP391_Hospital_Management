package orochi.service;

import orochi.model.Doctor;
import orochi.model.Patient;
import orochi.model.Users;
import orochi.repository.DoctorRepository;
import orochi.repository.PatientRepository;
import orochi.repository.UserRepository;
import orochi.config.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository,
                                    DoctorRepository doctorRepository,
                                    PatientRepository patientRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Get the exact role name from the user's role
        String authority;
        if (user.getRole() != null) {
            authority = user.getRole().getRoleName();
            logger.info("User {} has role in DB: {}", email, authority);
        } else {
            authority = "USER"; // Default role
            logger.warn("User {} has no role assigned, using default: {}", email, authority);
        }

        // Find doctor or patient ID if applicable
        Integer doctorId = null;
        Integer patientId = null;

        if ("DOCTOR".equals(authority)) {
            Optional<Doctor> doctor = doctorRepository.findByUserId(user.getUserId());
            if (doctor.isPresent()) {
                doctorId = doctor.get().getUserId();
                logger.info("Found doctorId {} for user {}", doctorId, email);
            } else {
                logger.warn("No doctor record found for user {} with ID {}", email, user.getUserId());
            }
        } else if ("PATIENT".equals(authority)) {
            Optional<Patient> patient = patientRepository.findById(user.getUserId());
            if (patient.isPresent()) {
                patientId = patient.get().getPatientId();
                logger.info("Found patientId {} for user {}", patientId, email);
            } else {
                logger.warn("No patient record found for user {} with ID {}", email, user.getUserId());
            }
        }

        SimpleGrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority);

        return new CustomUserDetails(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(grantedAuthority),
                user.getUserId(),
                doctorId,
                patientId
        );
    }
}