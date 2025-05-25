package orochi.controller;

import orochi.model.Users;
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

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final int roleId = 4; // Assuming 1 is the default role ID for new users

    @Autowired
    public AuthController(UserService userService) {
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
                                     RedirectAttributes redirectAttributes) {
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
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/register"; // Redirect back to registration page with error
        }
    }
}
