package orochi.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.config.CustomUserDetails;
import orochi.dto.AppointmentDTO;
import orochi.dto.AppointmentFormDTO;
import orochi.dto.SpecializationDTO;
import orochi.model.Appointment;
import orochi.model.Doctor;
import orochi.model.Patient;
import orochi.model.Specialization;
import orochi.model.Users;
import orochi.repository.PatientRepository;
import orochi.repository.UserRepository;
import orochi.service.AppointmentService;
import orochi.service.SpecializationService;
import orochi.service.impl.ReceptionistService;

@Controller
@RequestMapping("/receptionist")
public class ReceptionistController {

    private final ReceptionistService receptionistService;
    private final UserRepository userRepository;
    private final SpecializationService specializationService;
    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;

    @Autowired
    public ReceptionistController(ReceptionistService receptionistService, UserRepository userRepository, SpecializationService specializationService, AppointmentService appointmentService, PatientRepository patientRepository) {
        this.receptionistService = receptionistService;
        this.userRepository = userRepository;
        this.specializationService = specializationService;
        this.appointmentService = appointmentService;
        this.patientRepository = patientRepository;
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
    public String newAppointment(Authentication authentication,Model model) {
        // Ensure authentication is not null
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        List<SpecializationDTO> specializations = specializationService.getAll().stream()
                .map(spec -> SpecializationDTO.builder()
                        .specId(spec.getSpecId())
                        .specName(spec.getSpecName())
                        .price(spec.getPrice())
                        .build())
                .toList();

        model.addAttribute("specializations", specializations);

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


    @GetMapping("/appointment-details")
    public String appointment_details(@RequestParam("id") Integer appointmentId, Model model, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            // Không cần ép kiểu về int nếu appointmentId đã là Long
            AppointmentDTO appointment = appointmentService.getAppointmentDetails(appointmentId);
            if (appointment == null) {
                return "error/404";
            }

            model.addAttribute("appointment", appointment);
            // Extract specializations from the appointment DTO and add to model
            if (appointment.getSpecializations() != null) {
                model.addAttribute("specializations", appointment.getSpecializations());
            }
            return "Receptionists/appointment-details";
        } catch (Exception e) {
            // In lỗi ra log hoặc console để kiểm tra
            System.err.println("Lỗi khi xử lý yêu cầu xem chi tiết appointment:");
            e.printStackTrace(); // In đầy đủ stack trace để dễ debug

            // Có thể chuyển hướng tới trang lỗi chung
            model.addAttribute("errorMessage", e.getMessage());
            return "error/500"; // hoặc trang lỗi tùy chỉnh bạn có
        }
    }

    @GetMapping("/appointment-details/{appointmentId}")
    public String appointmentDetails(@PathVariable Integer appointmentId, Model model) {
        try {
            // Fetch appointment details
            AppointmentDTO appointment = appointmentService.getAppointmentDetails(appointmentId);
            if (appointment == null) {
                model.addAttribute("errorMessage", "Appointment not found");
                return "redirect:/receptionist/appointments";
            }

            // Add appointment data to model
            model.addAttribute("appointment", appointment);

            // Add specializations for dropdown
            List<Specialization> specializations = specializationService.getAllSpecializations();
            model.addAttribute("specializations", specializations);

            return "Receptionists/appointment-details";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading appointment details");
            return "redirect:/receptionist/appointments";
        }
    }

    @GetMapping("/api/appointment/{appointmentId}")
    @ResponseBody
    public ResponseEntity<AppointmentDTO> getAppointmentApi(@PathVariable Integer appointmentId) {
        try {
            AppointmentDTO appointment = appointmentService.getAppointmentDetails(appointmentId);
            if (appointment != null) {
                return ResponseEntity.ok(appointment);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
    @PostMapping("/api/appointment/update")
    @ResponseBody
    public ResponseEntity<?> updateAppointmentDetails(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            // Extract appointment data from request
            Integer appointmentId = (Integer) request.get("appointmentId");
            Integer specialtyId = (Integer) request.get("specialtyId");
            Integer doctorId = (Integer) request.get("doctorId");
            String appointmentDateStr = (String) request.get("appointmentDate");
            String appointmentTimeStr = (String) request.get("appointmentTime");
            String reasonForVisit = (String) request.get("reasonForVisit");

            // Validate required fields
            if (appointmentId == null) {
                return ResponseEntity.badRequest().body("Appointment ID is required");
            }

            // Create AppointmentFormDTO for update
            AppointmentFormDTO appointmentFormDTO = new AppointmentFormDTO();
            if (doctorId != null) {
                appointmentFormDTO.setDoctorId(doctorId);
            }
            if (appointmentDateStr != null && !appointmentDateStr.isEmpty()) {
                appointmentFormDTO.setAppointmentDate(LocalDate.parse(appointmentDateStr));
            }
            if (appointmentTimeStr != null && !appointmentTimeStr.isEmpty()) {
                appointmentFormDTO.setAppointmentTime(appointmentTimeStr);
            }
            if (reasonForVisit != null) {
                appointmentFormDTO.setDescription(reasonForVisit);
            }

            // Update appointment
            boolean updated = appointmentService.updateAppointment(appointmentId, appointmentFormDTO);

            if (updated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Appointment updated successfully"
                ));
            } else {
                return ResponseEntity.status(404).body("Appointment not found");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating appointment: " + e.getMessage());
        }
    }
    @GetMapping("/api/doctors/specialty/{specialtyId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDoctorsBySpecialty(@PathVariable Integer specialtyId) {
        try {
            List<Doctor> doctors = appointmentService.getDoctorsBySpecialization(specialtyId);
            List<Map<String, Object>> doctorList = doctors.stream()
                    .map(doctor -> {
                        Map<String, Object> doctorMap = new HashMap<>();
                        doctorMap.put("doctorId", doctor.getDoctorId());
                        doctorMap.put("fullName", doctor.getUser().getFullName());
                        return doctorMap;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(doctorList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/book-appointment")
    public String bookAppointment(
            @Valid @ModelAttribute("appointmentForm") AppointmentFormDTO appointmentForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            List<Specialization> specializations = appointmentService.getAllSpecializations();
            Patient patient = patientRepository.findById(appointmentForm.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + appointmentForm.getPatientId()));
            model.addAttribute("specialties", specializations);
            model.addAttribute("patient", patient);
            model.addAttribute("patientId", appointmentForm.getPatientId());
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");
            model.addAttribute("isUpdate", appointmentForm.getAppointmentId() != null);
            return "Receptionists/appointments";
        }

        try {
            Appointment appointment;
            if (appointmentForm.getAppointmentId() != null) {
                appointment = appointmentService.updateAppointment(
                        appointmentForm.getAppointmentId(),
                        appointmentForm.getPatientId(),
                        appointmentForm.getDoctorId(),
                        appointmentForm.getAppointmentDate(),
                        appointmentForm.getAppointmentTime(),
                        appointmentForm.getEmail(),
                        appointmentForm.getPhoneNumber(),
                        appointmentForm.getDescription(),
                        appointmentForm.getEmergencyContact()
                );
                redirectAttributes.addFlashAttribute("successMessage", "Your appointment has been successfully updated.");
            } else {
                appointment = appointmentService.bookAppointment(
                        appointmentForm.getPatientId(),
                        appointmentForm.getDoctorId(),
                        appointmentForm.getAppointmentDate(),
                        appointmentForm.getAppointmentTime(),
                        appointmentForm.getEmail(),
                        appointmentForm.getPhoneNumber(),
                        appointmentForm.getDescription(),
                        appointmentForm.getEmergencyContact()
                );
                redirectAttributes.addFlashAttribute("successMessage",
                        "Your appointment has been successfully booked for " +
                                appointmentForm.getAppointmentDate() + " at " +
                                formatTimeForDisplay(appointmentForm.getAppointmentTime()));
            }
            return "redirect:/Receptionists/appointments";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            List<Specialization> specializations = appointmentService.getAllSpecializations();
            Patient patient = patientRepository.findById(appointmentForm.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + appointmentForm.getPatientId()));
            model.addAttribute("specialties", specializations);
            model.addAttribute("patient", patient);
            model.addAttribute("patientId", appointmentForm.getPatientId());
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");
            model.addAttribute("isUpdate", appointmentForm.getAppointmentId() != null);
            return "Receptionists/appointments";
        }

    }
    private String formatTimeForDisplay(String time) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
        return String.format("%d:%02d %s", displayHour, minute, period);
    }

}
