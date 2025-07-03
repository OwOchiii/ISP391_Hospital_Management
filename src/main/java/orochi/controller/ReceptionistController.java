package orochi.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(ReceptionistController.class);

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

    // API endpoint to fetch all doctors with their specialties
    @GetMapping("/api/doctors")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllDoctorsApi() {
        try {
            List<Map<String, Object>> doctors = receptionistService.getAllDoctorsWithDetails();
            return ResponseEntity.ok(doctors);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to fetch individual doctor details
    @GetMapping("/api/doctors/{doctorId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDoctorDetailsApi(@PathVariable Integer doctorId) {
        try {
            Map<String, Object> doctor = receptionistService.getDoctorDetails(doctorId);
            if (doctor != null) {
                return ResponseEntity.ok(doctor);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to validate doctor specialty
    @GetMapping("/api/validate-doctor-specialty")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateDoctorSpecialty(
            @RequestParam Integer doctorId,
            @RequestParam Integer specialtyId) {
        try {
            boolean isValid = receptionistService.validateDoctorSpecialty(doctorId, specialtyId);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("message", isValid ? "Doctor has the required specialty" : "Doctor does not have the required specialty");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to get the single doctor for a specialty
    @GetMapping("/api/doctor/by-specialty/{specialtyId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDoctorBySpecialty(@PathVariable Integer specialtyId) {
        try {
            Map<String, Object> doctor = receptionistService.getDoctorBySpecialty(specialtyId);
            if (doctor != null) {
                return ResponseEntity.ok(doctor);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to get doctors by specialty
    @GetMapping("/api/doctors/by-specialty/{specialtyId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDoctorsBySpecialty(@PathVariable Integer specialtyId) {
        try {
            List<Map<String, Object>> doctors = receptionistService.getDoctorsBySpecialty(specialtyId);
            return ResponseEntity.ok(doctors);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

    // New payment API endpoints

    /**
     * API endpoint to get today's payment data
     */
    @GetMapping("/api/payments/today")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTodaysPayments() {
        try {
            List<Map<String, Object>> payments = receptionistService.getTodaysPaymentData();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            System.err.println("Error fetching today's payments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * API endpoint to get today's total revenue
     */
    @GetMapping("/api/payments/today/total")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTodaysTotalRevenue() {
        try {
            Double total = receptionistService.getTodaysTotalRevenue();
            Map<String, Object> response = new HashMap<>();
            response.put("total", total);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching today's total revenue: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * API endpoint to get filtered payment data by status
     */
    @GetMapping("/api/payments/today/filtered")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFilteredPayments(@RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> payments;
            if (status != null && !status.isEmpty()) {
                payments = receptionistService.getTodaysPaymentDataByStatus(status);
            } else {
                payments = receptionistService.getTodaysPaymentData();
            }
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            System.err.println("Error fetching filtered payments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * API endpoint to get all receipts with transaction data for debugging
     */
    @GetMapping("/api/payments/receipts")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllReceiptsWithTransactionData() {
        try {
            List<Map<String, Object>> receipts = receptionistService.getAllReceiptsWithTransactionData();
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            System.err.println("Error fetching receipts data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * API endpoint to get test payment data for debugging
     */
    @GetMapping("/api/payments/test")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTestPayments() {
        try {
            List<Map<String, Object>> payments = receptionistService.getTestPaymentData();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            System.err.println("Error fetching test payments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * API endpoint to get patients with appointments today and their transaction status
     * Logic: Check today's appointments by DateTime -> map to PatientID -> get Transaction status
     */
    @GetMapping("/api/payments/appointments-today")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPatientsWithAppointmentsToday() {
        try {
            List<Map<String, Object>> patientsWithPayments = receptionistService.getPatientsWithAppointmentsToday();
            return ResponseEntity.ok(patientsWithPayments);
        } catch (Exception e) {
            System.err.println("Error fetching patients with appointments today: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * API endpoint to get today's total revenue from appointments
     */
    @GetMapping("/api/payments/appointments-today/total")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTodaysAppointmentRevenue() {
        try {
            Double total = receptionistService.getTodaysAppointmentPaymentRevenue();
            Map<String, Object> response = new HashMap<>();
            response.put("total", total);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching today's appointment revenue: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * NEW API endpoint to get patients with appointments today and their payment status
     * This follows the exact logic requested: check DateTime in Appointment table for today
     * -> map by AppointmentID -> get PatientID -> get Transaction Status
     */
    @GetMapping("/api/payments/appointments-today-only")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTodaysAppointmentPayments() {
        try {
            List<Map<String, Object>> todaysAppointmentPayments = receptionistService.getTodaysAppointmentPaymentData();
            return ResponseEntity.ok(todaysAppointmentPayments);
        } catch (Exception e) {
            System.err.println("Error fetching today's appointment payments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * API endpoint to get all available statuses from database
     */
    @GetMapping("/api/payments/statuses")
    @ResponseBody
    public ResponseEntity<List<String>> getAllAvailableStatuses() {
        try {
            List<String> statuses = receptionistService.getAllAvailableStatuses();
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            System.err.println("Error fetching available statuses: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * API endpoint to get today's payment data with raw database status (no mapping)
     */
    @GetMapping("/api/payments/today/raw")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTodaysPaymentsRaw() {
        try {
            List<Map<String, Object>> payments = receptionistService.getTodaysPaymentDataRaw();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            System.err.println("Error fetching today's payments (raw): " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
    public String patientRegister(Authentication authentication, Model model) {
        try {
            // Ensure authentication is not null
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            // Add any necessary data to the model
            model.addAttribute("newUser", new Users());

            // Return the correct template path
            return "Receptionists/patient_register";

        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error rendering patient_register page: " + e.getMessage());
            e.printStackTrace();

            // Add error message to model
            model.addAttribute("errorMessage", "An error occurred while loading the registration page: " + e.getMessage());

            // Return to a safe page
            return "redirect:/receptionist/dashboard";
        }
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

    // New endpoint for pending appointments only
    @GetMapping("/pendingAppointmentRequest")
    public ResponseEntity<?> getPendingAppointmentTableData(){
        try {
            List<Map<String, Object>> pendingAppointmentTableData = receptionistService.getPendingAppointmentTableData();
            return ResponseEntity.ok(pendingAppointmentTableData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving pending appointment table data: " + e.getMessage());
        }
    }

    // New endpoint for today's pending appointments
    @GetMapping("/pendingAppointmentRequest/today")
    public ResponseEntity<?> getTodaysPendingAppointmentTableData(@RequestParam String date){
        try {
            System.out.println("Fetching appointments for date: " + date);
            List<Map<String, Object>> todaysPendingAppointments = receptionistService.getTodaysPendingAppointmentTableData(date);
            System.out.println("Found " + todaysPendingAppointments.size() + " appointments for today");
            return ResponseEntity.ok(todaysPendingAppointments);
        } catch (Exception e) {
            System.err.println("Error retrieving today's pending appointments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving today's pending appointment data: " + e.getMessage());
        }
    }

    @PostMapping("/confirmAppointment")
    public ResponseEntity<?> updateAppointmentStatus(
            @RequestParam int id
    ){
        try {
            // Update status to "Scheduled" (Approved)
            boolean isSuccess = receptionistService.updateAppointmentStatus(id, "Scheduled");
            if(isSuccess) {
                return ResponseEntity.ok("Appointment approved successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to approve appointment");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error approving appointment: " + e.getMessage());
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

            // Validate doctor specialty match if both doctor and specialty are provided
            if (doctorId != null && specialtyId != null) {
                boolean isValidSpecialty = receptionistService.validateDoctorSpecialty(doctorId, specialtyId);
                if (!isValidSpecialty) {
                    return ResponseEntity.badRequest().body("Error: The selected doctor does not specialize in the required medical specialty. Please select a doctor who specializes in this area.");
                }
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

    // API endpoint to check existing appointments for a doctor on a specific date
    @GetMapping("/api/appointments/check")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> checkExistingAppointments(
            @RequestParam Integer doctorId,
            @RequestParam String date,
            @RequestParam(defaultValue = "0") Integer excludeId) {

        try {
            // Parse the date string to LocalDate
            LocalDate appointmentDate = LocalDate.parse(date);

            // Get existing appointments for this doctor on this date
            List<Appointment> existingAppointments = appointmentService.getAppointmentsByDoctorAndDate(doctorId, appointmentDate);

            // Filter out the current appointment if we're editing
            if (excludeId > 0) {
                existingAppointments = existingAppointments.stream()
                        .filter(apt -> !apt.getAppointmentId().equals(excludeId))
                        .collect(Collectors.toList());
            }

            // Convert to a simple format for frontend
            List<Map<String, Object>> appointmentTimes = existingAppointments.stream()
                    .map(apt -> {
                        Map<String, Object> timeSlot = new HashMap<>();
                        timeSlot.put("appointmentId", apt.getAppointmentId());
                        timeSlot.put("appointmentTime", apt.getDateTime().toLocalTime().toString());
                        return timeSlot;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(appointmentTimes);

        } catch (Exception e) {
            System.err.println("Error checking existing appointments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
            return "Receptionists/new_appointment";
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
                        appointmentForm.getDescription()
//                        appointmentForm.getEmergencyContact()
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
                        appointmentForm.getDescription()
//                        appointmentForm.getEmergencyContact()
                );
                redirectAttributes.addFlashAttribute("successMessage",
                        "Your appointment has been successfully booked for " +
                                appointmentForm.getAppointmentDate() + " at " +
                                formatTimeForDisplay(appointmentForm.getAppointmentTime()));
            }
            return "redirect:/receptionist/new_appointment";
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
            return "Receptionists/new_appointment";
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

    @PostMapping("/register-patient")
    public ResponseEntity<?> registerNewPatient(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phoneNumber,
            @RequestParam String passwordHash,
            @RequestParam String dateOfBirth,
            @RequestParam String gender,
            @RequestParam String streetAddress,
            @RequestParam String city,
            @RequestParam String country,
            @RequestParam String addressType,
            @RequestParam(required = false) String description,
            Authentication authentication) {

        try {
            // Validate authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            // Get current user details
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof CustomUserDetails)) {
                return ResponseEntity.status(401).body("Invalid authentication");
            }

            CustomUserDetails userDetails = (CustomUserDetails) principal;
            Integer currentUserId = userDetails.getUserId();

            // Fetch current user from database
            Users currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            // Check if current user is a receptionist
            if (!currentUser.getRole().getRoleName().equals("RECEPTIONIST")) {
                return ResponseEntity.status(403).body("Only receptionists can register patients");
            }

            // Validate required fields
            if (fullName == null || fullName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Full name is required");
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Phone number is required");
            }
            if (passwordHash == null || passwordHash.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }
            if (dateOfBirth == null || dateOfBirth.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Date of birth is required");
            }
            if (gender == null || gender.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Gender is required");
            }

            // Validate email format
            if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                return ResponseEntity.badRequest().body("Invalid email format");
            }

            // Validate phone number format (Vietnam phone number)
            if (!phoneNumber.matches("^(0[3|5|7|8|9])+([0-9]{8})$")) {
                return ResponseEntity.badRequest().body("Invalid phone number format");
            }

            // Check if email already exists
            Optional<Users> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                return ResponseEntity.badRequest().body("Email already exists");
            }

            // Check if phone number already exists
            Optional<Users> existingUserByPhone = userRepository.findByPhoneNumber(phoneNumber);
            if (existingUserByPhone.isPresent()) {
                return ResponseEntity.badRequest().body("Phone number already exists");
            }

            // Prepare registration data for PatientContact structure
            Map<String, Object> registrationData = new HashMap<>();
            registrationData.put("fullName", fullName.trim());
            registrationData.put("email", email.trim().toLowerCase());
            registrationData.put("phoneNumber", phoneNumber.trim());
            // Hash the password before storing it
            String hashedPassword = passwordEncoder.encode(passwordHash);
            registrationData.put("passwordHash", hashedPassword);
            registrationData.put("dateOfBirth", dateOfBirth);
            registrationData.put("gender", gender);
            registrationData.put("streetAddress", streetAddress != null ? streetAddress.trim() : "");
            registrationData.put("city", city != null ? city.trim() : "");
            registrationData.put("country", country != null ? country.trim() : "");
            registrationData.put("addressType", addressType != null ? addressType.trim() : "Home");
            registrationData.put("description", description != null ? description.trim() : "");

            // Call service to register patient
            Users newPatient = receptionistService.registerNewPatient(registrationData);

            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Patient registered successfully",
                "patientId", newPatient.getPatient().getPatientId(),
                "userId", newPatient.getUserId()
            ));

        } catch (Exception e) {
            logger.error("Error registering patient", e);
            return ResponseEntity.status(500).body("Error registering patient: " + e.getMessage());
        }
    }

    @GetMapping("/patient_details")
    public String patientDetails(@RequestParam Integer patientId, Model model, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            // Add patientId to model for use in the template if needed
            model.addAttribute("patientId", patientId);

            return "Receptionists/patient_details";
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error rendering patient_details page: " + e.getMessage());
            e.printStackTrace();

            // Add error message to model
            model.addAttribute("errorMessage", "An error occurred while loading the patient details page: " + e.getMessage());

            // Return to a safe page
            return "redirect:/receptionist/patients";
        }
    }

}
