package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.*;
import orochi.repository.DoctorRepository;
import orochi.repository.MedicalOrderRepository;
import orochi.repository.NotificationRepository;
import orochi.service.DoctorService;
import orochi.service.MedicalRecordService;
import orochi.config.CustomUserDetails;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorController.class);

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @GetMapping("/dashboard")
    public String getDashboard(@RequestParam(required = false) Integer doctorId,
                               Model model,
                               Authentication authentication) {
        try {
            // If doctorId is not provided, try to get it from the authentication
            if (doctorId == null && authentication != null) {
                logger.info("No doctorId provided, attempting to retrieve from authenticated user");

                Object principal = authentication.getPrincipal();

                if (principal instanceof CustomUserDetails) {
                    orochi.config.CustomUserDetails userDetails = (orochi.config.CustomUserDetails) principal;
                    doctorId = userDetails.getDoctorId();

                    if (doctorId == null) {
                        logger.error("No doctorId found in CustomUserDetails for user: {}", userDetails.getUsername());
                        model.addAttribute("errorMessage", "Doctor ID is required. Please contact support.");
                        return "error";
                    }

                    logger.info("Retrieved doctorId {} from authentication", doctorId);
                } else {
                    logger.error("Authentication principal is not of CustomUserDetails type: {}",
                            principal != null ? principal.getClass().getName() : "null");
                    model.addAttribute("errorMessage", "Authentication error. Please log in again.");
                    return "error";
                }
            }


            if (doctorId == null) {
                logger.error("No doctorId provided and could not be determined from authentication");
                model.addAttribute("errorMessage", "Doctor ID is required");
                return "error";
            }

            logger.info("Loading dashboard for doctor ID: {}", doctorId);
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);

            // Check if doctor is null before accessing its properties
            if (doctor == null) {
                logger.error("Doctor not found for ID: {}", doctorId);
                model.addAttribute("errorMessage", "Doctor not found");
                return "error";
            }

            model.addAttribute("doctorName", doctor.getUser().getFullName());
            List<Appointment> todayAppointments = doctorService.getTodayAppointments(doctorId);
            if (todayAppointments == null) {
                todayAppointments = new ArrayList<>();
            }

            List<Appointment> upcomingAppointments = doctorService.getUpcomingAppointments(doctorId);
            if (upcomingAppointments == null) {
                upcomingAppointments = new ArrayList<>();
            }

            List<MedicalOrder> pendingOrders = medicalOrderRepository.findByDoctorIdAndStatus(doctorId, "Pending");
            if (pendingOrders == null) {
                pendingOrders = new ArrayList<>();
            }

            List<Patient> patients = doctorService.findAllPatients();
            if (patients == null) {
                patients = new ArrayList<>();
            }

            // Fetch notifications for the doctor
            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(doctor.getUserId());
            if (notifications == null) {
                notifications = new ArrayList<>();
            }

            // Count unread notifications
            long unreadNotifications = notifications.stream()
                    .filter(notification -> !notification.isRead())
                    .count();

            Notification latestNotification = notifications.isEmpty() ? null : notifications.get(0);
            model.addAttribute("latestNotification", latestNotification);

            model.addAttribute("todayAppointments", todayAppointments);
            model.addAttribute("upcomingAppointments", upcomingAppointments);
            model.addAttribute("pendingOrders", pendingOrders);
            model.addAttribute("patientCount", patients.size());
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadNotifications", unreadNotifications);

            logger.debug("Dashboard loaded successfully for doctor ID: {}", doctorId);
            return "doctor/dashboard";
        } catch (Exception e) {
            logger.error("Error loading dashboard for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "An error occurred while loading the dashboard: " + e.getMessage());
            return "error";
        }
    }


    @GetMapping("/patients")
    public String getPatientsWithAppointments(
            @RequestParam(required = false) Integer doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String status,
            Model model,
            Authentication authentication) {
        try {
            // If doctorId is not provided, try to get it from the authentication
            if (doctorId == null && authentication != null) {
                logger.info("No doctorId provided, attempting to retrieve from authenticated user");
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    doctorId = userDetails.getDoctorId();
                    logger.info("Retrieved doctorId {} from authentication", doctorId);
                }
            }

            logger.info("Fetching paginated patients with appointments for doctor ID: {}, page: {}, size: {}",
                    doctorId, page, size);

            // Get paginated patients - either for specific doctor or all patients
            Page<Patient> patientsPage;
            if (doctorId != null) {
                // Apply status filter if provided
                if (status != null && !status.equals("all")) {
                    patientsPage = doctorService.findByDoctorAndStatus(doctorId, status, page, size);
                } else {
                    patientsPage = doctorService.findByDoctor(doctorId, page, size);
                }
            } else {
                if (status != null && !status.equals("all")) {
                    patientsPage = doctorService.findAllPatientsByStatus(status, page, size);
                } else {
                    patientsPage = doctorService.findAllPatients(page, size);
                }
            }

            // Process patients to enrich with derived data
            int activeCount = 0;
            int newCount = 0;
            int inactiveCount = 0;

            for (Patient patient : patientsPage.getContent()) {
                // Calculate age if date of birth is available
                if (patient.getDateOfBirth() != null) {
                    int age = java.time.Period.between(
                            patient.getDateOfBirth(),
                            java.time.LocalDate.now()
                    ).getYears();
                    patient.setAge(age);
                }

                // Set status based on user's status if available
                if (patient.getUser() != null && patient.getUser().getStatus() != null) {
                    patient.setStatus(patient.getUser().getStatus().toUpperCase());
                } else {
                    patient.setStatus("ACTIVE");
                }

                // Count by status (for the current page)
                switch(patient.getStatus()) {
                    case "ACTIVE":
                        activeCount++;
                        break;
                    case "NEW":
                        newCount++;
                        break;
                    case "INACTIVE":
                        inactiveCount++;
                        break;
                }

                // Set total appointments
                if (doctorId != null) {
                    Long totalAppointments = doctorService.getAppointmentRepository()
                            .countByPatientIdAndDoctorId(patient.getPatientId(), doctorId);
                    patient.setTotalAppointments(totalAppointments != null ? totalAppointments.intValue() : 0);

                    // Set upcoming appointments
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    Long upcomingAppointments = doctorService.getAppointmentRepository()
                            .countByPatientIdAndDoctorIdAndDateTimeAfter(patient.getPatientId(), doctorId, now);
                    patient.setUpcomingAppointments(upcomingAppointments != null ? upcomingAppointments.intValue() : 0);

                    // Calculate last visit
                    java.util.Optional<Appointment> lastAppointment = doctorService.getAppointmentRepository()
                            .findTopByPatientIdAndDoctorIdAndDateTimeBeforeOrderByDateTimeDesc(
                                    patient.getPatientId(), doctorId, now);
                    if (lastAppointment.isPresent()) {
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                                lastAppointment.get().getDateTime().toLocalDate(),
                                java.time.LocalDate.now());
                        patient.setLastVisit(daysBetween == 0 ? "Today" :
                                (daysBetween == 1 ? "Yesterday" :
                                        daysBetween + " days ago"));
                    } else {
                        patient.setLastVisit("Never");
                    }
                } else {
                    // Set total appointments across all doctors
                    Long totalAppointments = doctorService.getAppointmentRepository()
                            .countByPatientId(patient.getPatientId());
                    patient.setTotalAppointments(totalAppointments != null ? totalAppointments.intValue() : 0);

                    // Set upcoming appointments across all doctors
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    Long upcomingAppointments = doctorService.getAppointmentRepository()
                            .countByPatientIdAndDateTimeAfter(patient.getPatientId(), now);
                    patient.setUpcomingAppointments(upcomingAppointments != null ? upcomingAppointments.intValue() : 0);

                    // Calculate last visit across all doctors
                    java.util.Optional<Appointment> lastAppointment = doctorService.getAppointmentRepository()
                            .findTopByPatientIdAndDateTimeBeforeOrderByDateTimeDesc(
                                    patient.getPatientId(), now);
                    if (lastAppointment.isPresent()) {
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                                lastAppointment.get().getDateTime().toLocalDate(),
                                java.time.LocalDate.now());
                        patient.setLastVisit(daysBetween == 0 ? "Today" :
                                (daysBetween == 1 ? "Yesterday" :
                                        daysBetween + " days ago"));
                    } else {
                        patient.setLastVisit("Never");
                    }
                }
            }

            model.addAttribute("patients", patientsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", patientsPage.getTotalPages());
            model.addAttribute("pageSize", size);

            // Use in-memory counts for this page
            model.addAttribute("totalPatients", patientsPage.getTotalElements());
            model.addAttribute("activePatients", activeCount);
            model.addAttribute("newPatients", newCount);
            model.addAttribute("inactivePatients", inactiveCount);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("selectedStatus", status != null ? status : "all");
            // Get doctor name if doctorId is provided
            if (doctorId != null) {
                Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
                if (doctor != null && doctor.getUser() != null) {
                    model.addAttribute("doctorName", doctor.getUser().getFullName());
                } else {
                    model.addAttribute("doctorName", "Unknown Doctor");
                }
            } else {
                model.addAttribute("doctorName", "All Doctors");
            }

            // Add the page object for pagination
            model.addAttribute("page", patientsPage);

            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);

            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(doctor.getUserId());
            if (notifications == null) {
                notifications = new ArrayList<>();
            }

            // Count unread notifications
            long unreadNotifications = notifications.stream()
                    .filter(notification -> !notification.isRead())
                    .count();

            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadNotifications", unreadNotifications);

            logger.debug("Retrieved {} patients (page {} of {})",
                    patientsPage.getContent().size(), page + 1, patientsPage.getTotalPages());
            return "doctor/patients";
        } catch (Exception e) {
            logger.error("Error fetching paginated patients", e);
            model.addAttribute("errorMessage", "Failed to retrieve patients: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/patients/{patientId}")
    public String getPatientDetails(
            @PathVariable Integer patientId,
            @RequestParam(required = false) Integer doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "all") String filter,
            Model model,
            Authentication authentication) {
        try {
            logger.info("Fetching details for patient ID: {}", patientId);

            // If doctorId is not provided, try to get it from authentication
            if (doctorId == null && authentication != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    doctorId = userDetails.getDoctorId();
                    logger.info("Retrieved doctorId {} from authentication", doctorId);
                }
            }

            // Fetch patient details
            Optional<Patient> patientOpt = doctorService.getPatientDetails(patientId);

            if (patientOpt.isEmpty()) {
                logger.warn("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found.");
                return "error";
            }

            Patient patient = patientOpt.get();
            model.addAttribute("patient", patient);

            // Fetch medical record and medications
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.getMedicalRecordByPatientId(patientId);
            if (medicalRecordOpt.isPresent()) {
                MedicalRecord medicalRecord = medicalRecordOpt.get();
                model.addAttribute("medicalRecord", medicalRecord);
                model.addAttribute("medications", medicalRecord.getMedications());
            } else {
                model.addAttribute("medications", new ArrayList<RecordMedication>());
            }

            // Calculate age if date of birth is available
            if (patient.getDateOfBirth() != null) {
                int age = java.time.Period.between(
                        patient.getDateOfBirth(),
                        java.time.LocalDate.now()
                ).getYears();
                model.addAttribute("patientAge", age);
            }

            // Fetch patient contact information
            List<PatientContact> patientContacts = doctorService.getPatientContactRepository()
                    .findByPatientIdOrderByContactIdAsc(patientId);
            model.addAttribute("patientContacts", patientContacts);

            // Set status based on user's status if available
            if (patient.getUser() != null && patient.getUser().getStatus() != null) {
                patient.setStatus(patient.getUser().getStatus().toUpperCase());
            } else {
                patient.setStatus("ACTIVE");
            }

            // Get paginated appointment history with filtering
            Page<Appointment> appointmentsPage;
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            // Create two versions of pageable - one with sort and one without
            org.springframework.data.domain.Pageable pageableWithSort =
                    org.springframework.data.domain.PageRequest.of(page, size,
                            org.springframework.data.domain.Sort.by("dateTime").descending());

            org.springframework.data.domain.Pageable pageableNoSort =
                    org.springframework.data.domain.PageRequest.of(page, size);

            if (doctorId != null) {
                // Get appointments for this patient with the current doctor
                if ("upcoming".equals(filter)) {
                    // Use pageable without sort for methods that already have ordering in their name
                    // The method already orders by dateTime ASC (future dates first)
                    appointmentsPage = doctorService.getAppointmentRepository()
                            .findByPatientIdAndDoctorIdAndDateTimeAfterOrderByDateTimeAsc(
                                    patientId, doctorId, now, pageableNoSort);
                } else if ("completed".equals(filter)) {
                    appointmentsPage = doctorService.getAppointmentRepository()
                            .findByPatientIdAndDoctorIdAndStatusOrderByDateTimeDesc(
                                    patientId, doctorId, "Completed", pageableNoSort);
                } else if ("cancelled".equals(filter)) {
                    appointmentsPage = doctorService.getAppointmentRepository()
                            .findByPatientIdAndDoctorIdAndStatusInOrderByDateTimeDesc(
                                    patientId, doctorId,
                                    java.util.Arrays.asList("Cancel", "Cancelled"), pageableNoSort);
                } else {
                    // Use pageable with sort for methods that don't specify ordering
                    appointmentsPage = doctorService.getAppointmentRepository()
                            .findByPatientIdAndDoctorId(patientId, doctorId, pageableWithSort);
                }
                model.addAttribute("isCurrentDoctor", true);

                // Get the doctor information
                Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
                if (doctor != null && doctor.getUser() != null) {
                    model.addAttribute("doctorName", doctor.getUser().getFullName());
                }
            } else {
                // Get all appointments for this patient regardless of doctor
                if ("upcoming".equals(filter)) {
                    appointmentsPage = doctorService.getAppointmentRepository()
                            .findByPatientIdAndDateTimeAfterOrderByDateTimeAsc(
                                    patientId, now, pageableNoSort);
                } else if ("completed".equals(filter)) {
                    appointmentsPage = doctorService.getAppointmentRepository()
                            .findByPatientIdAndStatusOrderByDateTimeDesc(
                                    patientId, "Completed", pageableNoSort);
                } else if ("cancelled".equals(filter)) {
                    appointmentsPage = doctorService.getAppointmentRepository()
                            .findByPatientIdAndStatusInOrderByDateTimeDesc(
                                    patientId,
                                    java.util.Arrays.asList("Cancel", "Cancelled"), pageableNoSort);
                } else {
                    appointmentsPage = doctorService.getAppointmentRepository()
                            .findByPatientId(patientId, pageableWithSort);
                }
                model.addAttribute("isCurrentDoctor", false);
            }

            // Add appointment statistics
            long completedCount = doctorService.getAppointmentRepository()
                    .countByPatientIdAndStatus(patientId, "Completed");
            long upcomingCount = doctorService.getAppointmentRepository()
                    .countByPatientIdAndDateTimeAfter(patientId, now);
            long cancelledCount = doctorService.getAppointmentRepository()
                    .countByPatientIdAndStatusIn(patientId, java.util.Arrays.asList("Cancel", "Cancelled"));

            // Calculate total appointments count (regardless of filter)
            long totalCount;
            if (doctorId != null) {
                totalCount = doctorService.getAppointmentRepository()
                        .countByPatientIdAndDoctorId(patientId, doctorId);
            } else {
                totalCount = doctorService.getAppointmentRepository()
                        .countByPatientId(patientId);
            }

            model.addAttribute("appointments", appointmentsPage.getContent());
            model.addAttribute("page", appointmentsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", appointmentsPage.getTotalPages());
            model.addAttribute("filter", filter);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("upcomingCount", upcomingCount);
            model.addAttribute("cancelledCount", cancelledCount);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("doctorId", doctorId);

            logger.debug("Successfully retrieved patient details for patient ID: {}", patientId);
            return "doctor/patient-details";
        } catch (Exception e) {
            logger.error("Error fetching patient details for patient ID: {}", patientId, e);
            model.addAttribute("errorMessage", "Failed to retrieve patient details: " + e.getMessage());
            return "error";
        }
    }



    @GetMapping("/patients/search")
    public String searchPatients(
            @RequestParam String name,
            @RequestParam Integer doctorId,
            Model model) {
        try {
            logger.info("Searching patients with name containing: '{}' for doctor ID: {}", name, doctorId);
            List<Patient> patients = doctorService.searchPatientsByName(name);
            model.addAttribute("patients", patients);
            model.addAttribute("searchTerm", name);
            model.addAttribute("doctorId", doctorId);

            logger.debug("Found {} patients matching search term '{}' for doctor ID: {}",
                patients.size(), name, doctorId);
            return "doctor/patient-search-results";
        } catch (Exception e) {
            logger.error("Error searching for patients with name '{}' for doctor ID: {}", name, doctorId, e);
            model.addAttribute("errorMessage", "Failed to search for patients: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/medical-orders")
    public String getAllMedicalOrders(
            @RequestParam Integer doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size, // Fixed page size of 5
            Model model) {
        try {
            logger.info("Fetching medical orders for doctor ID: {} with filters - status: {}, type: {}, date: {}, page: {}",
                    doctorId, status, type, date, page);
            long startTime = System.currentTimeMillis();

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid doctor ID: " + doctorId));

            // Parse date if provided
            LocalDate parsedDate = null;
            if (date != null && !date.isEmpty()) {
                parsedDate = LocalDate.parse(date);
            }

            // Get outgoing orders with a single optimized query
            List<MedicalOrder> allOutgoingOrders = medicalOrderRepository.findByDoctorIdAndFiltersWithDetails(
                    doctorId,
                    (status != null && !status.isEmpty()) ? status : null,
                    (type != null && !type.isEmpty()) ? type : null,
                    parsedDate);

            // Get incoming orders with a single optimized query
            List<MedicalOrder> allIncomingOrders = new ArrayList<>();
            List<Department> headedDepartments = doctor.getDepartmentsLed();
            if (headedDepartments != null && !headedDepartments.isEmpty()) {
                List<Integer> departmentIds = headedDepartments.stream()
                        .map(Department::getDepartmentId)
                        .collect(Collectors.toList());

                allIncomingOrders = medicalOrderRepository.findByDepartmentIdsAndFiltersWithDetails(
                        departmentIds,
                        (status != null && !status.isEmpty()) ? status : null,
                        (type != null && !type.isEmpty()) ? type : null,
                        parsedDate);
            }

            // Create paginated lists for outgoing and incoming orders
            int outgoingTotal = allOutgoingOrders.size();
            int incomingTotal = allIncomingOrders.size();

            // Calculate pagination details
            int outgoingStart = page * size;
            int outgoingEnd = Math.min(outgoingStart + size, outgoingTotal);
            int incomingStart = page * size;
            int incomingEnd = Math.min(incomingStart + size, incomingTotal);

            // Create paged sublists
            List<MedicalOrder> outgoingOrders = (outgoingStart < outgoingTotal) ?
                allOutgoingOrders.subList(outgoingStart, outgoingEnd) : new ArrayList<>();
            List<MedicalOrder> incomingOrders = (incomingStart < incomingTotal) ?
                allIncomingOrders.subList(incomingStart, incomingEnd) : new ArrayList<>();

            // Calculate total pages for each type
            int outgoingTotalPages = (int) Math.ceil((double) outgoingTotal / size);
            int incomingTotalPages = (int) Math.ceil((double) incomingTotal / size);
            int maxTotalPages = Math.max(outgoingTotalPages, incomingTotalPages);

            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(doctor.getUserId());
            if (notifications == null) {
                notifications = new ArrayList<>();
            }

            // Count unread notifications
            long unreadNotifications = notifications.stream()
                    .filter(notification -> !notification.isRead())
                    .count();

            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadNotifications", unreadNotifications);

            model.addAttribute("doctorId", doctorId);
            model.addAttribute("doctorName", doctor.getUser().getFullName());
            model.addAttribute("outgoingOrders", outgoingOrders);
            model.addAttribute("incomingOrders", incomingOrders);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("selectedType", type);
            model.addAttribute("selectedDate", date);

            // Add pagination attributes
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", maxTotalPages);
            model.addAttribute("outgoingTotalItems", outgoingTotal);
            model.addAttribute("incomingTotalItems", incomingTotal);
            model.addAttribute("outgoingTotalPages", outgoingTotalPages);
            model.addAttribute("incomingTotalPages", incomingTotalPages);

            long totalTime = System.currentTimeMillis() - startTime;
            logger.debug("Medical orders loaded in {} ms", totalTime);

            return "doctor/medical-orders";
        } catch (Exception e) {
            logger.error("Error fetching medical orders for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve medical orders: " + e.getMessage());
            return "error";
        }
    }

    // Helper method to apply filters to a list of medical orders
    private List<MedicalOrder> applyFilters(List<MedicalOrder> orders, String status, String type, String date) {
        List<MedicalOrder> filteredOrders = new ArrayList<>(orders);

        // Apply status filter
        if (status != null && !status.isEmpty()) {
            filteredOrders.removeIf(order -> !order.getStatus().equalsIgnoreCase(status));
        }

        // Apply type filter
        if (type != null && !type.isEmpty()) {
            filteredOrders.removeIf(order -> !order.getOrderType().equalsIgnoreCase(type));
        }

        // Apply date filter
        if (date != null && !date.isEmpty()) {
            LocalDate filterDate = LocalDate.parse(date);
            filteredOrders.removeIf(order -> {
                if (order.getOrderDate() == null) return true;

                // Convert java.sql.Date or java.util.Date to LocalDate safely
                LocalDate orderDate;
                if (order.getOrderDate() instanceof java.sql.Date) {
                    orderDate = ((java.sql.Date) order.getOrderDate()).toLocalDate();
                } else {
                    orderDate = order.getOrderDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                }

                return !orderDate.equals(filterDate);
            });
        }

        return filteredOrders;
    }

    @GetMapping("/medical-orders/status")
    public String getMedicalOrdersByStatus(
            @RequestParam Integer doctorId,
            @RequestParam String status,
            Model model) {
        try {
            logger.info("Fetching medical orders for doctor ID: {} with status: {}", doctorId, status);
            List<MedicalOrder> orders = medicalOrderRepository.findByDoctorIdAndStatus(doctorId, status);
            model.addAttribute("medicalOrders", orders);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("title", status + " Medical Orders");


            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid doctor ID: " + doctorId));

            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(doctor.getUserId());
            if (notifications == null) {
                notifications = new ArrayList<>();
            }

            // Count unread notifications
            long unreadNotifications = notifications.stream()
                    .filter(notification -> !notification.isRead())
                    .count();

            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadNotifications", unreadNotifications);


            logger.debug("Retrieved {} medical orders for doctor ID: {} with status: {}", orders.size(), doctorId, status);
            return "doctor/medical-orders";
        } catch (Exception e) {
            logger.error("Error fetching medical orders for doctor ID: {} with status: {}", doctorId, status, e);
            model.addAttribute("errorMessage", "Failed to retrieve " + status + " medical orders: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Display the notifications page for a doctor
     * @param doctorId The ID of the doctor
     * @param model The Spring model
     * @param authentication The current authentication object
     * @return The notification page template
     */
    @GetMapping("/notifications")
    public String getNotifications(@RequestParam(required = false) Integer doctorId,
                                 Model model,
                                 Authentication authentication) {
        try {
            // If doctorId is not provided, try to get it from the authentication
            if (doctorId == null && authentication != null) {
                logger.info("No doctorId provided, attempting to retrieve from authenticated user");
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    doctorId = userDetails.getDoctorId();
                    logger.info("Retrieved doctorId {} from authentication", doctorId);
                }
            }

            if (doctorId == null) {
                logger.error("No doctorId provided and could not be determined from authentication");
                model.addAttribute("errorMessage", "Doctor ID is required");
                return "error";
            }

            logger.info("Loading notifications for doctor ID: {}", doctorId);
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);

            // Check if doctor is null before accessing its properties
            if (doctor == null) {
                logger.error("Doctor not found for ID: {}", doctorId);
                model.addAttribute("errorMessage", "Doctor not found");
                return "error";
            }
            logger.info("Doctor Identified: {}", doctor.getUser().getUserId());
            // Fetch notifications for the doctor
            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(doctor.getUser().getUserId());
            if (notifications == null) {
                notifications = new ArrayList<>();
            }

            // Count notifications by type
            long unreadCount = notifications.stream()
                    .filter(notification -> !notification.isRead())
                    .count();

            long appointmentCount = notifications.stream()
                    .filter(notification -> notification.getType() != null &&
                           notification.getType().toString().toLowerCase().contains("appointment"))
                    .count();

            model.addAttribute("doctorName", doctor.getUser().getFullName());
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadNotifications", unreadCount);
            model.addAttribute("allCount", notifications.size());
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("appointmentCount", appointmentCount);

            logger.debug("Notifications page loaded successfully for doctor ID: {}", doctorId);
            return "doctor/doctor-notification";
        } catch (Exception e) {
            logger.error("Error loading notifications for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "An error occurred while loading notifications: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Mark a notification as read and redirect back to notifications page
     * @param notificationId The ID of the notification to mark as read
     * @param doctorId The ID of the doctor making the request
     * @param redirectAttributes RedirectAttributes for flash messages
     * @return Redirect to the notifications page
     */
    @PostMapping("/notifications/mark-read")
    public String markNotificationAsRead(
            @RequestParam Integer notificationId,
            @RequestParam Integer doctorId,
            RedirectAttributes redirectAttributes) {
        try {
            logger.info("Marking notification ID: {} as read for doctor ID: {}", notificationId, doctorId);

            Notification notification = notificationRepository.findById(notificationId).orElse(null);
            if (notification != null) {
                notification.setRead(true);
                notificationRepository.save(notification);
                redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read");
                logger.debug("Successfully marked notification ID: {} as read", notificationId);
            } else {
                logger.warn("Notification with ID: {} not found", notificationId);
                redirectAttributes.addFlashAttribute("errorMessage", "Notification not found");
            }

            return "redirect:/doctor/notifications?doctorId=" + doctorId;
        } catch (Exception e) {
            logger.error("Error marking notification ID: {} as read for doctor ID: {}",
                    notificationId, doctorId, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to mark notification as read: " + e.getMessage());
            return "redirect:/doctor/notifications?doctorId=" + doctorId;
        }
    }
}
