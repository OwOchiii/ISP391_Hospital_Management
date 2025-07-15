package orochi.controller;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.config.CustomUserDetails;
import orochi.dto.AppointmentDTO;
import orochi.dto.AppointmentFormDTO;
import orochi.dto.SpecializationDTO;
import orochi.model.*;
import orochi.repository.PatientRepository;
import orochi.repository.UserRepository;
import orochi.service.*;
import orochi.service.impl.ReceptionistService;
import orochi.service.FileStorageService;

@Controller
@RequestMapping("/receptionist")
public class ReceptionistController {

    private final ReceptionistService receptionistService;
    private final UserRepository userRepository;
    private final SpecializationService specializationService;
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final PatientRepository patientRepository;
    private final RoomService roomService;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(ReceptionistController.class);

    @Autowired
    public ReceptionistController(ReceptionistService receptionistService, UserRepository userRepository,
                                  SpecializationService specializationService, AppointmentService appointmentService,
                                  PatientRepository patientRepository, RoomService roomService,
                                  NotificationService notificationService, EmailService emailService,
                                  FileStorageService fileStorageService) {
        this.receptionistService = receptionistService;
        this.userRepository = userRepository;
        this.specializationService = specializationService;
        this.appointmentService = appointmentService;
        this.patientRepository = patientRepository;
        this.roomService = roomService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.fileStorageService = fileStorageService;
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
        List<Notification> notifications = notificationService.findByUserIdOrderByCreatedAtDesc(userId);
        long unreadCount = notifications.stream()
                .filter(n -> !n.isRead())
                .count();

        Notification latestNotification = notifications.isEmpty() ? null : notifications.get(0);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("latestNotification", latestNotification);

        return "Receptionists/dashboard";
    }

    @GetMapping("/new_appointment")
    public String newAppointment(Authentication authentication, Model model) {
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
    public String profile(Authentication authentication, Model model) {
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

        // Add user data to model for profile display
        model.addAttribute("user", user);

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

    @GetMapping("/api/payment_history")
    @ResponseBody
    public ResponseEntity<?> getPaymentHistoryData(Authentication authentication) {
        try {
            // Ensure authentication is not null
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
            }

            // Get payment history data from database using new method
            List<Map<String, Object>> paymentData = receptionistService.getPaymentHistoryData();

            logger.info("Payment history API returned {} records", paymentData.size());
            return ResponseEntity.ok(paymentData);

        } catch (Exception e) {
            logger.error("Error in payment history API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching payment history: " + e.getMessage());
        }
    }

    @GetMapping("/api/payment_history/date-range")
    @ResponseBody
    public ResponseEntity<?> getPaymentHistoryDataByDateRange(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            Authentication authentication) {
        try {
            // Ensure authentication is not null
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
            }

            // Parse dates
            LocalDate from = LocalDate.parse(fromDate);
            LocalDate to = LocalDate.parse(toDate);

            // Get payment history data from database with date filter
            List<Map<String, Object>> paymentData = receptionistService.getPaymentHistoryDataByDateRange(from, to);

            logger.info("Payment history API returned {} records for date range {}-{}", paymentData.size(), fromDate, toDate);
            return ResponseEntity.ok(paymentData);

        } catch (Exception e) {
            logger.error("Error in payment history date range API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching payment history by date range: " + e.getMessage());
        }
    }

