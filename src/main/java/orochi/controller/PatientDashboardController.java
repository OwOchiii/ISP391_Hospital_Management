package orochi.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.extern.slf4j.Slf4j;
import orochi.model.*;
import orochi.repository.*;
import orochi.config.CustomUserDetails;
import orochi.service.FeedbackService;
import orochi.service.NotificationService;
import orochi.service.PatientService;
import orochi.service.MedicalRecordService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Controller
@RequestMapping("/patient")
@Slf4j
public class PatientDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(PatientDashboardController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientContactRepository patientContactRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MedicalRecordService medicalRecordService;

    private static final List<String> ALLOWED_FEEDBACK_TYPES = Arrays.asList(
            "Quality of medical services", "Facilities", "Administrative procedures",
            "Online booking & application system", "Staff attitude and behavior",
            "Costs & payment", "Suggestions & improvements", "Timing & progress",
            "Safety & security", "Other"
    );

    private static void accept(Doctor doctor) {
        DoctorEducation latestEducation = doctor.getEducations().stream()
                .filter(edu -> edu.getGraduation() != null)
                .max(Comparator.comparing(DoctorEducation::getGraduation))
                .orElse(null);
        doctor.setLatestEducation(latestEducation); // Assumes a new setter in Doctor entity
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            Integer patientId = getCurrentPatientId();

            if (patientId == null) {
                logger.error("No patientId found for authenticated user");
                model.addAttribute("errorMessage", "Patient ID is required. Please contact support.");
                return "error";
            }

            Optional<Patient> patientOpt = patientService.getPatientById(patientId);
            if (!patientOpt.isPresent()) {
                logger.error("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found");
                return "error";
            }

            Patient patient = patientOpt.get();
            model.addAttribute("patientName", patient.getUser().getFullName());

            // Get upcoming appointments
            List<Appointment> upcomingAppointmentsList = patientService.getUpcomingAppointments(patientId);
            int upcomingAppointments = upcomingAppointmentsList != null ? upcomingAppointmentsList.size() : 0;
            model.addAttribute("upcomingAppointments", upcomingAppointments);

            // Get total appointments
            List<Appointment> allAppointments = patientService.getAllAppointments(patientId);
            int totalAppointments = allAppointments != null ? allAppointments.size() : 0;
            model.addAttribute("totalAppointments", totalAppointments);

            // Get active prescriptions
            List<Prescription> activePrescriptions = patientService.getActivePrescriptions(patientId);
            int activePrescriptionsCount = activePrescriptions != null ? activePrescriptions.size() : 0;
            model.addAttribute("activePrescriptions", activePrescriptionsCount);

            // Get last visit date
            String lastVisit = "N/A";
            if (allAppointments != null && !allAppointments.isEmpty()) {
                for (Appointment appointment : allAppointments) {
                    if (appointment.getDateTime().isBefore(LocalDateTime.now())) {
                        lastVisit = formatLastVisitDate(appointment.getDateTime());
                        break;
                    }
                }
            }
            model.addAttribute("lastVisit", lastVisit);

            //Lay thong bao moi nhat va list thong bao
            Integer userId = patient.getUser().getUserId();
            List<Notification> notes = notificationService.findByUserIdOrderByCreatedAtDesc(userId);
            Notification latest = notes.isEmpty() ? null : notes.get(0);
            model.addAttribute("latestNotification", latest);

            logger.info("Dashboard loaded successfully for patient ID: {}", patientId);
            return "patient/dashboard";
        } catch (Exception e) {
            logger.error("Error loading dashboard", e);
            model.addAttribute("errorMessage", "An error occurred while loading the dashboard: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/search-doctor")
    public String searchDoctor(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId != null) {
                model.addAttribute("patientId", patientId);
                Optional<Patient> patientOpt = patientRepository.findById(patientId);
                if (patientOpt.isPresent()) {
                    model.addAttribute("patientName", patientOpt.get().getUser().getFullName());
                }
            }
            List<Doctor> doctors = doctorRepository.findAll();
            // Pre-process doctors to include latest education
            doctors.forEach(PatientDashboardController::accept);
            model.addAttribute("doctors", doctors);
            List<Specialization> specializations = specializationRepository.findAll();
            model.addAttribute("specializations", specializations);
            return "patient/search-doctor";
        } catch (Exception e) {
            logger.error("Error loading search doctor page: " + e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the search doctor page.");
            return "error";
        }
    }

    @GetMapping("/appointment-list")
    public String appointmentList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required");
                return "error";
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();
                model.addAttribute("patientName", patient.getUser().getFullName());
            }

            int pageSize = 5;
            Pageable pageable = PageRequest.of(page, pageSize, Sort.by("dateTime").descending());

            String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
            String statusFilter = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
            LocalDateTime fromDateTime = null;
            LocalDateTime toDateTime = null;

            if (fromDate != null && !fromDate.trim().isEmpty()) {
                try {
                    fromDateTime = LocalDateTime.parse(fromDate + "T00:00:00");
                } catch (DateTimeParseException e) {
                    model.addAttribute("errorMessage", "Invalid From Date format. Please use YYYY-MM-DD.");
                    return "patient/appointment-list";
                }
            }
            if (toDate != null && !toDate.trim().isEmpty()) {
                try {
                    toDateTime = LocalDateTime.parse(toDate + "T23:59:59");
                    if (fromDateTime != null && toDateTime.isBefore(fromDateTime)) {
                        model.addAttribute("errorMessage", "To Date cannot be before From Date.");
                        return "patient/appointment-list";
                    }
                } catch (DateTimeParseException e) {
                    model.addAttribute("errorMessage", "Invalid To Date format. Please use YYYY-MM-DD.");
                    return "patient/appointment-list";
                }
            }

            Page<Appointment> appointmentPage = appointmentRepository.findByPatientIdWithFilters(
                    patientId, searchTerm, statusFilter, fromDateTime, toDateTime, pageable);

            long scheduledCount = appointmentRepository.countByPatientIdAndStatus(patientId, "Scheduled");
            long pendingCount = appointmentRepository.countByPatientIdAndStatus(patientId, "Pending");
            long completedCount = appointmentRepository.countByPatientIdAndStatus(patientId, "Completed");
            long cancelledCount = appointmentRepository.countByPatientIdAndStatus(patientId, "Cancel");

            model.addAttribute("appointments", appointmentPage.getContent());
            model.addAttribute("totalPages", appointmentPage.getTotalPages());
            model.addAttribute("currentPage", appointmentPage.getNumber());
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("patientId", patientId);
            model.addAttribute("totalAppointments", appointmentPage.getTotalElements());
            model.addAttribute("scheduledAppointments", scheduledCount);
            model.addAttribute("pendingAppointments", pendingCount);
            model.addAttribute("completedAppointments", completedCount);
            model.addAttribute("cancelledAppointments", cancelledCount);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);

            return "patient/appointment-list";
        } catch (Exception e) {
            logger.error("Error loading appointment list", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/appointment-list/{id}/cancel")
    public String showCancelAppointmentPage(@PathVariable("id") Integer appointmentId,
                                            @RequestParam("patientId") Integer patientId,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        try {
            Integer currentPatientId = getCurrentPatientId();
            if (currentPatientId == null || !currentPatientId.equals(patientId)) {
                logger.error("Unauthorized access attempt for patient ID: {}", patientId);
                redirectAttributes.addFlashAttribute("successMessage", "Invalid patient ID! Please try again.");
                return "redirect:/patient/appointment-list";
            }

            // Get patient info
            Patient patient;
            try {
                patient = patientRepository.findById(patientId)
                        .orElseThrow(() -> new RuntimeException("Patient not found for ID: " + patientId));
            } catch (RuntimeException e) {
                logger.error("Patient not found for ID: {}", patientId);
                redirectAttributes.addFlashAttribute("successMessage", "Invalid patient ID! Please try again.");
                return "redirect:/patient/appointment-list";
            }
            model.addAttribute("patientName", patient.getUser().getFullName());
            model.addAttribute("patientId", patientId);

            // Get appointment info
            Appointment appointment;
            try {
                appointment = appointmentRepository.findById(appointmentId)
                        .orElseThrow(() -> new RuntimeException("Appointment not found for ID: " + appointmentId));
            } catch (RuntimeException e) {
                logger.error("Appointment not found for ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("successMessage", "Invalid appointment ID or patient ID! Please try again.");
                return "redirect:/patient/appointment-list";
            }

            // Verify appointment belongs to the patient
            if (!appointment.getPatient().getPatientId().equals(patientId)) {
                logger.error("Appointment ID: {} does not belong to patient ID: {}", appointmentId, patientId);
                redirectAttributes.addFlashAttribute("successMessage", "Invalid appointment ID or patient ID! Please try again.");
                return "redirect:/patient/appointment-list";
            }

            // Check if appointment is cancellable
            if (!"Scheduled".equals(appointment.getStatus()) && !"Pending".equals(appointment.getStatus())) {
                logger.warn("Attempt to cancel non-scheduled appointment ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("successMessage", "Only scheduled or pending appointments can be cancelled.");
                return "redirect:/patient/appointment-list";
            }

            model.addAttribute("appointment", appointment);
            model.addAttribute("previousDescription", appointment.getDescription() != null ? appointment.getDescription() : "No description provided");

            logger.info("Cancel appointment page loaded for appointment ID: {}, patient ID: {}", appointmentId, patientId);
            return "patient/cancel-appointment";
        } catch (Exception e) {
            logger.error("Error loading cancel appointment page for appointment ID: {}", appointmentId, e);
            redirectAttributes.addFlashAttribute("successMessage", "An error occurred while loading the cancellation page. Please try again.");
            return "redirect:/patient/appointment-list";
        }
    }

    @PostMapping("/appointment-list/{id}/cancel")
    public String confirmCancelAppointment(@PathVariable("id") Integer appointmentId,
                                           @RequestParam("patientId") Integer patientId,
                                           @RequestParam("description") String cancelReason,
                                           RedirectAttributes redirectAttributes) {
        try {
            Integer currentPatientId = getCurrentPatientId();
            if (currentPatientId == null || !currentPatientId.equals(patientId)) {
                logger.error("Unauthorized access attempt for patient ID: {}", patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access or invalid patient ID");
                return "redirect:/patient/appointment-list";
            }

            // Get appointment info
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (!appointmentOpt.isPresent()) {
                logger.error("Appointment not found for ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found");
                return "redirect:/patient/appointment-list";
            }
            Appointment appointment = appointmentOpt.get();

            // Verify appointment belongs to the patient
            if (!appointment.getPatient().getPatientId().equals(patientId)) {
                logger.error("Appointment ID: {} does not belong to patient ID: {}", appointmentId, patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to cancel this appointment");
                return "redirect:/patient/appointment-list";
            }

            // Check if appointment is cancellable
            if (!"Scheduled".equals(appointment.getStatus()) && !"Pending".equals(appointment.getStatus())) {
                logger.warn("Attempt to cancel non-scheduled appointment ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("errorMessage", "Only scheduled appointments can be cancelled");
                return "redirect:/patient/appointment-list";
            }

            // Trim and normalize cancellation reason
            String trimmedReason = cancelReason.trim().replaceAll("\\s+", " ");
            if (trimmedReason.isEmpty()) {
                logger.warn("No cancellation reason provided for appointment ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("errorMessage", "Please provide a reason for cancellation");
                return "redirect:/patient/appointment-list/" + appointmentId + "/cancel?patientId=" + patientId;
            }
            if (trimmedReason.length() > 250) {
                trimmedReason = trimmedReason.substring(0, 250);
            }

            // Update appointment status and description
            appointment.setStatus("Cancel");
            appointment.setDescription(trimmedReason);
            appointmentRepository.save(appointment);

            logger.info("Appointment ID {} cancelled successfully by patient ID: {}", appointmentId, patientId);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment cancelled successfully");
            return "redirect:/patient/appointment-list";
        } catch (Exception e) {
            logger.error("Error cancelling appointment ID: {}", appointmentId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel appointment: " + e.getMessage());
            return "redirect:/patient/appointment-list";
        }
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required");
                return "error";
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                logger.error("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found");
                return "error";
            }
            Patient patient = patientOpt.get();
            model.addAttribute("patient", patient);
            model.addAttribute("patientName", patient.getUser().getFullName());
            model.addAttribute("patientId", patientId);

            // Fetch the most recent medical record
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.getMedicalRecordByPatientId(patientId);
            model.addAttribute("medicalRecord", medicalRecordOpt.orElse(null));

            return "patient/profile";
        } catch (Exception e) {
            logger.error("Error loading profile", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/update-profile")
    public String showUpdateProfileForm(Model model) {
        Integer patientId = getCurrentPatientId();
        if (patientId == null) {
            model.addAttribute("errorMessage", "Patient ID is required");
            return "error";
        }

        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (!patientOpt.isPresent()) {
            model.addAttribute("errorMessage", "Patient not found");
            return "error";
        }

        Patient patient = patientOpt.get();
        Users user = patient.getUser();
        String streetAddress = "";
        String city = "";
        String country = "Vietnam"; // Default to Vietnam
        if (patient.getContacts() != null && !patient.getContacts().isEmpty()) {
            PatientContact contact = patient.getContacts().get(0);
            streetAddress = contact.getStreetAddress();
            city = contact.getCity();
            country = contact.getCountry();
        }

        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhoneNumber(user.getPhoneNumber());
        form.setDateOfBirth(patient.getDateOfBirth());
        form.setGender(patient.getGender());
        form.setDescription(patient.getDescription());
        form.setStreetAddress(streetAddress);
        form.setCity(city);
        form.setCountry(country);

        // List of all 63 cities/provinces in Vietnam
        List<String> cities = Arrays.asList(
                "An Giang", "Ba Ria-Vung Tau", "Bac Giang", "Bac Kan", "Bac Lieu", "Bac Ninh", "Ben Tre",
                "Binh Dinh", "Binh Duong", "Binh Phuoc", "Binh Thuan", "Ca Mau", "Can Tho", "Cao Bang",
                "Da Nang", "Dak Lak", "Dak Nong", "Dien Bien", "Dong Nai", "Dong Thap", "Gia Lai",
                "Ha Giang", "Ha Nam", "Ha Noi", "Ha Tinh", "Hai Duong", "Hai Phong", "Hau Giang",
                "Ho Chi Minh City", "Hoa Binh", "Hung Yen", "Khanh Hoa", "Kien Giang", "Kon Tum",
                "Lai Chau", "Lam Dong", "Lang Son", "Lao Cai", "Long An", "Nam Dinh", "Nghe An",
                "Ninh Binh", "Ninh Thuan", "Phu Tho", "Phu Yen", "Quang Binh", "Quang Nam", "Quang Ngai",
                "Quang Ninh", "Quang Tri", "Soc Trang", "Son La", "Tay Ninh", "Thai Binh", "Thai Nguyen",
                "Thanh Hoa", "Thua Thien Hue", "Tien Giang", "Tra Vinh", "Tuyen Quang", "Vinh Long",
                "Vinh Phuc", "Yen Bai"
        );

        model.addAttribute("profileForm", form);
        model.addAttribute("patientId", patientId);
        model.addAttribute("patientName", user.getFullName());
        model.addAttribute("cities", cities);

        return "patient/update-profile";
    }

    @PostMapping("/update-profile")
    @Transactional
    public String updateProfile(@ModelAttribute("profileForm") @Valid ProfileUpdateForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes, Model model) {
        System.out.println("Processing update-profile with form: " + form);

        Integer patientId = getCurrentPatientId();
        if (patientId == null) {
            System.out.println("Patient ID is null");
            redirectAttributes.addFlashAttribute("errorMessage", "Patient ID is required");
            return "redirect:/patient/profile";
        }

        // Ensure country is always Vietnam
        form.setCountry("Vietnam");

        if (bindingResult.hasErrors()) {
            System.out.println("Validation errors found: " + bindingResult.getAllErrors());
            // Re-add cities to the model for form re-rendering
            List<String> cities = Arrays.asList(
                    "An Giang", "Ba Ria-Vung Tau", "Bac Giang", "Bac Kan", "Bac Lieu", "Bac Ninh", "Ben Tre",
                    "Binh Dinh", "Binh Duong", "Binh Phuoc", "Binh Thuan", "Ca Mau", "Can Tho", "Cao Bang",
                    "Da Nang", "Dak Lak", "Dak Nong", "Dien Bien", "Dong Nai", "Dong Thap", "Gia Lai",
                    "Ha Giang", "Ha Nam", "Ha Noi", "Ha Tinh", "Hai Duong", "Hai Phong", "Hau Giang",
                    "Ho Chi Minh City", "Hoa Binh", "Hung Yen", "Khanh Hoa", "Kien Giang", "Kon Tum",
                    "Lai Chau", "Lam Dong", "Lang Son", "Lao Cai", "Long An", "Nam Dinh", "Nghe An",
                    "Ninh Binh", "Ninh Thuan", "Phu Tho", "Phu Yen", "Quang Binh", "Quang Nam", "Quang Ngai",
                    "Quang Ninh", "Quang Tri", "Soc Trang", "Son La", "Tay Ninh", "Thai Binh", "Thai Nguyen",
                    "Thanh Hoa", "Thua Thien Hue", "Tien Giang", "Tra Vinh", "Tuyen Quang", "Vinh Long",
                    "Vinh Phuc", "Yen Bai"
            );
            model.addAttribute("cities", cities);
            return "patient/update-profile";
        }

        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (!patientOpt.isPresent()) {
            System.out.println("Patient not found for ID: " + patientId);
            redirectAttributes.addFlashAttribute("errorMessage", "Patient not found");
            return "redirect:/patient/profile";
        }

        Patient patient = patientOpt.get();
        Users user = patient.getUser();
        System.out.println("Found patient ID: " + patient.getPatientId() + ", user ID: " + user.getUserId());

        try {
            user.setFullName(form.getFullName());
            userRepository.save(user);
            // Update Patient entity
            patient.setDateOfBirth(form.getDateOfBirth());
            patient.setGender(form.getGender());
            patient.setDescription(form.getDescription());
            System.out.println("Saving patient ID: " + patient.getPatientId());
            patientRepository.save(patient);

            // Update or create PatientContact
            Optional<PatientContact> contactOpt = patientContactRepository.findFirstByPatientId(patientId);
            if (contactOpt.isPresent()) {
                PatientContact contact = contactOpt.get();
                contact.setStreetAddress(form.getStreetAddress());
                contact.setCity(form.getCity());
                contact.setCountry("Vietnam");
                System.out.println("Saving existing contact ID: " + contact.getContactId());
                patientContactRepository.save(contact);
            } else {
                PatientContact newContact = new PatientContact();
                newContact.setPatientId(patientId);
                newContact.setAddressType("Primary");
                newContact.setStreetAddress(form.getStreetAddress());
                newContact.setCity(form.getCity());
                newContact.setCountry("Vietnam");
                System.out.println("Saving new contact for patient ID: " + patientId);
                patientContactRepository.save(newContact);
            }

            System.out.println("Profile updated successfully for patient ID: " + patientId);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");
            return "redirect:/patient/profile";
        } catch (Exception e) {
            System.out.println("Error updating profile for patient ID: " + patientId + ", error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
            // Re-add cities to the model for form re-rendering
            List<String> cities = Arrays.asList(
                    "An Giang", "Ba Ria-Vung Tau", "Bac Giang", "Bac Kan", "Bac Lieu", "Bac Ninh", "Ben Tre",
                    "Binh Dinh", "Binh Duong", "Binh Phuoc", "Binh Thuan", "Ca Mau", "Can Tho", "Cao Bang",
                    "Da Nang", "Dak Lak", "Dak Nong", "Dien Bien", "Dong Nai", "Dong Thap", "Gia Lai",
                    "Ha Giang", "Ha Nam", "Ha Noi", "Ha Tinh", "Hai Duong", "Hai Phong", "Hau Giang",
                    "Ho Chi Minh City", "Hoa Binh", "Hung Yen", "Khanh Hoa", "Kien Giang", "Kon Tum",
                    "Lai Chau", "Lam Dong", "Lang Son", "Lao Cai", "Long An", "Nam Dinh", "Nghe An",
                    "Ninh Binh", "Ninh Thuan", "Phu Tho", "Phu Yen", "Quang Binh", "Quang Nam", "Quang Ngai",
                    "Quang Ninh", "Quang Tri", "Soc Trang", "Son La", "Tay Ninh", "Thai Binh", "Thai Nguyen",
                    "Thanh Hoa", "Thua Thien Hue", "Tien Giang", "Tra Vinh", "Tuyen Quang", "Vinh Long",
                    "Vinh Phuc", "Yen Bai"
            );
            model.addAttribute("cities", cities);
            return "patient/update-profile";
        }
    }

    @GetMapping("/feedback")
    public String feedback(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required");
                return "error";
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            Patient patient = patientOpt.orElseThrow(() -> new Exception("Patient not found"));
            model.addAttribute("userId", patient.getUser().getUserId());
            model.addAttribute("patientId", patientId);
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");

            return "patient/feedback";
        } catch (Exception e) {
            logger.error("Error loading feedback page", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/feedback")
    public String submitFeedback(
            @RequestParam("feedbackType") String feedbackType,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required");
                return "error";
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                model.addAttribute("errorMessage", "Patient not found");
                return "error";
            }
            Integer userId = patientOpt.get().getUser().getUserId();

            String trimmedFeedbackType = feedbackType.trim();
            String trimmedTitle = title.trim().replaceAll("\\s+", " ");
            String trimmedDescription = description.trim().replaceAll("\\s+", " ");

            if (trimmedFeedbackType.isEmpty() || trimmedTitle.isEmpty() || trimmedDescription.isEmpty()) {
                model.addAttribute("errorMessage", "All fields are required");
                return "patient/feedback";
            }

            if (!ALLOWED_FEEDBACK_TYPES.contains(trimmedFeedbackType)) {
                model.addAttribute("errorMessage", "Invalid feedback type");
                return "patient/feedback";
            }

            if (trimmedTitle.length() > 100) {
                trimmedTitle = trimmedTitle.substring(0, 100);
            }

            if (trimmedDescription.length() > 250) {
                trimmedDescription = trimmedDescription.substring(0, 250);
            }

            Feedback feedback = new Feedback();
            feedback.setUserId(userId);
            feedback.setFeedbackType(trimmedFeedbackType);
            feedback.setTitle(trimmedTitle);
            feedback.setDescription(trimmedDescription);

            feedbackService.saveFeedback(feedback);
            model.addAttribute("successMessage", "Feedback submitted successfully");
            model.addAttribute("userId", userId);
            model.addAttribute("patientId", patientId);
            model.addAttribute("patientName", patientOpt.get().getUser().getFullName());
            return "redirect:/patient/feedback";
        } catch (Exception e) {
            logger.error("Error submitting feedback", e);
            model.addAttribute("errorMessage", "Failed to submit feedback: " + e.getMessage());
            model.addAttribute("feedbackType", feedbackType);
            model.addAttribute("title", title);
            model.addAttribute("description", description);
            return "patient/feedback";
        }
    }

    @GetMapping("/my-feedback")
    public String myFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String feedbackType,
            Model model) {
        try {
            if (page < 0) {
                page = 0;
            }

            Integer patientId = getCurrentPatientId();
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            Patient patient = patientOpt.orElseThrow(() -> new Exception("Patient not found"));
            Integer userId = patient.getUser().getUserId();

            model.addAttribute("userId", userId);
            model.addAttribute("patientId", patientId);
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");
            model.addAttribute("feedbackTypes", ALLOWED_FEEDBACK_TYPES); // Add feedback types for dropdown

            Pageable pageable = PageRequest.of(page, size);
            Page<Feedback> feedbackPage = null;

            String fromDateTrimmed = (fromDate != null) ? fromDate.trim() : null;
            String toDateTrimmed = (toDate != null) ? toDate.trim() : null;
            String feedbackTypeTrimmed = (feedbackType != null) ? feedbackType.trim() : null;

            LocalDateTime start = null, end = null;
            if (fromDateTrimmed != null && !fromDateTrimmed.isEmpty()) {
                try {
                    start = LocalDateTime.parse(fromDateTrimmed + "T00:00:00");
                } catch (DateTimeParseException e) {
                    model.addAttribute("errorMessage", "Invalid From Date format. Please use YYYY-MM-DD.");
                    model.addAttribute("feedbacks", Collections.emptyList());
                    model.addAttribute("currentPage", 0);
                    model.addAttribute("totalPages", 0);
                    model.addAttribute("size", size);
                    model.addAttribute("fromDate", fromDate);
                    model.addAttribute("toDate", toDate);
                    model.addAttribute("feedbackType", feedbackType);
                    return "patient/my-feedback";
                }
            }
            if (toDateTrimmed != null && !toDateTrimmed.isEmpty()) {
                try {
                    end = LocalDateTime.parse(toDateTrimmed + "T23:59:59");
                    if (start != null && end.isBefore(start)) {
                        model.addAttribute("errorMessage", "To Date cannot be before From Date.");
                        model.addAttribute("feedbacks", Collections.emptyList());
                        model.addAttribute("currentPage", 0);
                        model.addAttribute("totalPages", 0);
                        model.addAttribute("size", size);
                        model.addAttribute("fromDate", fromDate);
                        model.addAttribute("toDate", toDate);
                        model.addAttribute("feedbackType", feedbackType);
                        return "patient/my-feedback";
                    }
                } catch (DateTimeParseException e) {
                    model.addAttribute("errorMessage", "Invalid To Date format. Please use YYYY-MM-DD.");
                    model.addAttribute("feedbacks", Collections.emptyList());
                    model.addAttribute("currentPage", 0);
                    model.addAttribute("totalPages", 0);
                    model.addAttribute("size", size);
                    model.addAttribute("fromDate", fromDate);
                    model.addAttribute("toDate", toDate);
                    model.addAttribute("feedbackType", feedbackType);
                    return "patient/my-feedback";
                }
            }
            if (feedbackTypeTrimmed != null && !feedbackTypeTrimmed.isEmpty() && !ALLOWED_FEEDBACK_TYPES.contains(feedbackTypeTrimmed)) {
                model.addAttribute("errorMessage", "Invalid Feedback Type.");
                model.addAttribute("feedbacks", Collections.emptyList());
                model.addAttribute("currentPage", 0);
                model.addAttribute("totalPages", 0);
                model.addAttribute("size", size);
                model.addAttribute("fromDate", fromDate);
                model.addAttribute("toDate", toDate);
                model.addAttribute("feedbackType", feedbackType);
                return "patient/my-feedback";
            }

            if (feedbackTypeTrimmed != null && !feedbackTypeTrimmed.isEmpty()) {
                if (start != null && end != null) {
                    feedbackPage = feedbackService.getFeedbackByUserIdAndTypeAndDateRange(userId, feedbackTypeTrimmed, start, end, pageable);
                } else if (start != null) {
                    feedbackPage = feedbackService.getFeedbackByUserIdAndTypeAndDateRange(userId, feedbackTypeTrimmed, start, LocalDateTime.now(), pageable);
                } else if (end != null) {
                    feedbackPage = feedbackService.getFeedbackByUserIdAndTypeAndDateRange(userId, feedbackTypeTrimmed, LocalDateTime.of(1900, 1, 1, 0, 0), end, pageable);
                } else {
                    feedbackPage = feedbackService.getFeedbackByUserIdAndType(userId, feedbackTypeTrimmed, pageable);
                }
            } else {
                if (start != null && end != null) {
                    feedbackPage = feedbackService.getFeedbackByUserIdAndDateRange(userId, start, end, pageable);
                } else if (start != null) {
                    feedbackPage = feedbackService.getFeedbackByUserIdAndDateRange(userId, start, LocalDateTime.now(), pageable);
                } else if (end != null) {
                    feedbackPage = feedbackService.getFeedbackByUserIdAndDateRange(userId, LocalDateTime.of(1900, 1, 1, 0, 0), end, pageable);
                } else {
                    feedbackPage = feedbackService.getFeedbackByUserId(userId, pageable);
                }
            }

            model.addAttribute("feedbacks", feedbackPage != null ? feedbackPage.getContent() : Collections.emptyList());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", feedbackPage != null ? feedbackPage.getTotalPages() : 0);
            model.addAttribute("size", size);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            model.addAttribute("feedbackType", feedbackType);
            return "patient/my-feedback";
        } catch (Exception e) {
            logger.error("Error fetching feedback", e);
            model.addAttribute("errorMessage", "Failed to fetch feedback: " + e.getMessage());
            model.addAttribute("feedbacks", Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("size", size);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            model.addAttribute("feedbackType", feedbackType);
            return "patient/my-feedback";
        }
    }

    @PostMapping("/delete-feedback")
    public String deleteFeedback(@RequestParam("feedbackId") Integer feedbackId) {
        feedbackService.deleteFeedback(feedbackId);
        return "redirect:/patient/my-feedback";
    }

    @GetMapping("/appointment-list/{id}/report")
    public String getMedicalReport(@PathVariable("id") Integer appointmentId,
                                   @RequestParam("patientId") Integer patientId,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            Integer currentPatientId = getCurrentPatientId();
            if (currentPatientId == null || !currentPatientId.equals(patientId)) {
                logger.error("Unauthorized access attempt for patient ID: {}", patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access");
                return "redirect:/patient/appointment-list";
            }

            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (!appointmentOpt.isPresent()) {
                logger.error("Appointment not found for ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found");
                return "redirect:/patient/appointment-list";
            }
            Appointment appointment = appointmentOpt.get();

            if (!appointment.getPatientId().equals(patientId)) {
                logger.error("Appointment ID: {} does not belong to patient ID: {}", appointmentId, patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to view this report");
                return "redirect:/patient/appointment-list";
            }

            if (!"Completed".equals(appointment.getStatus())) {
                logger.warn("Attempt to view medical report for non-completed appointment ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("errorMessage", "Medical report available only for completed appointments");
                return "redirect:/patient/appointment-list";
            }

            // Fetch prescriptions for the appointment
            List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointmentId);
            Prescription prescription = prescriptions.isEmpty() ? null : prescriptions.get(0);

            // Get medicines from the prescription, if it exists
            List<Medicine> medicines = prescription != null ? prescription.getMedicines() : List.of();

            // Add data to model
            model.addAttribute("appointmentId", appointmentId);
            model.addAttribute("patientId", patientId);
            model.addAttribute("patientName", appointment.getPatient().getFullName());
            model.addAttribute("prescription", prescription);
            model.addAttribute("medicines", medicines);

            return "patient/medical-report";
        } catch (Exception e) {
            logger.error("Error retrieving medical report for appointment ID: {}", appointmentId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to load medical report: " + e.getMessage());
            return "redirect:/patient/appointment-list";
        }
    }

    @GetMapping("/payment-history")
    public String paymentHistory(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                logger.error("No patientId found for authenticated user");
                model.addAttribute("errorMessage", "Patient ID is required. Please contact support.");
                return "error";
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                logger.error("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found");
                return "error";
            }

            Patient patient = patientOpt.get();
            Integer userId = patient.getUser().getUserId();

            // Get all transactions for the patient
            List<Transaction> transactions = transactionRepository.findByUserIdOrderByTimeOfPaymentDesc(userId);

            // Count transactions by status
            long paidCount = transactions.stream()
                    .filter(t -> "Paid".equals(t.getStatus()))
                    .count();

            long refundedCount = transactions.stream()
                    .filter(t -> "Refunded".equals(t.getStatus()))
                    .count();

            long pendingCount = transactions.stream()
                    .filter(t -> "Pending".equals(t.getStatus()))
                    .count();

            // Count transactions by method
            long cashCount = transactions.stream()
                    .filter(t -> "Cash".equals(t.getMethod()))
                    .count();

            long bankingCount = transactions.stream()
                    .filter(t -> "Banking".equals(t.getMethod()))
                    .count();

            // Add attributes to model
            model.addAttribute("userId", userId);
            model.addAttribute("patientId", patientId);
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");
            model.addAttribute("transactions", transactions);
            model.addAttribute("totalTransactions", transactions.size());
            model.addAttribute("paidTransactions", paidCount);
            model.addAttribute("refundedTransactions", refundedCount);
            model.addAttribute("pendingTransactions", pendingCount);
            model.addAttribute("cashTransactions", cashCount);
            model.addAttribute("bankingTransactions", bankingCount);

            logger.info("Payment history loaded successfully for patient ID: {}", patientId);
            return "patient/payment-history";
        } catch (Exception e) {
            logger.error("Error loading payment history for patient ID: {}", getCurrentPatientId(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the payment history: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/payment-details/{transactionId}")
    public String paymentDetails(@PathVariable("transactionId") Integer transactionId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                logger.error("No patientId found for authenticated user");
                redirectAttributes.addFlashAttribute("errorMessage", "Patient ID is required. Please contact support.");
                return "redirect:/patient/payment-history";
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                logger.error("Patient not found for ID: {}", patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "Patient not found");
                return "redirect:/patient/payment-history";
            }
            Patient patient = patientOpt.get();
            Integer userId = patient.getUser().getUserId();

            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (!transactionOpt.isPresent()) {
                logger.error("Transaction not found for ID: {}", transactionId);
                redirectAttributes.addFlashAttribute("errorMessage", "Transaction not found");
                return "redirect:/patient/payment-history";
            }
            Transaction transaction = transactionOpt.get();

            if (!transaction.getUserId().equals(userId)) {
                logger.error("Transaction ID: {} does not belong to patient ID: {}", transactionId, patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to view this transaction's details");
                return "redirect:/patient/payment-history";
            }

            Receipt receipt = transaction.getReceipt();
            if (receipt == null) {
                logger.warn("No receipt found for transaction ID: {}", transactionId);
                model.addAttribute("errorMessage", "No receipt available for this transaction");
            } else {
                model.addAttribute("receipt", receipt);
            }

            model.addAttribute("patientName", patient.getUser().getFullName());
            model.addAttribute("patientId", patientId);

            return "patient/payment-details";
        } catch (Exception e) {
            logger.error("Error loading payment details for transaction ID: {}", transactionId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while loading the payment details: " + e.getMessage());
            return "redirect:/patient/payment-history";
        }
    }

    @GetMapping("/security-password")
    public String securityPassword(Model model) {
        Integer patientId = getCurrentPatientId(); // Assume this method exists to get authenticated patient ID
        if (patientId == null) {
            model.addAttribute("errorMessage", "Patient ID is required");
            return "error";
        }

        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (!patientOpt.isPresent()) {
            model.addAttribute("errorMessage", "Patient not found");
            return "error";
        }

        Patient patient = patientOpt.get();
        model.addAttribute("patientId", patientId);
        model.addAttribute("patientName", patient.getUser().getFullName());

        return "patient/security-password";
    }

    @PostMapping("/security-password")
    public String changePassword(@RequestParam Integer patientId,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Validate input data
            if (currentPassword == null || newPassword == null || confirmPassword == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "All password fields are required");
                return "redirect:/patient/security-password";
            }

            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match");
                return "redirect:/patient/security-password";
            }

            if (newPassword.length() < 8) {
                redirectAttributes.addFlashAttribute("errorMessage", "New password must be at least 8 characters long");
                return "redirect:/patient/security-password";
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Patient not found");
                return "redirect:/patient/security-password";
            }

            Patient patient = patientOpt.get();
            Users user = patient.getUser();
            if (user == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User account not found");
                return "redirect:/patient/security-password";
            }

            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect");
                return "redirect:/patient/security-password";
            }

            if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("errorMessage", "New password cannot be the same as the current password");
                return "redirect:/patient/security-password";
            }

            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
            return "redirect:/patient/security-password";
        } catch (Exception e) {
            logger.error("Error changing password", e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while changing password");
            return "redirect:/patient/security-password";
        }
    }

    @GetMapping("/notifications")
    public String viewNotifications(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                logger.error("No patientId found for authenticated user");
                model.addAttribute("errorMessage", "Patient ID is required. Please contact support.");
                return "error";
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                logger.error("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found");
                return "error";
            }

            Patient patient = patientOpt.get();
            Integer userId = patient.getUser().getUserId();
            List<Notification> notifications = notificationService.findByUserIdOrderByCreatedAtDesc(userId);

            // Log notification details for debugging
            notifications.forEach(n -> logger.debug("Notification ID: {}, IsRead: {}", n.getNotificationId(), n.isRead()));

            model.addAttribute("notifications", notifications);
            model.addAttribute("totalNotifications", notifications.size());
            model.addAttribute("unreadNotifications", notifications.stream().filter(n -> !n.isRead()).count());
            model.addAttribute("readNotifications", notifications.stream().filter(Notification::isRead).count());
            model.addAttribute("patientId", patientId);
            model.addAttribute("patientName", patient.getUser().getFullName());

            logger.info("Notifications loaded successfully for patient ID: {}, user ID: {}, total: {}, unread: {}, read: {}",
                    patientId, userId, notifications.size(), notifications.stream().filter(n -> !n.isRead()).count(), notifications.stream().filter(Notification::isRead).count());
            return "patient/notifications";
        } catch (Exception e) {
            logger.error("Error loading notifications for patient ID: {}", getCurrentPatientId(), e);
            model.addAttribute("errorMessage", "An error occurred while loading notifications: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/notifications/{id}/read")
    @Transactional
    @ResponseBody
    public ResponseEntity<?> markNotificationAsRead(@PathVariable("id") Integer notificationId) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                logger.error("No patientId found for authenticated user");
                return ResponseEntity.badRequest().body("Patient ID is required");
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                logger.error("Patient not found for ID: {}", patientId);
                return ResponseEntity.notFound().build();
            }

            Patient patient = patientOpt.get();
            Integer userId = patient.getUser().getUserId();
            Notification notification = notificationService.findByIdAndUserId(notificationId, userId);
            if (notification == null) {
                logger.error("Notification ID: {} not found for user ID: {}", notificationId, userId);
                return ResponseEntity.notFound().build();
            }

            logger.debug("Before update: Notification ID: {}, IsRead: {}", notification.getNotificationId(), notification.isRead());
            notification.setRead(true);
            notificationService.save(notification);
            logger.debug("After update: Notification ID: {}, IsRead: {}", notification.getNotificationId(), notification.isRead());

            // Verify database state
            Notification updatedNotification = notificationService.findById(notificationId)
                    .orElseThrow(() -> new IllegalStateException("Notification not found after save"));
            logger.debug("Database state: Notification ID: {}, IsRead: {}",
                    updatedNotification.getNotificationId(), updatedNotification.isRead());

            logger.info("Notification ID: {} marked as read for patient ID: {}, user ID: {}",
                    notificationId, patientId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error marking notification ID: {} as read", notificationId, e);
            return ResponseEntity.status(500).body("Failed to mark notification as read: " + e.getMessage());
        }
    }

    private Integer getCurrentPatientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getPatientId();
        }
        return null;
    }

    private String formatLastVisitDate(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate());

        if (daysDifference == 0) {
            return "Today";
        } else if (daysDifference == 1) {
            return "Yesterday";
        } else if (daysDifference < 7) {
            return daysDifference + " days ago";
        } else if (daysDifference < 30) {
            long weeks = daysDifference / 7;
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            return dateTime.format(formatter);
        }
    }

    @PostMapping("/notifications/mark-read/{id}")
    @Transactional
    @ResponseBody
    public ResponseEntity<?> markReadViaAlias(@PathVariable("id") Integer notificationId) {
        return markNotificationAsRead(notificationId);
    }
}

// New form-backing object
class PasswordForm {
    private Integer patientId;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    // Getters and setters
    public Integer getPatientId() { return patientId; }
    public void setPatientId(Integer patientId) { this.patientId = patientId; }
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}