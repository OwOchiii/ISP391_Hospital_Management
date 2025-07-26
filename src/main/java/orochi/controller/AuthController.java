package orochi.controller;

import jakarta.servlet.http.HttpServletRequest;
import orochi.model.Users;
import orochi.service.CaptchaService;
import orochi.service.EmailService;
import orochi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    private final CaptchaService captchaService; // Assuming you have a CaptchaService for CAPTCHA validation
    private final EmailService emailService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final int roleId = 4; // Assuming 4 is the default role ID for new users

    @Value("${google.recaptcha.key.site}")
    private String recaptchaSiteKey;

    @Autowired
    public AuthController(CaptchaService captchaService, EmailService emailService,@Qualifier("userServiceImpl") UserService userService) {
        this.captchaService = captchaService;
        this.emailService = emailService;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "errorType", required = false) String errorType,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "captchaError", required = false) String captchaError,
            HttpServletRequest request,
            Model model) {

        if (error != null) {
            // Handle specific error types from our custom failure handler
            if (errorType != null) {
                switch (errorType) {
                    case "account_locked":
                        model.addAttribute("errorMessage", "Your account is currently locked. Please contact your administrator for support.");
                        logger.warn("Login attempt for locked account");
                        break;
                    case "credentials_expired":
                        model.addAttribute("errorMessage", "Your password has expired. Please contact your administrator for support.");
                        logger.warn("Login attempt with expired credentials");
                        break;
                    case "bad_credentials":
                        model.addAttribute("errorMessage", "Invalid email or password");
                        logger.warn("Failed login attempt - bad credentials");
                        break;
                    case "authentication_failed":
                    default:
                        model.addAttribute("errorMessage", "Invalid email or password");
                        logger.warn("Failed login attempt - authentication failed");
                        break;
                }
            } else {
                // Fallback to original logic for backwards compatibility
                Exception exception = (Exception) request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
                if (exception != null) {
                    String exceptionMessage = exception.getMessage();
                    String exceptionClass = exception.getClass().getSimpleName();

                    logger.info("Authentication exception: {} - {}", exceptionClass, exceptionMessage);

                    // Check for various account locked scenarios
                    if (exceptionMessage != null && exceptionMessage.contains("Account is locked")) {
                        model.addAttribute("errorMessage", "Your account is currently locked. Please contact your administrator for support.");
                        logger.warn("Login attempt for locked account");
                    } else if ("AccountExpiredException".equals(exceptionClass) ||
                              "LockedException".equals(exceptionClass) ||
                              "DisabledException".equals(exceptionClass)) {
                        model.addAttribute("errorMessage", "Your account is currently locked. Please contact your administrator for support.");
                        logger.warn("Login attempt for locked account - Exception type: {}", exceptionClass);
                    } else if ("CredentialsExpiredException".equals(exceptionClass)) {
                        model.addAttribute("errorMessage", "Your password has expired. Please contact your administrator for support.");
                        logger.warn("Login attempt with expired credentials");
                    } else {
                        model.addAttribute("errorMessage", "Invalid email or password");
                        logger.warn("Failed login attempt - Exception: {} - {}", exceptionClass, exceptionMessage);
                    }
                } else {
                    model.addAttribute("errorMessage", "Invalid email or password");
                    logger.warn("Failed login attempt - No exception details available");
                }
            }
        }

        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully");
            logger.info("User logged out successfully");
        }

        if (captchaError != null) {
            // Build a more descriptive error message based on the error code
            String errorMessage;
            switch (captchaError) {
                case "EMPTY_RESPONSE":
                    errorMessage = "Please complete the reCAPTCHA verification";
                    break;
                case "VALIDATION_FAILED":
                    errorMessage = "reCAPTCHA verification failed - please try again";
                    break;
                default:
                    errorMessage = "reCAPTCHA error: " + captchaError;
            }
            model.addAttribute("errorMessage", errorMessage);

            // Add debug information if available
            String debugInfo = (String) request.getSession().getAttribute("CAPTCHA_DEBUG_INFO");
            if (debugInfo != null) {
                logger.debug("CAPTCHA debug info: {}", debugInfo);
                model.addAttribute("captchaDebugInfo", debugInfo);
            }

            logger.warn("CAPTCHA verification failed with code: {}", captchaError);

            // Get the username from session if it was saved during CAPTCHA error
            String lastUsername = (String) request.getSession().getAttribute("LAST_USERNAME");
            if (lastUsername != null) {
                model.addAttribute("lastUsername", lastUsername);
                // Remove the attribute once used
                request.getSession().removeAttribute("LAST_USERNAME");
            }

            // Clear the CAPTCHA error flags
            request.getSession().removeAttribute("CAPTCHA_ERROR");
            request.getSession().removeAttribute("CAPTCHA_DEBUG_INFO");
        }

        // Add the site key to the model using the injected property
        model.addAttribute("captchaSiteKey", recaptchaSiteKey);
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("captchaSiteKey", recaptchaSiteKey);
        return "auth/register"; // Returns the register.html page
    }

    @PostMapping("/register")
    public String handleRegistration(@ModelAttribute Users user,
                                     @RequestParam String fullName,
                                     @RequestParam String email,
                                     @RequestParam String password,
                                     @RequestParam String phoneNumber,
                                     @RequestParam(required = false, defaultValue = "false") boolean agreeTerms,
                                     @RequestParam("g-recaptcha-response") String captchaResponse,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        if (!captchaService.validateCaptcha(captchaResponse)) {
            model.addAttribute("errorMessage", "Please verify you are not a robot");
            model.addAttribute("captchaSiteKey", recaptchaSiteKey);
            return "auth/register";
        }

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
        user.setStatus("Active");

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
            logger.info("PASSWORD RESET LINK: {}", resetUrl);
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
