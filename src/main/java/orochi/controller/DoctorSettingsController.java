package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.Doctor;
import orochi.model.Notification;
import orochi.model.Specialization;
import orochi.model.Users;
import orochi.repository.DoctorRepository;
import orochi.repository.NotificationRepository;
import orochi.repository.UserRepository;
import orochi.config.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/doctor")
public class DoctorSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorSettingsController.class);

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Display the settings page for a doctor
     */
    @GetMapping("/settings")
    public String getSettings(@RequestParam(required = false) Integer doctorId,
                              Model model,
                              Authentication authentication) {
        try {
            // If doctorId is not provided, try to get it from the authentication
            if (doctorId == null && authentication != null) {
                logger.info("No doctorId provided, attempting to retrieve from authenticated user");

                Object principal = authentication.getPrincipal();

                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    doctorId = userDetails.getDoctorId();

                    if (doctorId == null) {
                        logger.error("No doctorId found in CustomUserDetails for user: {}", userDetails.getUsername());
                        model.addAttribute("errorMessage", "Doctor ID is required. Please contact support.");
                        return "error";
                    }

                    logger.info("Retrieved doctorId {} from authentication", doctorId);
                } else {
                    logger.error("Authentication principal is not of CustomUserDetails type: {}",
                            principal != null ? principal.getClass().getName() : "null");
                    model.addAttribute("errorMessage", "Authentication error. Please log in again.");
                    return "error";
                }
            }

            if (doctorId == null) {
                logger.error("No doctorId provided and could not be determined from authentication");
                model.addAttribute("errorMessage", "Doctor ID is required");
                return "error";
            }

            logger.info("Loading settings for doctor ID: {}", doctorId);
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);

            // Check if doctor is null before accessing its properties
            if (doctor == null) {
                logger.error("Doctor not found for ID: {}", doctorId);
                model.addAttribute("errorMessage", "Doctor not found");
                return "error";
            }

            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(doctor.getUserId());
            if (notifications == null) {
                notifications = new ArrayList<>();
            }

            // Count unread notifications
            long unreadNotifications = notifications.stream()
                    .filter(notification -> !notification.isRead())
                    .count();

            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadNotifications", unreadNotifications);

            Users user = doctor.getUser();
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("doctorName", user.getFullName());
            model.addAttribute("doctorEmail", user.getEmail());
            model.addAttribute("doctorPhone", user.getPhoneNumber());

            // Handle doctor specialization(s)
            String specializationText = "Not specified";
            if (doctor.getSpecializations() != null && !doctor.getSpecializations().isEmpty()) {
                specializationText = doctor.getSpecializations().stream()
                    .map(Specialization::getSpecName)
                    .collect(Collectors.joining(", "));
            }
            model.addAttribute("doctorSpecialization", specializationText);

            // Check if we need to display the security tab (e.g., after password change)
            String anchor = (String) model.asMap().get("anchor");
            if (anchor != null && anchor.equals("security")) {
                model.addAttribute("activeTab", "security");
            }

            return "doctor/settings";
        } catch (Exception e) {
            logger.error("Error loading doctor settings page", e);
            model.addAttribute("errorMessage", "An error occurred while loading settings");
            return "error";
        }
    }

    /**
     * Handle password change requests using traditional MVC approach
     */
    @PostMapping("/settings/change-password")
    public String changePassword(@RequestParam Integer doctorId,
                                @RequestParam String currentPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            // Validate input data
            if (currentPassword == null || newPassword == null || confirmPassword == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "All password fields are required");
                return "redirect:/doctor/settings?doctorId=" + doctorId;
            }

            // Check if passwords match
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match");
                return "redirect:/doctor/settings?doctorId=" + doctorId;
            }

            // Check password length
            if (newPassword.length() < 8) {
                redirectAttributes.addFlashAttribute("errorMessage", "New password must be at least 8 characters long");
                return "redirect:/doctor/settings?doctorId=" + doctorId;
            }

            // Get the doctor and associated user
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
            if (doctor == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Doctor not found");
                return "redirect:/doctor/settings?doctorId=" + doctorId;
            }

            Users user = doctor.getUser();
            if (user == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User account not found");
                return "redirect:/doctor/settings?doctorId=" + doctorId;
            }

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect");
                return "redirect:/doctor/settings?doctorId=" + doctorId;
            }

            if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("errorMessage", "New password cannot be the same as the current password");
                return "redirect:/doctor/settings?doctorId=" + doctorId;
            }

            // Update password
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);




            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
            return "redirect:/doctor/settings?doctorId=" + doctorId + "#security";
        } catch (Exception e) {
            logger.error("Error changing password", e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while changing password");
            return "redirect:/doctor/settings?doctorId=" + doctorId;
        }
    }
}
