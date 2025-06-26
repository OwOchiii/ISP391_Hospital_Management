package orochi.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import orochi.config.CustomUserDetails;
import orochi.model.Users;
import orochi.repository.UserRepository;
import orochi.service.impl.ReceptionistService;
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

    @GetMapping("/dashboard")
    public String receptionistDashboard(Model model, Authentication authentication) {
        // Ensure authentication is not null
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Extract CustomUserDetails from the principal
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            return "redirect:/error";
        }

        // Extract userId from CustomUserDetails
        CustomUserDetails userDetails = (CustomUserDetails) principal;
        Integer userId = userDetails.getUserId();

        // Fetch the Users entity from the database
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Verify the user is a Receptionist
        if (!user.getRole().getRoleName().equals("RECEPTIONIST")) {
            return "redirect:/error";
        }

        // Add user, appointments, and patients to the model for display
        model.addAttribute("user", user);
        model.addAttribute("appointments", receptionistService.getAllAppointments());
        model.addAttribute("patients", receptionistService.getAllPatients());
        model.addAttribute("newUser", new Users());
        model.addAttribute("newPatients", receptionistService.newPatients());
        model.addAttribute("ourDoctor", receptionistService.ourDoctors());
        model.addAttribute("totalAppointments", receptionistService.totalAppointment());
        model.addAttribute("activeStaff", receptionistService.activeStaff());
        return "Receptionists/dashboard";
    }

    @GetMapping("/new_appointment")
    public String newAppointment(Authentication authentication) {
        // Ensure authentication is not null
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        //getData, action ...
        return "Receptionists/new_appointment";
    }


    @GetMapping("/appointments")
    public String appointments(Authentication authentication) {
        // Ensure authentication is not null
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        return "Receptionists/appointments";
    }

    @GetMapping("/doctors")
    public String doctors(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        return "Receptionists/doctors";
    }

    @GetMapping("/patients")
    public String patients(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        return "Receptionists/patients";
    }
    @GetMapping("/payments")
    public String payments(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        return "Receptionists/payments";
    }
    @GetMapping("/reports")
    public String reports(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        return "Receptionists/reports";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        return "Receptionists/profile";
    }

    @GetMapping("/settings")
    public String settings(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        return "Receptionists/settings";
    }



    @PostMapping("/register")
    public String registerPatient(@ModelAttribute("newUser") Users user) {
        receptionistService.registerPatient(user);
        return "redirect:/receptionist/dashboard";
    }

    @GetMapping("/patient_register")
    public String patientRegister(Authentication authentication) {
        // Ensure authentication is not null
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Get patient registration form data, initialize registration process ...
        return "Receptionists/patient_register";
    }

    @GetMapping("/payment_history")
    public String paymentHistory(Authentication authentication) {
        // Ensure authentication is not null
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Fetch payment history data, filter or paginate payments ...
        return "Receptionists/payment_history";
    }

    @GetMapping("/view_payment_details")
    public String viewPaymentDetails(Authentication authentication) {
        // Ensure authentication is not null
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Fetch payment details based on payment ID from request parameter ...
        return "Receptionists/view_payment_details";
    }

    @GetMapping("/view_patient_details")
    public String viewPatientDetails(Authentication authentication) {
        // Ensure authentication is not null
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Fetch patient details based on patient ID from request parameter ...
        return "Receptionists/view_patient_details";
    }

    @GetMapping("/pay_invoice")
    public String payInvoice(Authentication authentication) {
        // Ensure authentication is not null
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Fetch invoice details based on invoice ID from request parameter ...
        return "Receptionists/pay_invoice";
    }

    @GetMapping("/patientStatus")
    public ResponseEntity<?> getPatientStatusChartData(String period) {
        try {
            Map<String, Object> data = receptionistService.getPatientStatusChartData(period);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching patient status: " + e.getMessage());
        }
    }

    @GetMapping("/appointmentRequest")
    public ResponseEntity<?> getAppointmentTableData(){
        try {
            List<Map<String, Object>> appointmentTableData = receptionistService.getAppointmentTableData();
            return ResponseEntity.ok(appointmentTableData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving appointment table data: " + e.getMessage());
        }
    }

    @PostMapping("/confirmAppointment")
    public ResponseEntity<?> updateAppointmentStatus(
            @RequestParam int appointId
    ){
        try {
            // Update status to "Scheduled"
            boolean isSuccess = receptionistService.updateAppointmentStatus(appointId, "Scheduled");
            if(isSuccess) {
                return ResponseEntity.ok("Update successfully");
            } else {
                return ResponseEntity.badRequest().body("Update failed");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error update appointment status: " + e.getMessage());
        }
    }

}