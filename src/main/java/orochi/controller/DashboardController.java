package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.Doctor;
import orochi.model.Patient;
import orochi.model.Role;
import orochi.model.Users;
import orochi.repository.DoctorRepository;
import orochi.repository.PatientRepository;
import orochi.repository.UserRepository;

import java.util.Optional;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    // Role constants to match your database values
    private static final String ROLE_DOCTOR = "Doctor";
    private static final String ROLE_PATIENT = "Patient";
    private static final String ROLE_ADMIN = "Admin";

    @GetMapping("/dashboard")
    public String redirectToDashboard(Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            logger.warn("No authentication found, redirecting to login");
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        logger.info("User {} logged in, redirecting to appropriate dashboard", username);

        // First, find the user by email (assuming email is used as username)
        Optional<Users> userOpt = userRepository.findByEmail(username);

        if (userOpt.isEmpty()) {
            logger.error("User not found for email: {}", username);
            redirectAttributes.addFlashAttribute("errorMessage", "User not found. Please log in again.");
            return "redirect:/auth/login";
        }

        Users user = userOpt.get();
        Role role = user.getRole();

        if (role == null) {
            logger.error("Role not found for user: {}", username);
            redirectAttributes.addFlashAttribute("errorMessage", "Role not found. Please contact administrator.");
            return "redirect:/auth/login";
        }

        String roleName = role.getRoleName();
        logger.info("User {} has role: {}", username, roleName);

        // Redirect based on role
        if (ROLE_ADMIN.equalsIgnoreCase(roleName)) {
            logger.info("Redirecting to admin dashboard");
            return "redirect:/admin/dashboard";
        }
        else if (ROLE_DOCTOR.equalsIgnoreCase(roleName)) {
            // Find the doctor record
            Optional<Doctor> doctorOpt = doctorRepository.findByUserId(user.getUserId());

            if (doctorOpt.isPresent()) {
                Integer doctorId = doctorOpt.get().getUserId();
                logger.info("Redirecting to doctor dashboard with ID: {}", doctorId);
                redirectAttributes.addAttribute("doctorId", doctorId);
                return "redirect:/doctor/dashboard";
            } else {
                logger.error("Doctor record not found for user ID: {}", user.getUserId());
                redirectAttributes.addFlashAttribute("errorMessage", "Doctor record not found. Please contact administrator.");
                return "redirect:/error";
            }
        }
        else if (ROLE_PATIENT.equalsIgnoreCase(roleName)) {
            // Find the patient record
            Optional<Patient> patientOpt = patientRepository.findById(user.getUserId());

            if (patientOpt.isPresent()) {
                Integer patientId = patientOpt.get().getPatientId();
                logger.info("Redirecting to patient dashboard with ID: {}", patientId);
                redirectAttributes.addAttribute("patientId", patientId);
                return "redirect:/patient/dashboard";
            } else {
                logger.error("Patient record not found for user ID: {}", user.getUserId());
                redirectAttributes.addFlashAttribute("errorMessage", "Patient record not found. Please contact administrator.");
                return "redirect:/error";
            }
        }
        else {
            // For other roles or if role is not recognized
            logger.warn("Unrecognized role '{}' for user {}, redirecting to home", roleName, username);
            return "redirect:/";
        }
    }
}