    @GetMapping("/view_payment_details")
    public String viewPaymentDetails(@RequestParam(required = false) Integer patientId,
                                   @RequestParam(required = false) Integer receiptId,
                                   Authentication authentication,
                                   Model model) {
        try {
            // Ensure authentication is not null
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            // Check if patientId is provided
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required to view payment details");
                return "redirect:/receptionist/payments";
            }

            // Lấy thông tin bệnh nhân và cuộc hẹn giống như pay_invoice
            Map<String, Object> invoiceData = receptionistService.getInvoiceDataByPatientId(patientId);

            if (invoiceData == null) {
                model.addAttribute("errorMessage", "Patient not found");
                return "redirect:/receptionist/payments";
            }

            // Lấy thông tin staff để hiển thị trong "Processed By"
            Map<String, Object> staffInfo = receptionistService.getReceptionistStaffInfo();
            logger.info("=== Controller staffInfo debug ===");
            logger.info("Controller received staffInfo: {}", staffInfo);
            logger.info("staffInfo fullName: {}", staffInfo.get("fullName"));
            logger.info("staffInfo userId: {}", staffInfo.get("userId"));

            model.addAttribute("invoiceData", invoiceData);
            model.addAttribute("staffInfo", staffInfo);
            model.addAttribute("patientId", patientId);
            model.addAttribute("receiptId", receiptId);

            // Debug model attributes
            logger.info("=== Model attributes debug ===");
            logger.info("Model staffInfo: {}", model.getAttribute("staffInfo"));

            return "Receptionists/view_payment_details";
        } catch (Exception e) {
            logger.error("Error loading view payment details page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading payment details: " + e.getMessage());
            return "redirect:/receptionist/payments";
        }
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
    public String payInvoice(@RequestParam Integer patientId,
                            @RequestParam(required = false) Integer receiptId,
                            Model model,
                            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            // Lấy thông tin bệnh nhân và cuộc hẹn
            Map<String, Object> invoiceData = receptionistService.getInvoiceDataByPatientId(patientId);

            if (invoiceData == null) {
                model.addAttribute("errorMessage", "Patient not found");
                return "redirect:/receptionist/payments";
            }

            // Lấy thông tin staff để hiển thị trong "Processed By"
            Map<String, Object> staffInfo = receptionistService.getReceptionistStaffInfo();
            logger.info("=== Controller staffInfo debug ===");
            logger.info("Controller received staffInfo: {}", staffInfo);
            logger.info("staffInfo fullName: {}", staffInfo.get("fullName"));
            logger.info("staffInfo userId: {}", staffInfo.get("userId"));

            model.addAttribute("invoiceData", invoiceData);
            model.addAttribute("staffInfo", staffInfo);
            model.addAttribute("patientId", patientId);
            model.addAttribute("receiptId", receiptId);

            // Debug model attributes
            logger.info("=== Model attributes debug ===");
            logger.info("Model staffInfo: {}", model.getAttribute("staffInfo"));

            return "Receptionists/pay_invoice";
        } catch (Exception e) {
            logger.error("Error loading pay invoice page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading invoice data: " + e.getMessage());
            return "redirect:/receptionist/payments";
        }
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
    public ResponseEntity<?> getAppointmentTableData() {
        try {
            // Sử dụng method mới để chỉ lấy appointments của ngày hiện tại
            String todayStr = LocalDate.now().toString();
            List<Map<String, Object>> todaysAppointmentTableData = receptionistService.getTodaysPendingAppointmentTableData(todayStr);
            return ResponseEntity.ok(todaysAppointmentTableData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving today's appointment table data: " + e.getMessage());
        }
    }

    // New endpoint for pending appointments only - also filter by today's date
    @GetMapping("/pendingAppointmentRequest")
    public ResponseEntity<?> getPendingAppointmentTableData() {
        try {
            // Chỉ lấy pending appointments của ngày hiện tại
            String todayStr = LocalDate.now().toString();
            List<Map<String, Object>> todaysPendingAppointmentTableData = receptionistService.getTodaysPendingAppointmentTableData(todayStr);
            return ResponseEntity.ok(todaysPendingAppointmentTableData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving today's pending appointment table data: " + e.getMessage());
        }
    }

    // New endpoint for today's pending appointments
    @GetMapping("/pendingAppointmentRequest/today")
    public ResponseEntity<?> getTodaysPendingAppointmentTableData(@RequestParam String date) {
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
    ) {
        try {
            // Update status to "Scheduled" (Approved)
            boolean isSuccess = receptionistService.updateAppointmentStatus(id, "Scheduled");
            if (isSuccess) {
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
            String updateReason = (String) request.get("updateReason");

            // Default reason if not provided
            if (updateReason == null || updateReason.trim().isEmpty()) {
                updateReason = "doctor schedule conflict";
            }

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
                // Get the updated appointment details to send email notification
                try {
                    // Fetch the updated appointment with all relationships loaded
                    Appointment updatedAppointment = appointmentService.getAppointmentById(appointmentId);

                    if (updatedAppointment != null && updatedAppointment.getPatient() != null &&
                        updatedAppointment.getPatient().getUser() != null &&
                        updatedAppointment.getPatient().getUser().getEmail() != null) {

                        // Send email notification to patient
                        String patientEmail = updatedAppointment.getPatient().getUser().getEmail();
                        logger.info("Sending appointment update email to: {}", patientEmail);
                        emailService.sendAppointmentUpdateEmail(patientEmail, updatedAppointment, updateReason);
                        logger.info("Appointment update email sent successfully");
                    } else {
                        logger.warn("Could not send appointment update email: patient email not available");
                    }
                } catch (Exception e) {
                    // Log but don't fail the appointment update if email sending fails
                    logger.error("Failed to send appointment update email: {}", e.getMessage(), e);
                }

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

            // Send registration confirmation email
            try {
                logger.info("Sending registration confirmation email to: {}", email);

                // QUAN TRỌNG: Đảm bảo patient object có đầy đủ thông tin user
                Patient patientForEmail = newPatient.getPatient();
                if (patientForEmail != null) {
                    // Đảm bảo user relationship được set
                    if (patientForEmail.getUser() == null) {
                        patientForEmail.setUser(newPatient);
                    }

                    logger.info("=== Email Patient Data Debug ===");
                    logger.info("Patient ID: {}", patientForEmail.getPatientId());
                    logger.info("Patient User: {}", patientForEmail.getUser() != null ? "SET" : "NULL");
                    if (patientForEmail.getUser() != null) {
                        logger.info("User FullName: {}", patientForEmail.getUser().getFullName());
                        logger.info("User Email: {}", patientForEmail.getUser().getEmail());
                        logger.info("User Phone: {}", patientForEmail.getUser().getPhoneNumber());
                        logger.info("User ID: {}", patientForEmail.getUser().getUserId());
                    }

                    emailService.sendPatientRegistrationEmail(email, patientForEmail, passwordHash);
                    logger.info("Registration confirmation email sent successfully");
                } else {
                    logger.error("Patient object is null after registration");
                    throw new RuntimeException("Patient object not found after registration");
                }
            } catch (Exception e) {
                logger.error("Failed to send registration confirmation email: {}", e.getMessage(), e);
                // Continue with registration even if email fails
            }

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

    // API endpoint for booking appointments
    @PostMapping("/appointments/book")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bookAppointment(@Valid @ModelAttribute AppointmentFormDTO appointmentDTO,
                                                               BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (bindingResult.hasErrors()) {
                response.put("success", false);
                response.put("message", "Validation errors: " + bindingResult.getAllErrors().get(0).getDefaultMessage());
                return ResponseEntity.badRequest().body(response);
            }

            Appointment appointment = appointmentService.bookAppointment(appointmentDTO);

            response.put("success", true);
            response.put("message", "Appointment booked successfully");
            response.put("appointmentId", appointment.getAppointmentId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error booking appointment: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API endpoint to get doctor by specialty (single doctor per specialty)
    @GetMapping("/api/doctor/specialty/{specialtyId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDoctorBySpecialtyApi(@PathVariable Integer specialtyId) {
        try {
            List<Doctor> doctors = appointmentService.getDoctorsBySpecialization(specialtyId);
            List<Map<String, Object>> doctorList = doctors.stream().map(doctor -> {
                Map<String, Object> doctorMap = new HashMap<>();
                doctorMap.put("doctorId", doctor.getDoctorId());
                // Sửa lỗi: Lấy fullName từ user object
                doctorMap.put("fullName", doctor.getUser() != null ? doctor.getUser().getFullName() : "Unknown Doctor");
                doctorMap.put("bioDescription", doctor.getBioDescription());
                return doctorMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(doctorList);
        } catch (Exception e) {
            logger.error("Error fetching doctors by specialty: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    // API endpoint to get rooms by specialty - NOW USING REAL DATA
    @GetMapping("/api/rooms/specialty/{specialtyId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRoomsBySpecialty(@PathVariable Integer specialtyId) {
        try {
            // Use RoomService to get real room data by specialty
            List<Map<String, Object>> roomDetails = roomService.getRoomDetailsWithDepartmentBySpecialtyId(specialtyId);

            if (!roomDetails.isEmpty()) {
                // Convert to the format expected by frontend
                List<Map<String, Object>> rooms = roomDetails.stream()
                        .map(roomData -> {
                            Map<String, Object> room = new HashMap<>();
                            room.put("roomId", roomData.get("roomId"));
                            room.put("roomNumber", roomData.get("roomNumber"));
                            room.put("departmentName", roomData.get("departmentName"));
                            room.put("roomName", roomData.get("roomName"));
                            room.put("type", roomData.get("type"));
                            room.put("capacity", roomData.get("capacity"));
                            return room;
                        })
                        .collect(Collectors.toList());

                logger.info("Found {} rooms for specialty ID {}", rooms.size(), specialtyId);
                return ResponseEntity.ok(rooms);
            } else {
                // If no rooms found via detailed query, try simplified approach
                List<orochi.model.Room> roomEntities = roomService.getRoomsBySpecialtyId(specialtyId);

                if (!roomEntities.isEmpty()) {
                    List<Map<String, Object>> rooms = roomEntities.stream()
                            .map(roomEntity -> {
                                Map<String, Object> room = new HashMap<>();
                                room.put("roomId", roomEntity.getRoomId());
                                room.put("roomNumber", roomEntity.getRoomNumber());
                                room.put("departmentName", roomEntity.getDepartment() != null ?
                                        roomEntity.getDepartment().getDeptName() : "General");
                                room.put("roomName", roomEntity.getRoomName());
                                room.put("type", roomEntity.getType());
                                room.put("capacity", roomEntity.getCapacity());
                                return room;
                            })
                            .collect(Collectors.toList());

                    logger.info("Found {} rooms (simplified) for specialty ID {}", rooms.size(), specialtyId);
                    return ResponseEntity.ok(rooms);
                }

                // If still no rooms found, return a default/fallback room
                logger.warn("No rooms found for specialty ID {}. Returning fallback room.", specialtyId);
                List<Map<String, Object>> fallbackRooms = new ArrayList<>();
                Map<String, Object> fallbackRoom = new HashMap<>();
                fallbackRoom.put("roomId", 1);
                fallbackRoom.put("roomNumber", "101");
                fallbackRoom.put("departmentName", "General Medicine");
                fallbackRoom.put("roomName", "Consultation Room 101");
                fallbackRoom.put("type", "Consultation");
                fallbackRoom.put("capacity", 2);
                fallbackRooms.add(fallbackRoom);

                return ResponseEntity.ok(fallbackRooms);
            }

        } catch (Exception e) {
            logger.error("Error fetching rooms by specialty ID {}: ", specialtyId, e);

            // Return fallback data in case of error
            List<Map<String, Object>> errorFallbackRooms = new ArrayList<>();
            Map<String, Object> errorRoom = new HashMap<>();
            errorRoom.put("roomId", 1);
            errorRoom.put("roomNumber", "101");
            errorRoom.put("departmentName", "General");
            errorRoom.put("roomName", "Emergency Room");
            errorRoom.put("type", "Emergency");
            errorRoom.put("capacity", 1);
            errorFallbackRooms.add(errorRoom);

            return ResponseEntity.ok(errorFallbackRooms);
        }
    }

    // API endpoint to get doctor availability
    @GetMapping("/api/doctor/{doctorId}/availability")
    @ResponseBody
    public ResponseEntity<Map<String, List<String>>> getDoctorAvailability(
            @PathVariable Integer doctorId,
            @RequestParam String date) {
        try {
            LocalDate appointmentDate = LocalDate.parse(date);
            Map<String, List<String>> availability = appointmentService.getDoctorAvailability(appointmentDate, doctorId);
            return ResponseEntity.ok(availability);
        } catch (Exception e) {
            logger.error("Error fetching doctor availability: ", e);
            Map<String, List<String>> errorResponse = new HashMap<>();
            errorResponse.put("bookedTimes", new ArrayList<>());
            errorResponse.put("unavailableTimes", new ArrayList<>());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    /**
     * API endpoint to test payment history data retrieval
     */
    @GetMapping("/api/payment_history/test")
    @ResponseBody
    public ResponseEntity<?> testPaymentHistoryData(Authentication authentication) {
        try {
            // Ensure authentication is not null
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
            }

            // Test direct repository call
            List<Map<String, Object>> directData = receptionistService.getAllPaymentHistoryDataForTesting();

            Map<String, Object> testResult = new HashMap<>();
            testResult.put("directRepositoryData", directData);
            testResult.put("dataCount", directData.size());

            if (!directData.isEmpty()) {
                testResult.put("sampleRecord", directData.get(0));
            }

            logger.info("Test payment history API returned {} records", directData.size());
            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            logger.error("Error in test payment history API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error testing payment history: " + e.getMessage());
        }
    }

    /**
     * API endpoint to get today's PENDING payment data only
     * For Today's Payment List - only show transactions with Pending status for current date
     */
    @GetMapping("/api/payments/today/pending")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTodaysPendingPayments() {
        try {
            // Get today's payment data filtered by Pending status ONLY
            List<Map<String, Object>> allTodaysPayments = receptionistService.getTodaysAppointmentPaymentData();

            // Filter to STRICTLY include ONLY Pending transactions
            List<Map<String, Object>> pendingPayments = allTodaysPayments.stream()
                    .filter(payment -> {
                        String status = (String) payment.get("status");
                        // CHỈ LẤY STATUS "Pending" - KHÔNG LẤY CÁC STATUS KHÁC
                        return status != null && "Pending".equalsIgnoreCase(status.trim());
                    })
                    .collect(Collectors.toList());

            logger.info("Today's pending payments API returned {} PENDING records only", pendingPayments.size());
            return ResponseEntity.ok(pendingPayments);
        } catch (Exception e) {
            logger.error("Error fetching today's pending payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getUserId();
        }
        return null;
    }

    @PostMapping("/notifications/{id}/read")
    @Transactional
    @ResponseBody
    public ResponseEntity<?> markNotificationAsRead(@PathVariable("id") Integer notificationId) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            logger.error("No userId found for authenticated user");
            return ResponseEntity.badRequest().body("User ID is required");
        }

        Notification notification = notificationService.findByIdAndUserId(notificationId, userId);
        if (notification == null) {
            logger.error("Notification ID: {} not found for user ID: {}", notificationId, userId);
            return ResponseEntity.notFound().build();
        }

        notification.setRead(true);
        notificationService.save(notification);
        logger.info("Notification ID: {} marked as read for user ID: {}", notificationId, userId);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/notifications/mark-read/{id}")
    @Transactional
    @ResponseBody
    public ResponseEntity<?> markReadAlias(@PathVariable("id") Integer id) {
        return markNotificationAsRead(id);
    }

    /**
     * API endpoint to process payment
     */
    @PostMapping("/api/process-payment")
    @ResponseBody
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Object> paymentRequest, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
            }

            // Extract payment data from request
            Integer patientId = (Integer) paymentRequest.get("patientId");
            Integer appointmentId = (Integer) paymentRequest.get("appointmentId");
            String transactionIdStr = (String) paymentRequest.get("transactionId");
            String receiptIdStr = (String) paymentRequest.get("receiptId");
            String method = (String) paymentRequest.get("method");
            String notes = (String) paymentRequest.get("notes");
            Object totalAmountObj = paymentRequest.get("totalAmount");
            Object amountReceivedObj = paymentRequest.get("amountReceived");

            logger.info("Processing payment request: {}", paymentRequest);

            // Validate required fields
            if (patientId == null || appointmentId == null || method == null) {
                return ResponseEntity.badRequest().body("Missing required payment fields");
            }

            // Convert totalAmount to proper type
            Double totalAmount = 0.0;
            if (totalAmountObj != null) {
                if (totalAmountObj instanceof Number) {
                    totalAmount = ((Number) totalAmountObj).doubleValue();
                } else {
                    totalAmount = Double.parseDouble(totalAmountObj.toString());
                }
            }

            // Convert amountReceived for Cash payments
            Double amountReceived = null;
            if ("Cash".equals(method) && amountReceivedObj != null) {
                if (amountReceivedObj instanceof Number) {
                    amountReceived = ((Number) amountReceivedObj).doubleValue();
                } else {
                    amountReceived = Double.parseDouble(amountReceivedObj.toString());
                }
            }

            // Process payment through service - this will update status from Pending to Paid
            Map<String, Object> result = receptionistService.processPayment(
                patientId,
                appointmentId,
                transactionIdStr,
                receiptIdStr,
                method,
                totalAmount,
                amountReceived,
                notes
            );

            logger.info("Payment processed successfully: {}", result);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment processed successfully",
                "transactionId", result.get("transactionId"),
                "receiptId", result.get("receiptId"),
                "status", "Paid", // Status is now Paid
                "timeOfPayment", java.time.LocalDateTime.now().toString()
            ));

        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payment: " + e.getMessage());
        }
    }

    /**
     * API endpoint to update receptionist profile
     * Validates and updates Full Name, Email, and Phone Number
     * ID field is read-only and cannot be modified
     */
    @PostMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update basic info
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhoneNumber(phoneNumber);

            String avatarUrl = null;

            // Handle avatar upload
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    // Get the current working directory (project root)
                    String projectRoot = System.getProperty("user.dir");
                    String uploadDir = projectRoot + File.separator + "uploads" + File.separator + "receptionist-avatars" + File.separator;

                    // Create uploads directory if it doesn't exist
                    File uploadDirFile = new File(uploadDir);
                    if (!uploadDirFile.exists()) {
                        boolean created = uploadDirFile.mkdirs();
                        logger.info("Created upload directory: {} - Success: {}", uploadDir, created);
                    }

                    // Generate unique filename
                    String originalFilename = avatarFile.getOriginalFilename();
                    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    String newFilename = "receptionist_" + userId + "_" +
                                       java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                                       fileExtension;

                    // Save file
                    File destinationFile = new File(uploadDir + newFilename);
                    logger.info("Saving file to: {}", destinationFile.getAbsolutePath());
                    avatarFile.transferTo(destinationFile);

                    // Set avatar URL for web access
                    avatarUrl = "/uploads/receptionist-avatars/" + newFilename;
                    user.setAvatarUrl(avatarUrl);
                    logger.info("Avatar saved successfully. URL: {}", avatarUrl);

                } catch (Exception e) {
                    logger.error("Error uploading avatar: ", e);
                    response.put("success", false);
                    response.put("message", "Error uploading avatar: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }

            // Save user
            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Profile updated successfully");
            if (avatarUrl != null) {
                response.put("avatarUrl", avatarUrl);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating profile: ", e);
            response.put("success", false);
            response.put("message", "Error updating profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API endpoint to upload avatar for receptionist
     */
    @PostMapping("/api/upload-avatar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("avatarFile") MultipartFile avatarFile,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Validate file
            if (avatarFile == null || avatarFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn file ảnh");
                return ResponseEntity.badRequest().body(response);
            }

            // Check file type
            String contentType = avatarFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "Chỉ chấp nhận file ảnh");
                return ResponseEntity.badRequest().body(response);
            }

            // Get current user
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify user is receptionist
            if (!user.getRole().getRoleName().equals("RECEPTIONIST")) {
                response.put("success", false);
                response.put("message", "Only receptionists can upload avatar");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            try {
                // Upload file using FileStorageService
                String imageUrl = fileStorageService.storeFile(avatarFile, "receptionist-avatars");

                // Find or create Receptionist record
                Receptionist receptionist = receptionistService.findByUserId(userId);
                if (receptionist == null) {
                    receptionist = new Receptionist();
                    receptionist.setUserId(userId);
                }

                // Delete old avatar if exists
                if (receptionist.getImageUrl() != null) {
                    try {
                        fileStorageService.deleteFile(receptionist.getImageUrl());
                    } catch (Exception e) {
                        logger.warn("Could not delete old avatar: {}", e.getMessage());
                    }
                }

                // Save new avatar URL
                receptionist.setImageUrl(imageUrl);
                receptionistService.save(receptionist);

                response.put("success", true);
                response.put("message", "Upload ảnh thành công!");
                response.put("imageUrl", imageUrl);

                logger.info("Avatar uploaded successfully for user {}: {}", userId, imageUrl);
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                logger.error("Error uploading avatar for user {}: ", userId, e);
                response.put("success", false);
                response.put("message", "Lỗi upload ảnh: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("Error in upload avatar endpoint: ", e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API endpoint to get current avatar information for receptionist
     */
    @GetMapping("/api/avatar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentAvatar(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Get current user
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify user is receptionist
            if (!user.getRole().getRoleName().equals("RECEPTIONIST")) {
                response.put("success", false);
                response.put("message", "Only receptionists can access avatar");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Find Receptionist record
            Receptionist receptionist = receptionistService.findByUserId(userId);

            response.put("success", true);
            response.put("hasAvatar", receptionist != null && receptionist.getImageUrl() != null);
            response.put("imageUrl", receptionist != null ? receptionist.getImageUrl() : null);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting current avatar: ", e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
