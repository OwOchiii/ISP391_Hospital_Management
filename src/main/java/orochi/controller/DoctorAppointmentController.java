package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.*;
import orochi.repository.*;
import orochi.service.DoctorService;
import orochi.service.FileStorageService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/doctor/appointments")
public class DoctorAppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorAppointmentController.class);

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalResultRepository medicalResultRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private MedicineInventoryRepository medicineInventoryRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("")
    public String getAllAppointments(
            @RequestParam Integer doctorId,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            Model model) {
        try {
            logger.info("Fetching appointments for doctor ID: {} with filters: filter={}, search={}, status={}, dateFrom={}",
                    doctorId, filter, search, status, dateFrom);

            // Set doctor name
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
            if (doctor != null && doctor.getUser() != null) {
                model.addAttribute("doctorName", doctor.getUser().getFullName());
            }

            // Get appointments based on filters
            List<Appointment> appointments;
            String filterTitle = "All Appointments";

            // First, apply the main filter category
            if ("today".equals(filter)) {
                appointments = doctorService.getTodayAppointments(doctorId);
                filterTitle = "Today's Appointments";
            } else if ("upcoming".equals(filter)) {
                appointments = doctorService.getUpcomingAppointments(doctorId);
                filterTitle = "Upcoming Appointments";
            } else {
                appointments = doctorService.getAppointments(doctorId);
            }

            // Then apply additional filters one by one

            // Search by patient name
            if (search != null && !search.trim().isEmpty()) {
                appointments = doctorService.searchAppointmentsByPatientName(doctorId, search.trim());
                filterTitle = "Search Results: " + search;
            }

            // Filter by status
            if (status != null && !status.isEmpty()) {
                // If we already filtered by search, apply status filter to the results
                if (search != null && !search.trim().isEmpty()) {
                    final String statusFilter = status;
                    appointments = appointments.stream()
                            .filter(a -> statusFilter.equals(a.getStatus()))
                            .toList();
                } else {
                    // Otherwise get appointments by status directly
                    appointments = doctorService.getAppointmentsByStatus(doctorId, status);
                }

                // Update filter title
                if (search != null && !search.trim().isEmpty()) {
                    filterTitle += " - Status: " + status;
                } else {
                    filterTitle = status + " Appointments";
                }
            }

            // Filter by date
            if (dateFrom != null) {
                final LocalDate filterDate = dateFrom;
                appointments = appointments.stream()
                        .filter(a -> a.getDateTime() != null &&
                                a.getDateTime().toLocalDate().isEqual(filterDate))
                        .toList();

                // Update filter title
                if ((search != null && !search.trim().isEmpty()) ||
                        (status != null && !status.isEmpty())) {
                    filterTitle += " - Date: " + dateFrom;
                } else {
                    filterTitle = "Appointments on " + dateFrom;
                }
            }

            // Set all required template attributes
            model.addAttribute("appointments", appointments);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("title", filterTitle);
            model.addAttribute("currentFilter", filter != null ? filter : "all");
            model.addAttribute("currentFilterTitle", filterTitle);
            model.addAttribute("searchTerm", search);
            model.addAttribute("status", status);
            model.addAttribute("dateFrom", dateFrom);

            // Count metrics
            model.addAttribute("allCount", doctorService.getAppointments(doctorId).size());
            model.addAttribute("todayCount", doctorService.getTodayAppointments(doctorId).size());
            model.addAttribute("upcomingCount", doctorService.getUpcomingAppointments(doctorId).size());

            // Pagination (simplified)
            model.addAttribute("totalPages", 1);
            model.addAttribute("currentPage", page);

            // Fetch notifications for the doctor
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
            logger.debug("Retrieved {} appointments for doctor ID: {}", appointments.size(), doctorId);
            return "doctor/appointments";
        } catch (Exception e) {
            logger.error("Error fetching appointments for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve appointments: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/update-status")
    @ResponseBody
    public ResponseEntity<?> updateAppointmentStatus(
            @RequestParam Integer appointmentId,
            @RequestParam Integer doctorId,
            @RequestParam String status) {
        try {
            logger.info("Updating status for appointment ID: {} to {} by doctor ID: {}",
                    appointmentId, status, doctorId);

            // Validate status value against the EXACT allowed values in the database constraint
            Set<String> allowedStatuses = Set.of("Scheduled", "Completed", "Cancel","Pending");
            if (!allowedStatuses.contains(status)) {
                logger.warn("Invalid status value: {} for appointment ID: {}", status, appointmentId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false,
                                "message", "Invalid status value. Allowed values are: " +
                                        String.join(", ", allowedStatuses)));
            }

            Optional<Appointment> appointmentOpt = doctorService.getAppointmentDetails(appointmentId, doctorId);

            if (appointmentOpt.isEmpty()) {
                logger.warn("Appointment not found or access denied for appointment ID: {} and doctor ID: {}",
                        appointmentId, doctorId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Appointment not found or access denied"));
            }

            Appointment appointment = appointmentOpt.get();

            try {
                // Update appointment status with the new status parameter
                appointment.setStatus(status);
                doctorService.getAppointmentRepository().save(appointment);

                logger.info("Successfully updated appointment ID: {} status to: {}", appointmentId, status);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Appointment status updated successfully",
                        "newStatus", status));
            } catch (IllegalArgumentException e) {
                logger.error("Invalid status value: {} for appointment ID: {}", status, appointmentId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false,
                                "message", "Invalid status value. Allowed values are: " +
                                        String.join(", ", allowedStatuses)));
            }
        } catch (Exception e) {
            logger.error("Error updating status for appointment ID: {} to {} by doctor ID: {}",
                    appointmentId, status, doctorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to update status: " + e.getMessage()));
        }
    }

    @GetMapping("/today")
    public String getTodayAppointments(
            @RequestParam("doctorId") Integer doctorId,
            Model model,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        try {
            logger.info("Fetching today's appointments for doctor ID: {}", doctorId);

            // Get doctor name
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
            if (doctor != null && doctor.getUser() != null) {
                model.addAttribute("doctorName", doctor.getUser().getFullName());
            }

            // Get today's appointments
            List<Appointment> appointments = doctorService.getTodayAppointments(doctorId);

            // Set model attributes
            model.addAttribute("appointments", appointments);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("title", "Today's Appointments");
            model.addAttribute("currentFilter", "today");
            model.addAttribute("currentFilterTitle", "Today's Appointments");

            // Count metrics - important to keep these
            model.addAttribute("allCount", doctorService.getAppointments(doctorId).size());
            model.addAttribute("todayCount", appointments.size());
            model.addAttribute("upcomingCount", doctorService.getUpcomingAppointments(doctorId).size());

            // Pagination
            model.addAttribute("totalPages", 1);
            model.addAttribute("currentPage", page);

            logger.debug("Retrieved {} today's appointments for doctor ID: {}", appointments.size(), doctorId);
            return "doctor/appointments";
        } catch (Exception e) {
            logger.error("Error fetching today's appointments for doctor ID: {}", doctorId, e);
            return "error";
        }
    }

    @GetMapping("/upcoming")
    public String getUpcomingAppointments(
            @RequestParam("doctorId") Integer doctorId,
            Model model,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        try {
            logger.info("Fetching upcoming appointments for doctor ID: {}", doctorId);

            // Get doctor name
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
            if (doctor != null && doctor.getUser() != null) {
                model.addAttribute("doctorName", doctor.getUser().getFullName());
            }

            // Get upcoming appointments
            List<Appointment> appointments = doctorService.getUpcomingAppointments(doctorId);

            // Set model attributes
            model.addAttribute("appointments", appointments);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("title", "Upcoming Appointments");
            model.addAttribute("currentFilter", "upcoming");
            model.addAttribute("currentFilterTitle", "Upcoming Appointments");

            // Count metrics - important to keep these
            model.addAttribute("allCount", doctorService.getAppointments(doctorId).size());
            model.addAttribute("todayCount", doctorService.getTodayAppointments(doctorId).size());
            model.addAttribute("upcomingCount", appointments.size());

            // Pagination
            model.addAttribute("totalPages", 1);
            model.addAttribute("currentPage", page);

            logger.debug("Retrieved {} upcoming appointments for doctor ID: {}", appointments.size(), doctorId);
            return "doctor/appointments";
        } catch (Exception e) {
            logger.error("Error fetching upcoming appointments for doctor ID: {}", doctorId, e);
            return "error";
        }
    }

    @GetMapping("/date")
    public String getAppointmentsByDate(
            @RequestParam Integer doctorId,
            @RequestParam LocalDate date,
            Model model) {
        try {
            logger.info("Fetching appointments for doctor ID: {} on date: {}", doctorId, date);
            List<Appointment> appointments = doctorService.getAppointmentsByDate(doctorId, date);
            model.addAttribute("appointments", appointments);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("selectedDate", date);
            model.addAttribute("title", "Appointments on " + date);
            logger.debug("Retrieved {} appointments for doctor ID: {} on date: {}", appointments.size(), doctorId, date);
            return "doctor/appointments";
        } catch (Exception e) {
            logger.error("Error fetching appointments for doctor ID: {} on date: {}", doctorId, date, e);
            model.addAttribute("errorMessage", "Failed to retrieve appointments for the selected date: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/status")
    public String getAppointmentsByStatus(
            @RequestParam Integer doctorId,
            @RequestParam String status,
            Model model) {
        try {
            logger.info("Fetching appointments for doctor ID: {} with status: {}", doctorId, status);
            List<Appointment> appointments = doctorService.getAppointmentsByStatus(doctorId, status);
            model.addAttribute("appointments", appointments);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("title", status + " Appointments");
            logger.debug("Retrieved {} appointments for doctor ID: {} with status: {}", appointments.size(), doctorId, status);
            return "doctor/appointments";
        } catch (Exception e) {
            logger.error("Error fetching appointments for doctor ID: {} with status: {}", doctorId, status, e);
            model.addAttribute("errorMessage", "Failed to retrieve " + status + " appointments: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/{appointmentId}")
    public String getAppointmentDetails(
            @PathVariable Integer appointmentId,
            @RequestParam Integer doctorId,
            @RequestParam(defaultValue = "0") Integer orderPage,
            @RequestParam(defaultValue = "5") Integer orderSize,
            @RequestParam(defaultValue = "0") Integer resultPage,
            @RequestParam(defaultValue = "5") Integer resultSize,
            Model model) {
        try {
            logger.info("Fetching details for appointment ID: {} for doctor ID: {}", appointmentId, doctorId);

            // First, try the standard permission check through doctorService
            Optional<Appointment> appointment = doctorService.getAppointmentDetails(appointmentId, doctorId);

            // If that fails, check if the doctor has access through medical orders
            if (appointment.isEmpty()) {
                logger.info("Doctor {} is not directly assigned to appointment {}. Checking medical order access...",
                        doctorId, appointmentId);

                // Check if doctor has any medical orders for this appointment
                boolean hasMedicalOrderAccess = medicalOrderRepository.existsByAppointmentIdAndDoctorId(appointmentId, doctorId);

                if (hasMedicalOrderAccess) {
                    logger.info("Doctor {} has access to appointment {} through medical orders", doctorId, appointmentId);
                    // Directly fetch the appointment since we've verified access through medical orders
                    appointment = appointmentRepository.findById(appointmentId);
                }
            }

            if (appointment.isPresent()) {
                Optional<Patient> patient = doctorService.getPatientDetails(appointment.get().getPatientId());

                // Fetch the doctor information for this appointment
                Optional<Doctor> doctor = doctorRepository.findById(appointment.get().getDoctorId());
                model.addAttribute("doctor", doctor.orElse(null));

                // Get patient's prescription history for this appointment
                List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointmentId);
                model.addAttribute("prescriptions", prescriptions);

                // Get available medicines for the dropdown
                List<MedicineInventory> medicineInventory = medicineInventoryRepository.findByCurrentStockGreaterThan(0);
                model.addAttribute("medicineInventory", medicineInventory);

                if (patient.isPresent()) {
                    List<Appointment> patientAppointmentHistory = appointmentRepository.findByPatientIdAndDateTimeBefore(
                            appointment.get().getPatientId(),
                            appointment.get().getDateTime(),
                            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dateTime"))
                    ).getContent();

                    model.addAttribute("patientAppointmentHistory", patientAppointmentHistory);
                }
                // Create Pageable objects for pagination
                Pageable orderPageable = PageRequest.of(orderPage, orderSize, Sort.by(Sort.Direction.DESC, "orderDate"));
                Pageable resultPageable = PageRequest.of(resultPage, resultSize, Sort.by(Sort.Direction.DESC, "resultDate"));

                // Get paginated medical orders and results
                Page<MedicalOrder> medicalOrdersPage = medicalOrderRepository.findByAppointmentId(appointmentId, orderPageable);
                Page<MedicalResult> resultsPage = medicalResultRepository.findByAppointmentId(appointmentId, resultPageable);

                model.addAttribute("appointment", appointment.get());
                model.addAttribute("patient", patient.orElse(null));
                model.addAttribute("medicalOrders", medicalOrdersPage.getContent());
                model.addAttribute("doctorId", doctorId);
                model.addAttribute("results", resultsPage.getContent());

                // Add pagination information to the model
                model.addAttribute("orderCurrentPage", orderPage);
                model.addAttribute("orderTotalPages", medicalOrdersPage.getTotalPages());
                model.addAttribute("orderTotalItems", medicalOrdersPage.getTotalElements());
                model.addAttribute("orderSize", orderSize);

                model.addAttribute("resultCurrentPage", resultPage);
                model.addAttribute("resultTotalPages", resultsPage.getTotalPages());
                model.addAttribute("resultTotalItems", resultsPage.getTotalElements());
                model.addAttribute("resultSize", resultSize);

                // Get departments for the dropdown
                List<Department> departments = departmentRepository.findAll();
                model.addAttribute("departments", departments);

                logger.debug("Successfully retrieved appointment details for appointment ID: {}", appointmentId);
                return "doctor/appointment-details";
            } else {
                logger.warn("Appointment not found or access denied for appointment ID: {} and doctor ID: {}", appointmentId, doctorId);
                model.addAttribute("errorMessage", "Appointment not found or access denied.");
                return "error";
            }
        } catch (Exception e) {
            logger.error("Error fetching appointment details for appointment ID: {} and doctor ID: {}", appointmentId, doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve appointment details: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/{appointmentId}/orders/create")
    public String createMedicalOrder(
            @PathVariable Integer appointmentId,
            @RequestParam Integer doctorId,
            @RequestParam String orderType,
            @RequestParam String description,
            @RequestParam String status,
            @RequestParam(required = false) Integer assignedToDeptId,
            RedirectAttributes redirectAttributes) {

        try {
            logger.info("Creating medical order for appointment ID: {} by doctor ID: {}", appointmentId, doctorId);

            // Verify the appointment exists and belongs to this doctor
            Optional<Appointment> appointmentOpt = doctorService.getAppointmentDetails(appointmentId, doctorId);
            if (appointmentOpt.isEmpty()) {
                logger.warn("Attempt to create order for non-existent or unauthorized appointment ID: {} by doctor ID: {}",
                        appointmentId, doctorId);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Appointment not found or you don't have permission to create orders for it.");
                return "redirect:/doctor/appointments";
            }

            // Create the medical order
            MedicalOrder medicalOrder = new MedicalOrder();
            medicalOrder.setAppointmentId(appointmentId);
            medicalOrder.setDoctorId(doctorId);
            medicalOrder.setOrderType(orderType);
            medicalOrder.setDescription(description);
            medicalOrder.setStatus(status);
            medicalOrder.setAssignedToDeptId(assignedToDeptId);
            medicalOrder.setOrderDate(new java.sql.Date(System.currentTimeMillis()));

            // Save the order
            medicalOrderRepository.save(medicalOrder);
            logger.info("Successfully created medical order ID: {} for appointment ID: {}",
                    medicalOrder.getOrderId(), appointmentId);

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Medical order created successfully.");

            // Redirect back to the appointment details
            return "redirect:/doctor/appointments/" + appointmentId + "?doctorId=" + doctorId;

        } catch (Exception e) {
            logger.error("Error creating medical order for appointment ID: {} by doctor ID: {}",
                    appointmentId, doctorId, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to create medical order: " + e.getMessage());
            return "redirect:/doctor/appointments/" + appointmentId + "?doctorId=" + doctorId;
        }
    }

    @PostMapping("/{appointmentId}/orders/{orderId}/update-status")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Integer appointmentId,
            @PathVariable Integer orderId,
            @RequestParam Integer doctorId,
            @RequestParam String status) {

        try {
            logger.info("Updating status of medical order ID: {} to {} by doctor ID: {}",
                    orderId, status, doctorId);

            Optional<MedicalOrder> orderOpt = medicalOrderRepository.findById(orderId);

            if (orderOpt.isPresent()) {
                MedicalOrder order = orderOpt.get();

                // Check if this doctor has permission to update this order
                if (!order.getDoctorId().equals(doctorId)) {
                    logger.warn("Doctor ID: {} attempted to update order ID: {} belonging to another doctor",
                            doctorId, orderId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("success", false,
                                    "message", "You don't have permission to update this medical order."));
                }

                // Update the status
                order.setStatus(status);
                medicalOrderRepository.save(order);

                logger.info("Successfully updated status of medical order ID: {} to {}", orderId, status);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Order status updated successfully",
                        "newStatus", status));

            } else {
                logger.warn("Medical order not found for ID: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false,
                                "message", "Medical order not found"));
            }

        } catch (Exception e) {
            logger.error("Error updating status of medical order ID: {} to {} by doctor ID: {}",
                    orderId, status, doctorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false,
                            "message", "Failed to update order status: " + e.getMessage()));
        }
    }

    @PostMapping("/{appointmentId}/medical-result/submit")
    public String submitMedicalResult(
            @PathVariable Integer appointmentId,
            @RequestParam Integer doctorId,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile resultFile,
            @RequestParam String status,
            @RequestParam(required = false) Integer orderId,
            RedirectAttributes redirectAttributes) {

        try {
            logger.info("Submitting medical result for appointment ID: {} by doctor ID: {}", appointmentId, doctorId);

            // Verify the appointment exists and belongs to this doctor
            Optional<Appointment> appointmentOpt = doctorService.getAppointmentDetails(appointmentId, doctorId);
            if (appointmentOpt.isEmpty()) {
                logger.warn("Attempt to submit result for non-existent or unauthorized appointment ID: {} by doctor ID: {}",
                        appointmentId, doctorId);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Appointment not found or you don't have permission to submit results for it.");
                return "redirect:/doctor/appointments";
            }

            // If an order ID is provided, check if it already has a result
            if (orderId != null) {
                Optional<MedicalOrder> orderOpt = medicalOrderRepository.findById(orderId);
                if (orderOpt.isPresent()) {
                    MedicalOrder order = orderOpt.get();
                    if (order.getResultId() != null) {
                        logger.warn("Order ID: {} already has a result. Only one result per order is allowed.", orderId);
                        redirectAttributes.addFlashAttribute("errorMessage",
                                "This order already has a result. Only one result is allowed per order.");
                        return "redirect:/doctor/appointments/" + appointmentId + "?doctorId=" + doctorId;
                    }
                } else {
                    logger.warn("Medical order not found for ID: {}", orderId);
                    redirectAttributes.addFlashAttribute("errorMessage", "Medical order not found.");
                    return "redirect:/doctor/appointments/" + appointmentId + "?doctorId=" + doctorId;
                }
            }

            // Create the medical result
            MedicalResult medicalResult = new MedicalResult();
            medicalResult.setAppointmentId(appointmentId);
            medicalResult.setDoctorId(doctorId);
            medicalResult.setDescription(description);
            medicalResult.setStatus(status);
            medicalResult.setResultDate(LocalDateTime.now());

            // Handle file upload if provided
            if (resultFile != null && !resultFile.isEmpty()) {
                try {
                    String fileUrl = fileStorageService.storeFile(resultFile, "medical-results");
                    medicalResult.setFileUrl(fileUrl);
                    logger.info("File uploaded successfully for appointment ID: {}, file URL: {}",
                            appointmentId, fileUrl);
                } catch (IOException e) {
                    logger.error("Failed to upload file for appointment ID: {}", appointmentId, e);
                    redirectAttributes.addFlashAttribute("warningMessage",
                            "Medical result was saved but file upload failed: " + e.getMessage());
                    // Continue with saving the result even if file upload fails
                }
            }

            // Save the result
            MedicalResult savedResult = medicalResultRepository.save(medicalResult);
            logger.info("Successfully created medical result ID: {} for appointment ID: {}",
                    savedResult.getResultId(), appointmentId);

            // If order ID is provided, update the order to reference this result
            if (orderId != null) {
                Optional<MedicalOrder> orderOpt = medicalOrderRepository.findById(orderId);
                if (orderOpt.isPresent()) {
                    MedicalOrder order = orderOpt.get();
                    order.setResultId(savedResult.getResultId());
                    medicalOrderRepository.save(order);
                    logger.info("Updated order ID: {} to reference result ID: {}", orderId, savedResult.getResultId());
                }
            }

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Medical result submitted successfully.");

            // Redirect back to the appointment details
            return "redirect:/doctor/appointments/" + appointmentId + "?doctorId=" + doctorId;

        } catch (Exception e) {
            logger.error("Error submitting medical result for appointment ID: {} by doctor ID: {}",
                    appointmentId, doctorId, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to submit medical result: " + e.getMessage());
            return "redirect:/doctor/appointments/" + appointmentId + "?doctorId=" + doctorId;
        }
    }
}
