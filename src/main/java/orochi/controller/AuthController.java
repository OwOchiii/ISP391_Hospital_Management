package orochi.controller;

import orochi.model.Users;
import orochi.service.EmailService;
import orochi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final EmailService emailService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final int roleId = 4; // Assuming 1 is the default role ID for new users

    @Autowired
    public AuthController(EmailService emailService, UserService userService) {
        this.emailService = emailService;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password");
            logger.warn("Failed login attempt");
        }

        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully");
            logger.info("User logged out successfully");
        }

        return "auth/login"; // Returns the login.html page
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register"; // Returns the register.html page
    }

    @PostMapping("/register")
    public String handleRegistration(@ModelAttribute Users user,
                                     @RequestParam String fullName,
                                     @RequestParam String email,
                                     @RequestParam String password,
                                     @RequestParam String phoneNumber,
                                     @RequestParam(required = false, defaultValue = "false") boolean agreeTerms,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        // Check if terms were agreed to
        if (!agreeTerms) {
            model.addAttribute("errorMessage", "You must agree to the terms and conditions to register.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phoneNumber", phoneNumber);
            return "auth/register"; // Return to the form with error
        }

        // Check if phone number already exists

        // Set user properties
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(password); // The service will hash this
        user.setPhoneNumber(phoneNumber);
        user.setRoleId(roleId);
        user.setGuest(false); // Assuming new registrations are not guests by default

        try {
            userService.registerNewUser(user);
            logger.info("User registered successfully: {}", email);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/auth/login"; // Redirect to login page after registration
        } catch (RuntimeException e) {
            logger.warn("Registration failed for email {}: {}", email, e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phoneNumber", phoneNumber);
            return "auth/register"; // Return to the form with error
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email, Model model) {
        try {
            // Generate token
            String token = userService.generatePasswordResetToken(email);

            // Create reset URL (using hardcoded domain for simplicity)
            String resetUrl = "http://localhost:8090/auth/reset-password?token=" + token + "&email=" + email;

            // Log the reset URL to console for development purposes
            //logger.info("PASSWORD RESET LINK: {}", resetUrl);
            emailService.sendPasswordResetEmail(email, resetUrl);
            // Add success message to model
            model.addAttribute("successMessage", "Password reset instructions have been sent to your email.");
            model.addAttribute("email", email);

            return "auth/forgot-password";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error: " + e.getMessage());
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(
            @RequestParam String token,
            @RequestParam String email,
            Model model) {

        logger.info("Reset password page requested with token: {} and email: {}", token, email);

        // Validate token
        boolean isValid = userService.validatePasswordResetToken(token, email);
        logger.info("Token validation result: {}", isValid);

        if (!isValid) {
            model.addAttribute("errorMessage", "Invalid or expired password reset link.");
            return "auth/reset-password";
        }

        model.addAttribute("token", token);
        model.addAttribute("email", email);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam String token,
            @RequestParam String email,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {

        // Validate inputs
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            model.addAttribute("token", token);
            model.addAttribute("email", email);
            return "auth/reset-password";
        }

        try {
            userService.resetPassword(token, email, newPassword);
            model.addAttribute("successMessage", "Your password has been reset successfully. You can now log in with your new password.");
            return "auth/reset-password";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", "Error: " + e.getMessage());
            model.addAttribute("token", token);
            model.addAttribute("email", email);
            return "auth/reset-password";
        }
    }
}
