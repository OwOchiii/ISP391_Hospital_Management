package orochi.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import orochi.config.CustomUserDetails;
import orochi.model.Users;
import orochi.repository.UserRepository;
import orochi.service.impl.ReceptionistService;

@Controller
@RequestMapping("/receptionist")
public class ReceptionistController {

    private final ReceptionistService receptionistService;
    private final UserRepository userRepository;

    public ReceptionistController(ReceptionistService receptionistService, UserRepository userRepository) {
        this.receptionistService = receptionistService;
        this.userRepository = userRepository;
    }

    private Users getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Integer userId = userDetails.getUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    @GetMapping("/dashboard")
    public String receptionistDashboard(Model model, Authentication authentication) {
        Users user = getAuthenticatedUser(authentication);
        if (!user.getRole().getRoleName().equals("RECEPTIONIST")) {
            return "redirect:/error";
        }
        model.addAttribute("user", user);
        model.addAttribute("appointments", receptionistService.getAllAppointments());
        model.addAttribute("patients", receptionistService.getAllPatients());
        model.addAttribute("newPatientsCount", receptionistService.countNewPatients());
        model.addAttribute("doctorsCount", receptionistService.countDoctors());
        model.addAttribute("totalIncome", receptionistService.getTotalIncome());
        model.addAttribute("activeStaffCount", receptionistService.countActiveStaff());
        model.addAttribute("newUser", new Users());
        return "Receptionists/dashboard";
    }

    @GetMapping("/appointments")
    public String appointments(Model model, Authentication authentication) {
        Users user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("inProgressAppointments", receptionistService.getAppointmentsByStatus("Scheduled"));
        model.addAttribute("completedAppointments", receptionistService.getAppointmentsByStatus("Completed"));
        model.addAttribute("rejectedAppointments", receptionistService.getAppointmentsByStatus("Cancel"));
        return "appointments";
    }

    @GetMapping("/doctors")
    public String doctors(Model model, Authentication authentication) {
        Users user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("doctors", receptionistService.getAllDoctors());
        return "doctors";
    }

    @GetMapping("/patients")
    public String patients(Model model, Authentication authentication) {
        Users user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("patients", receptionistService.getAllPatients());
        return "patients";
    }

    @GetMapping("/reports")
    public String reports(Model model, Authentication authentication) {
        Users user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("appointmentStatsMonth", receptionistService.countAppointmentsByPeriod("month"));
        model.addAttribute("appointmentStatsYear", receptionistService.countAppointmentsByPeriod("year"));
        model.addAttribute("incomeStatsMonth", receptionistService.getFinancialStatsByPeriod("month"));
        model.addAttribute("incomeStatsYear", receptionistService.getFinancialStatsByPeriod("year"));
        return "reports";
    }

    @GetMapping("/new-appointment")
    public String newAppointment(Model model, Authentication authentication) {
        Users user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("appointmentForm", new AppointmentForm()); // Custom form class
        return "new_appointment";
    }

    @GetMapping("/patient-details/{id}")
    public String patientDetails(@PathVariable("id") Integer patientId, Model model, Authentication authentication) {
        Users user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("patient", receptionistService.getPatientById(patientId));
        return "patient_details";
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        Users user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/settings")
    public String settings(Model model, Authentication authentication) {
        Users user = getAuthenticatedUser(authentication);
        model.addAttribute("user", user);
        return "settings";
    }

    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }

    @PostMapping("/register")
    public String registerPatient(@ModelAttribute("newUser") Users user) {
        receptionistService.registerPatient(user);
        return "redirect:/receptionist/dashboard";
    }
}