package orochi.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Add PDF generation imports
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import orochi.repository.AppointmentRepository;
import orochi.repository.PatientRepository;
import orochi.repository.UserRepository;
import orochi.repository.TransactionRepository;
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
    private final TransactionRepository transactionRepository;
    private final AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(ReceptionistController.class);

    @Autowired
    public ReceptionistController(ReceptionistService receptionistService, UserRepository userRepository,
                                  SpecializationService specializationService, AppointmentService appointmentService,
                                  PatientRepository patientRepository, RoomService roomService,
                                  NotificationService notificationService, EmailService emailService,
                                  FileStorageService fileStorageService, TransactionRepository transactionRepository,
                                  AppointmentRepository appointmentRepository) {
        this.receptionistService = receptionistService;
        this.userRepository = userRepository;
        this.specializationService = specializationService;
        this.appointmentService = appointmentService;
        this.patientRepository = patientRepository;
        this.roomService = roomService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.fileStorageService = fileStorageService;
        this.transactionRepository = transactionRepository;
        this.appointmentRepository = appointmentRepository;
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
            // Ch����� lấy pending appointments của ngày hiện tại
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
            logger.info("=== CONFIRM APPOINTMENT REQUEST ===");
            logger.info("Confirming appointment with ID: {}", id);

            // Update status to "Scheduled" (Approved) - Điều này sẽ trigger tạo Transaction
            boolean isSuccess = receptionistService.updateAppointmentStatus(id, "Scheduled");
            if (isSuccess) {
                logger.info("✅ Appointment {} confirmed successfully - Transaction should be created automatically", id);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Appointment confirmed successfully and transaction created"
                ));
            } else {
                logger.error("❌ Failed to confirm appointment {}", id);
                return ResponseEntity.badRequest().body("Failed to approve appointment");
            }
        } catch (Exception e) {
            logger.error("❌ Error confirming appointment {}: {}", id, e.getMessage(), e);
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

                // QUAN TRỌNG: Đảm bảo patient object có đ���y đủ thông tin user
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
    @PostMapping("/api/appointments/book")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bookAppointment(@RequestBody AppointmentFormDTO appointmentDTO,
                                                         BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("Received appointment booking request: {}", appointmentDTO);

            if (bindingResult.hasErrors()) {
                logger.error("Validation errors in appointment request: {}", bindingResult.getAllErrors());
                response.put("success", false);
                response.put("message", "Validation errors: " + bindingResult.getAllErrors().get(0).getDefaultMessage());
                return ResponseEntity.badRequest().body(response);
            }

            Appointment appointment = appointmentService.bookAppointment(appointmentDTO);
            logger.info("Appointment booked successfully with ID: {}", appointment.getAppointmentId());

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

            // Get current user ID as issuer
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer issuerId = userDetails.getUserId();

            // Process payment through service - this will update status from Pending to Paid
            Map<String, Object> result = receptionistService.processPayment(
                patientId,
                appointmentId,
                transactionIdStr,
                receiptIdStr,
                method,
                totalAmount,
                amountReceived,
                notes,
                issuerId  // Pass the issuer ID to fix the NULL constraint error
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

    @GetMapping("/online-payment")
    public String onlinePayment(@RequestParam Integer patientId,
                               @RequestParam Integer appointmentId,
                               Model model,
                               Authentication authentication) {
        try {
            // Ensure authentication is not null
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            // Fetch invoice data for the patient
            Map<String, Object> invoiceData = receptionistService.getInvoiceDataByPatientId(patientId);

            if (invoiceData == null) {
                model.addAttribute("errorMessage", "Patient not found");
                return "redirect:/receptionist/payments";
            }

            // Get staff info for the "Processed By" field
            Map<String, Object> staffInfo = receptionistService.getReceptionistStaffInfo();

            // Calculate payment amounts (similar to PatientController)
            BigDecimal consultationFee = new BigDecimal("500000"); // 500,000 VND
            BigDecimal serviceFee = new BigDecimal("50000");       // 50,000 VND
            BigDecimal discount = new BigDecimal("50000");         // 50,000 VND discount
            BigDecimal totalAmount = consultationFee.add(serviceFee).subtract(discount);

            // Add data to the model
            model.addAttribute("invoiceData", invoiceData);
            model.addAttribute("staffInfo", staffInfo);
            model.addAttribute("patientId", patientId);
            model.addAttribute("appointmentId", appointmentId);
            // Add the missing totalAmount and amount attributes
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("amount", totalAmount);
            model.addAttribute("consultationFee", consultationFee);
            model.addAttribute("serviceFee", serviceFee);
            model.addAttribute("discount", discount);

            // Return the online payment template
            return "Receptionists/online-payment";
        } catch (Exception e) {
            logger.error("Error loading online payment page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading payment details: " + e.getMessage());
            return "redirect:/receptionist/payments";
        }
    }

    // VNPay Configuration - cập nhật để xử lý VND
    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_ReturnUrl = "http://localhost:8089/receptionist/payment-return";
    public static String vnp_TmnCode = "K65YFBNM";
    public static String secretKey = "HSRAHZZPFEPACYMRDC023GI3WD3HRZSE";
    public static String vnp_Version = "2.1.0";

    // Tỷ giá USD to VND (có thể cập nhật từ API thực tế)
    private static final double USD_TO_VND_RATE = 24000.0; // 1 USD = 24,000 VND

    @PostMapping("/process-online-payment")
    public String processOnlinePayment(@RequestParam Integer appointmentId,
                                     @RequestParam Integer patientId,
                                     @RequestParam BigDecimal amount,
                                     @RequestParam String paymentMethod,
                                     @RequestParam String customerName,
                                     @RequestParam String customerPhone,
                                     @RequestParam String customerEmail,
                                     @RequestParam(required = false) String notes,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        try {
            logger.info("=== STARTING ONLINE PAYMENT PROCESSING ===");
            logger.info("AppointmentId: {}, PatientId: {}, Amount (VND): {}, Method: {}",
                       appointmentId, patientId, amount, paymentMethod);
            logger.info("Customer: {}, Phone: {}, Email: {}", customerName, customerPhone, customerEmail);

            // STEP 1: Validate appointment exists
            Appointment appointment = appointmentService.getAppointmentById(appointmentId);
            if (appointment == null) {
                logger.error("Invalid appointment ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid appointment");
                return "redirect:/receptionist/online-payment?patientId=" + patientId + "&appointmentId=" + appointmentId;
            }

            // STEP 2: Get patient information and validate
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (patientOpt.isEmpty()) {
                logger.error("Patient not found: {}", patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "Patient not found");
                return "redirect:/receptionist/online-payment?patientId=" + patientId + "&appointmentId=" + appointmentId;
            }

            // STEP 3: Calculate correct Patient Payment amount from services
            Map<String, Object> invoiceData = receptionistService.getInvoiceDataByPatientId(patientId);
            if (invoiceData == null) {
                logger.error("Invoice data not found for patient: {}", patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "Invoice data not found");
                return "redirect:/receptionist/online-payment?patientId=" + patientId + "&appointmentId=" + appointmentId;
            }

            // 🔥 CALCULATE PATIENT PAYMENT CORRECTLY 🔥
            List<Map<String, Object>> servicesUsed = (List<Map<String, Object>>) invoiceData.get("servicesUsed");
            BigDecimal correctPatientPaymentVND = calculateCorrectPatientPaymentInVND(servicesUsed);

            logger.info("=== PAYMENT AMOUNT VALIDATION ===");
            logger.info("Received Amount from Form (VND): {}", amount);
            logger.info("Calculated Patient Payment (VND): {}", correctPatientPaymentVND);

            // Validate that the amount matches the calculated Patient Payment
            BigDecimal tolerance = new BigDecimal("1000"); // Allow 1,000 VND tolerance
            if (amount.subtract(correctPatientPaymentVND).abs().compareTo(tolerance) > 0) {
                logger.warn("Amount mismatch - Expected Patient Payment: {} VND, Received: {} VND",
                           correctPatientPaymentVND, amount);

                // Force use the correct Patient Payment amount
                amount = correctPatientPaymentVND;
                logger.info("��� Updated amount to correct Patient Payment: {} VND", amount);
            }

            // STEP 4: 🔥 FIND EXISTING TRANSACTION - DO NOT CREATE NEW 🔥
            Transaction transaction = null;

            // Find existing transaction by AppointmentID and UserID
            List<Transaction> existingTransactions = transactionRepository.findByAppointmentId(appointmentId);
            if (!existingTransactions.isEmpty()) {
                // Filter by UserID to ensure exact match
                Optional<Transaction> matchingTransaction = existingTransactions.stream()
                    .filter(t -> t.getUserId() != null && t.getUserId().equals(patientOpt.get().getUser().getUserId()))
                    .findFirst();

                if (matchingTransaction.isPresent()) {
                    transaction = matchingTransaction.get();
                    logger.info("✅ FOUND EXISTING Transaction {} for AppointmentID={} AND UserID={}",
                               transaction.getTransactionId(), appointmentId, patientOpt.get().getUser().getUserId());
                }
            }

            // If NO existing transaction found, CREATE ONLY ONE transaction
            if (transaction == null) {
                logger.info("No existing transaction found - creating new one for AppointmentID={} AND UserID={}",
                           appointmentId, patientOpt.get().getUser().getUserId());

                transaction = new Transaction();
                transaction.setAppointmentId(appointmentId);
                transaction.setUserId(patientOpt.get().getUser().getUserId());

                // Map payment method to database-compatible values
                String dbMethod = mapPaymentMethodToDbValue(paymentMethod);
                transaction.setMethod(dbMethod);
                transaction.setStatus("Pending");
                transaction.setTimeOfPayment(LocalDateTime.now());

                // Save ONLY if no existing transaction found
                transaction = transactionRepository.save(transaction);
                logger.info("✅ CREATED NEW Transaction {} for AppointmentID={} AND UserID={}",
                           transaction.getTransactionId(), appointmentId, patientOpt.get().getUser().getUserId());
            } else {
                // Update existing transaction method and status if needed
                if (!"Paid".equalsIgnoreCase(transaction.getStatus())) {
                    String dbMethod = mapPaymentMethodToDbValue(paymentMethod);
                    transaction.setMethod(dbMethod);
                    transaction.setStatus("Pending");
                    transaction.setTimeOfPayment(LocalDateTime.now());
                    transaction = transactionRepository.save(transaction);
                    logger.info("✅ UPDATED EXISTING Transaction {} - Method: {}, Status: Pending",
                               transaction.getTransactionId(), dbMethod);
                } else {
                    logger.warn("⚠️ Transaction {} is already PAID - redirecting to success page", transaction.getTransactionId());
                    redirectAttributes.addFlashAttribute("successMessage", "Payment already completed successfully!");
                    return "redirect:/receptionist/payment-return?vnp_TxnRef=" + transaction.getTransactionId() + "&vnp_ResponseCode=00";
                }
            }

            // STEP 5: Process payment URL based on method
            String redirectUrl = null;
            switch (paymentMethod.toLowerCase()) {
                case "vnpay":
                    logger.info("Processing VNPay payment with Patient Payment amount {} VND...", amount);
                    redirectUrl = createVNPayPaymentUrl(transaction, request, amount);
                    logger.info("VNPay URL result: {}", redirectUrl != null ? "SUCCESS" : "FAILED");
                    break;
                case "momo":
                    redirectUrl = createMoMoPaymentUrl(transaction);
                    break;
                case "zalopay":
                    redirectUrl = createZaloPayPaymentUrl(transaction);
                    break;
                default:
                    logger.error("Invalid payment method: {}", paymentMethod);
                    redirectAttributes.addFlashAttribute("errorMessage", "Invalid payment method: " + paymentMethod);
                    return "redirect:/receptionist/online-payment?patientId=" + patientId + "&appointmentId=" + appointmentId;
            }

            if (redirectUrl == null || redirectUrl.isEmpty()) {
                logger.error("FAILED to create payment URL for method: {}", paymentMethod);
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to initiate payment. Please try again.");
                return "redirect:/receptionist/online-payment?patientId=" + patientId + "&appointmentId=" + appointmentId;
            }

            logger.info("=== PREPARING REDIRECT ===");
            logger.info("Using Transaction ID: {}", transaction.getTransactionId());
            logger.info("Method: {}", paymentMethod);
            logger.info("Amount (VND): {}", amount);
            logger.info("URL: {}", redirectUrl);

            // Validate VNPay URL before redirect
            if (paymentMethod.equalsIgnoreCase("vnpay")) {
                if (!redirectUrl.startsWith("https://sandbox.vnpayment.vn")) {
                    logger.error("Invalid VNPay URL format: {}", redirectUrl);
                    redirectAttributes.addFlashAttribute("errorMessage", "Invalid payment gateway URL");
                    return "redirect:/receptionist/online-payment?patientId=" + patientId + "&appointmentId=" + appointmentId;
                }

                try {
                    java.net.URL testUrl = new java.net.URL(redirectUrl);
                    logger.info("URL validation passed for: {}", testUrl.getHost());
                } catch (java.net.MalformedURLException e) {
                    logger.error("Malformed VNPay URL: {}", e.getMessage());
                    redirectAttributes.addFlashAttribute("errorMessage", "Invalid payment URL format");
                    return "redirect:/receptionist/online-payment?patientId=" + patientId + "&appointmentId=" + appointmentId;
                }
            }

            logger.info("=== EXECUTING REDIRECT ===");
            logger.info("Redirecting to: {}", redirectUrl);

            // For VNPay, use direct redirect
            if (paymentMethod.equalsIgnoreCase("vnpay")) {
                logger.info("Using direct redirect to VNPay");
                try {
                    response.sendRedirect(redirectUrl);
                    return null;
                } catch (Exception redirectException) {
                    logger.error("Direct redirect failed: {}", redirectException.getMessage());
                    redirectAttributes.addFlashAttribute("errorMessage", "Failed to redirect to VNPay: " + redirectException.getMessage());
                    return "redirect:/receptionist/online-payment?patientId=" + patientId + "&appointmentId=" + appointmentId;
                }
            }

            return "redirect:" + redirectUrl;

        } catch (Exception e) {
            logger.error("EXCEPTION in processOnlinePayment", e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while processing the payment: " + e.getMessage());
            return "redirect:/receptionist/online-payment?patientId=" + patientId + "&appointmentId=" + appointmentId;
        }
    }

    /**
     * Calculate CORRECT Patient Payment in VND from services used
     * Patient Payment = SubTotal - Discount (where SubTotal = sum of all service totals)
     */
    private BigDecimal calculateCorrectPatientPaymentInVND(List<Map<String, Object>> servicesUsed) {
        if (servicesUsed == null || servicesUsed.isEmpty()) {
            return BigDecimal.ZERO;
        }

        logger.info("=== CALCULATING CORRECT PATIENT PAYMENT ===");

        // Calculate SubTotal (sum of all service totals in USD)
        BigDecimal subTotalUSD = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (Map<String, Object> service : servicesUsed) {
            Object totalObj = service.get("total");
            Object quantityObj = service.get("quantity");

            if (totalObj != null && quantityObj != null) {
                BigDecimal serviceTotal;
                int quantity;

                try {
                    if (totalObj instanceof Number) {
                        serviceTotal = new BigDecimal(((Number) totalObj).doubleValue());
                    } else {
                        serviceTotal = new BigDecimal(totalObj.toString());
                    }

                    if (quantityObj instanceof Number) {
                        quantity = ((Number) quantityObj).intValue();
                    } else {
                        quantity = Integer.parseInt(quantityObj.toString());
                    }

                    subTotalUSD = subTotalUSD.add(serviceTotal);
                    totalQuantity += quantity;

                    logger.info("Service - Total: {} USD, Quantity: {}", serviceTotal, quantity);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid service total or quantity: {} / {}", totalObj, quantityObj);
                }
            }
        }

        // Calculate discount based on quantity rules
        BigDecimal discountPercent = BigDecimal.ZERO;
        if (totalQuantity >= 5) {
            discountPercent = new BigDecimal("10"); // 10%
        } else if (totalQuantity >= 3) {
            discountPercent = new BigDecimal("5");  // 5%
        } else if (totalQuantity == 1) {
            discountPercent = new BigDecimal("3");  // 3%
        }

        // Calculate discount amount
        BigDecimal discountAmountUSD = subTotalUSD.multiply(discountPercent).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);

        // Calculate Patient Payment (SubTotal - Discount)
        BigDecimal patientPaymentUSD = subTotalUSD.subtract(discountAmountUSD);

        // Convert to VND
        BigDecimal patientPaymentVND = patientPaymentUSD.multiply(new BigDecimal(USD_TO_VND_RATE));

        logger.info("=== PATIENT PAYMENT CALCULATION RESULTS ===");
        logger.info("SubTotal USD: {}", subTotalUSD);
        logger.info("Total Quantity: {}", totalQuantity);
        logger.info("Discount Percent: {}%", discountPercent);
        logger.info("Discount Amount USD: {}", discountAmountUSD);
        logger.info("Patient Payment USD: {}", patientPaymentUSD);
        logger.info("Patient Payment VND: {}", patientPaymentVND);

        return patientPaymentVND.setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    private String createVNPayPaymentUrl(Transaction transaction, HttpServletRequest request, BigDecimal amount) {
        try {
            logger.info("=== Creating VNPay URL ===");
            logger.info("Transaction ID: {}, Amount (VND): {}", transaction.getTransactionId(), amount);

            // Validate input parameters
            if (transaction == null || transaction.getTransactionId() == null) {
                logger.error("Transaction or TransactionID is null");
                return null;
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                logger.error("Amount is null or invalid: {}", amount);
                return null;
            }

            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", vnp_Version);
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", vnp_TmnCode);

            // VNPay requires amount in smallest unit (xu) - multiply VND by 100
            // Example: 5,808,000 VND -> 580,800,000 (xu)
            long amountInXu = amount.multiply(new BigDecimal(100)).longValue();
            vnpParams.put("vnp_Amount", String.valueOf(amountInXu));

            logger.info("Amount conversion: {} VND -> {} xu", amount, amountInXu);

            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", String.valueOf(transaction.getTransactionId()));

            String orderInfo = String.format("Thanh toan kham benh - Ma GD: %d - Ma hen: %d - So tien: %s VND",
                                            transaction.getTransactionId(),
                                            transaction.getAppointmentId(),
                                            amount.toPlainString());
            vnpParams.put("vnp_OrderInfo", orderInfo);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnpParams.put("vnp_IpAddr", getClientIpAddress(request));

            String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            vnpParams.put("vnp_CreateDate", createDate);

            logger.info("=== VNPay Parameters ===");
            logger.info("vnp_Version: {}", vnpParams.get("vnp_Version"));
            logger.info("vnp_Command: {}", vnpParams.get("vnp_Command"));
            logger.info("vnp_TmnCode: {}", vnpParams.get("vnp_TmnCode"));
            logger.info("vnp_Amount (xu): {}", vnpParams.get("vnp_Amount"));
            logger.info("vnp_CurrCode: {}", vnpParams.get("vnp_CurrCode"));
            logger.info("vnp_TxnRef: {}", vnpParams.get("vnp_TxnRef"));
            logger.info("vnp_OrderInfo: {}", vnpParams.get("vnp_OrderInfo"));
            logger.info("vnp_ReturnUrl: {}", vnpParams.get("vnp_ReturnUrl"));
            logger.info("vnp_CreateDate: {}", vnpParams.get("vnp_CreateDate"));

            // Build query string and sort parameters alphabetically
            StringBuilder query = new StringBuilder();
            vnpParams.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        if (query.length() > 0) query.append("&");
                        try {
                            query.append(entry.getKey())
                                 .append("=")
                                 .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                        } catch (Exception e) {
                            logger.error("Error encoding parameter: {} = {}", entry.getKey(), entry.getValue());
                        }
                    });

            String queryString = query.toString();
            logger.info("Query string before hash: {}", queryString);

            // Generate secure hash using HMAC SHA512
            String secureHash = hmacSHA512(secretKey, queryString);
            if (secureHash == null || secureHash.isEmpty()) {
                logger.error("Failed to generate secure hash");
                return null;
            }
            logger.info("Generated secure hash: {}", secureHash);

            String finalQuery = queryString + "&vnp_SecureHash=" + secureHash;
            String finalUrl = vnp_PayUrl + "?" + finalQuery;

            logger.info("=== Final VNPay URL ===");
            logger.info("Base URL: {}", vnp_PayUrl);
            logger.info("Full URL length: {} characters", finalUrl.length());
            logger.info("Final URL: {}", finalUrl);

            // Validate URL format
            if (!finalUrl.startsWith("https://sandbox.vnpayment.vn")) {
                logger.error("Invalid VNPay URL format: {}", finalUrl);
                return null;
            }

            // Test URL construction
            try {
                java.net.URL testUrl = new java.net.URL(finalUrl);
                logger.info("URL validation successful: {}", testUrl.getHost());
            } catch (java.net.MalformedURLException e) {
                logger.error("Malformed URL: {}", e.getMessage());
                return null;
            }

            logger.info("VNPay URL created successfully, ready for redirect");
            return finalUrl;

        } catch (Exception e) {
            logger.error("Error creating VNPay payment URL", e);
            e.printStackTrace(); // Print full stack trace for debugging
            return null;
        }
    }

    private String createMoMoPaymentUrl(Transaction transaction) {
        logger.info("MoMo payment initiated for transaction: {}", transaction.getTransactionId());
        return "/receptionist/payment-processing?method=momo&transactionId=" + transaction.getTransactionId();
    }

    private String createZaloPayPaymentUrl(Transaction transaction) {
        logger.info("ZaloPay payment initiated for transaction: {}", transaction.getTransactionId());
        return "/receptionist/payment-processing?method=zalopay&transactionId=" + transaction.getTransactionId();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            logger.error("Error generating HMAC SHA512", e);
            return null;
        }
    }

    @GetMapping("/payment-return")
    public String paymentReturn(@RequestParam Map<String, String> allRequestParams, Model model) {
        try {
            logger.info("=== VNPAY PAYMENT RETURN IN RECEPTIONIST CONTROLLER ===");
            logger.info("VNPay payment return with params: {}", allRequestParams);

            String transactionId = allRequestParams.get("vnp_TxnRef");
            String paymentStatus = allRequestParams.get("vnp_ResponseCode");
            String amountStr = allRequestParams.get("vnp_Amount");

            logger.info("VNPay return parameters:");
            logger.info("- Transaction ID: {}", transactionId);
            logger.info("- Response Code: {}", paymentStatus);
            logger.info("- Amount (xu): {}", amountStr);

            // Convert amount string to proper number (VNPay returns amount in xu - smallest unit)
            Long amountInXu = null;
            Double amountInVND = null;
            if (amountStr != null && !amountStr.trim().isEmpty()) {
                try {
                    amountInXu = Long.parseLong(amountStr);
                    // Convert from xu to VND (divide by 100)
                    amountInVND = amountInXu / 100.0;
                    logger.info("Amount conversion: {} xu -> {} VND", amountInXu, amountInVND);
                } catch (NumberFormatException e) {
                    logger.error("Error parsing amount: {}", amountStr, e);
                }
            }

            // Get current receptionist user ID for ProcessedByUserID
            Integer currentReceptionistId = getCurrentUserId();
            logger.info("Current receptionist processing payment: {}", currentReceptionistId);

            if (transactionId != null && !transactionId.trim().isEmpty()) {
                try {
                    Optional<Transaction> transactionOpt = transactionRepository.findById(Integer.valueOf(transactionId));
                    if (transactionOpt.isPresent()) {
                        Transaction transaction = transactionOpt.get();

                        String currentStatus = transaction.getStatus();
                        logger.info("Current transaction status before update: {}", currentStatus);

                        // Map VNPay response code to valid database Status values
                        if ("00".equals(paymentStatus)) {
                            // PAYMENT SUCCESSFUL - Update Transaction FIRST (ALWAYS update regardless of appointment check)
                            transaction.setStatus("Paid");
                            transaction.setProcessedByUserId(currentReceptionistId);
                            transaction.setMethod("Banking"); // VNPay is Banking method

                            // SAVE TRANSACTION IMMEDIATELY - This must happen regardless of appointment validation
                            Transaction savedTransaction = transactionRepository.save(transaction);
                            logger.info("✅ PAYMENT SUCCESSFUL - Transaction {} updated: Status='Paid', ProcessedBy={}, Method='Banking'",
                                       transactionId, currentReceptionistId);
                            logger.info("💾 Transaction {} saved to database with final status: {}, ProcessedBy: {}, Method: {}",
                                       transactionId, savedTransaction.getStatus(), savedTransaction.getProcessedByUserId(), savedTransaction.getMethod());

                            // Now try to update corresponding appointment status (SEPARATE from transaction update)
                            boolean appointmentUpdated = false;
                            if (transaction.getAppointmentId() != null && transaction.getUserId() != null) {
                                Optional<Appointment> appointmentOpt = appointmentRepository.findById(transaction.getAppointmentId());
                                if (appointmentOpt.isPresent()) {
                                    Appointment appointment = appointmentOpt.get();
                                    // STRICT SECURITY CHECK: Verify both AppointmentID and UserID match
                                    if (appointment.getAppointmentId().equals(transaction.getAppointmentId()) &&
                                        appointment.getPatientId().equals(transaction.getUserId())) {
                                        String oldAppointmentStatus = appointment.getStatus();
                                        appointment.setStatus("Completed");
                                        appointmentRepository.save(appointment);
                                        appointmentUpdated = true;
                                        logger.info("✅ Updated Appointment {} status from '{}' to 'Completed' for UserID: {}",
                                                   appointment.getAppointmentId(), oldAppointmentStatus, transaction.getUserId());
                                    } else {
                                        logger.error("❌ SECURITY VIOLATION - Appointment {} (PatientID: {}) does not match Transaction {} (AppointmentID: {}, UserID: {})",
                                                   appointment.getAppointmentId(), appointment.getPatientId(),
                                                   transactionId, transaction.getAppointmentId(), transaction.getUserId());
                                    }
                                } else {
                                    logger.error("❌ Appointment with ID {} not found for successful payment", transaction.getAppointmentId());
                                }
                            } else {
                                logger.error("❌ Transaction {} missing AppointmentID or UserID", transactionId);
                            }

                            // Get user names for Processed By information
                            String processedByName = "N/A";
                            if (savedTransaction.getProcessedByUserId() != null) {
                                Optional<Users> processedByUser = userRepository.findById(savedTransaction.getProcessedByUserId());
                                if (processedByUser.isPresent()) {
                                    processedByName = processedByUser.get().getFullName();
                                    logger.info("Processed by user: {} (ID: {})", processedByName, savedTransaction.getProcessedByUserId());
                                }
                            }

                            String customerName = "N/A";
                            if (savedTransaction.getUserId() != null) {
                                Optional<Users> customerUser = userRepository.findById(savedTransaction.getUserId());
                                if (customerUser.isPresent()) {
                                    customerName = customerUser.get().getFullName();
                                    logger.info("Customer: {} (ID: {})", customerName, savedTransaction.getUserId());
                                }
                            }

                            // Extract amount from RefundReason if available (only numbers)
                            Double amountReceived = amountInVND;
                            if (savedTransaction.getRefundReason() != null && !savedTransaction.getRefundReason().trim().isEmpty()) {
                                String refundReason = savedTransaction.getRefundReason().trim();
                                // Extract only numbers from RefundReason
                                String numberOnly = refundReason.replaceAll("[^0-9.]", "");
                                if (!numberOnly.isEmpty()) {
                                    try {
                                        amountReceived = Double.parseDouble(numberOnly);
                                        logger.info("Amount received from RefundReason: {} -> {}", refundReason, amountReceived);
                                    } catch (NumberFormatException e) {
                                        logger.warn("Could not extract number from RefundReason: {}", refundReason);
                                    }
                                }
                            }

                            // Get payment method from Transaction table
                            String paymentMethod = savedTransaction.getMethod() != null ? savedTransaction.getMethod() : "Banking";

                            // Set success message based on appointment update result
                            if (appointmentUpdated) {
                                model.addAttribute("successMessage", "Payment completed successfully! Transaction saved and appointment completed.");
                            } else {
                                model.addAttribute("successMessage", "Payment completed successfully! Transaction saved but appointment requires manual review.");
                            }

                            // Add data to model for display (using saved transaction with proper user names)
                            model.addAttribute("transaction", savedTransaction);
                            model.addAttribute("paymentStatus", paymentStatus);
                            model.addAttribute("amount", amountInVND);
                            model.addAttribute("amountInXu", amountInXu);
                            model.addAttribute("amountReceived", amountReceived);
                            model.addAttribute("processedByName", processedByName);
                            model.addAttribute("customerName", customerName);
                            model.addAttribute("paymentMethod", paymentMethod);

                            // Generate and save PDF receipt
                            try {
                                String pdfFilePath = generatePaymentReceiptPdf(savedTransaction, amountInVND, amountReceived, processedByName, customerName, paymentMethod);

                                // Save PDF path to database by updating transaction
                                savedTransaction.setRefundReason(savedTransaction.getRefundReason() + " | PDF Path: " + pdfFilePath);
                                transactionRepository.save(savedTransaction);

                                model.addAttribute("pdfReceiptPath", pdfFilePath);
                                logger.info("PDF receipt generated and saved: {}", pdfFilePath);
                            } catch (Exception e) {
                                logger.error("Failed to generate PDF receipt: {}", e.getMessage());
                            }

                            logger.info("=== PAYMENT RETURN PROCESSING COMPLETED ===");
                            return "Receptionists/payment-return";
                        } else {
                            // PAYMENT FAILED - Update ONLY transaction status, keep as Pending for retry
                            if (!"Paid".equals(currentStatus)) {
                                transaction.setStatus("Pending");
                                logger.warn("❌ PAYMENT FAILED - Transaction {} status: {} -> Pending (Response Code: {})",
                                           transactionId, currentStatus, paymentStatus);
                            } else {
                                logger.warn("⚠️ Payment failed but transaction {} is already Paid, not changing status", transactionId);
                            }

                            // Save transaction with updated status for failed payments
                            Transaction savedTransaction = transactionRepository.save(transaction);
                            logger.info("💾 Transaction {} saved with status: {} after failed payment", transactionId, savedTransaction.getStatus());

                            // Get user names for failed payment display
                            String processedByName = "N/A";
                            String customerName = "N/A";
                            String paymentMethod = savedTransaction.getMethod() != null ? savedTransaction.getMethod() : "Banking";

                            if (savedTransaction.getUserId() != null) {
                                Optional<Users> customerUser = userRepository.findById(savedTransaction.getUserId());
                                if (customerUser.isPresent()) {
                                    customerName = customerUser.get().getFullName();
                                }
                            }

                            model.addAttribute("transaction", savedTransaction);
                            model.addAttribute("paymentStatus", paymentStatus);
                            model.addAttribute("amount", amountInVND);
                            model.addAttribute("amountInXu", amountInXu);
                            model.addAttribute("processedByName", processedByName);
                            model.addAttribute("customerName", customerName);
                            model.addAttribute("paymentMethod", paymentMethod);
                            model.addAttribute("errorMessage", "Payment failed. Please try again or contact support.");
                        }
                    } else {
                        logger.error("❌ Transaction not found for ID: {}", transactionId);
                        model.addAttribute("errorMessage", "Transaction not found with ID: " + transactionId);
                    }
                } catch (NumberFormatException e) {
                    logger.error("❌ Invalid transaction ID format: {}", transactionId, e);
                    model.addAttribute("errorMessage", "Invalid transaction ID format");
                }
            } else {
                logger.error("❌ Transaction ID is missing from VNPay response");
                model.addAttribute("errorMessage", "Transaction ID is missing from payment response");
            }

            logger.info("=== PAYMENT RETURN PROCESSING COMPLETED ===");
            return "Receptionists/payment-return";
        } catch (Exception e) {
            logger.error("💥 ERROR handling payment return in ReceptionistController", e);
            model.addAttribute("errorMessage", "An error occurred while processing the payment return: " + e.getMessage());
            return "error";
        }
    }

    private String mapPaymentMethodToDbValue(String paymentMethod) {
        switch (paymentMethod.toLowerCase()) {
            case "vnpay":
            case "momo":
            case "zalopay":
                return "Banking"; // Tất cả payment method online đều là Banking
            case "cash":
                return "Cash"; // Chỉ Cash mới là Cash
            default:
                return "Banking"; // Default to Banking thay vì Cash
        }
    }

    private String generatePaymentReceiptPdf(Transaction transaction, Double amountVND, Double amountReceived, String processedByName, String customerName, String paymentMethod) throws Exception {
        // Prepare PDF document
        Document document = new Document();
        String pdfFilePath = null;

        try {
            // Define file name and path
            String fileName = "Receipt_" + transaction.getTransactionId() + ".pdf";
            String filePath = System.getProperty("user.dir") + "/uploads/receipts/" + fileName;

            // Create uploads directory if it doesn't exist
            File uploadDir = new File(System.getProperty("user.dir") + "/uploads/receipts/");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Create PDF writer
            PdfWriter.getInstance(document, new FileOutputStream(filePath));

            // Open document
            document.open();

            // Add content to PDF
            document.add(new Paragraph("Payment Receipt", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Transaction ID: " + transaction.getTransactionId()));
            document.add(new Paragraph("Appointment ID: " + transaction.getAppointmentId()));
            document.add(new Paragraph("Patient ID: " + transaction.getUserId()));
            document.add(new Paragraph("Amount (VND): " + amountVND));
            document.add(new Paragraph("Amount Received: " + amountReceived));
            document.add(new Paragraph("Payment Method: " + transaction.getMethod()));
            document.add(new Paragraph("Status: " + transaction.getStatus()));
            document.add(new Paragraph("Processed By: " + processedByName));
            document.add(new Paragraph("Customer Name: " + customerName));
            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

            // Close document
            document.close();

            pdfFilePath = filePath;
        } catch (Exception e) {
            logger.error("Error generating PDF receipt: ", e);
            throw e;
        }

        return pdfFilePath;
    }
}
