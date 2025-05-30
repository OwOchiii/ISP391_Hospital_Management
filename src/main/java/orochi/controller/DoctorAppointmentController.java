package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.Appointment;
import orochi.model.Doctor;
import orochi.model.MedicalOrder;
import orochi.model.Patient;
import orochi.repository.DoctorRepository;
import orochi.repository.MedicalOrderRepository;
import orochi.service.DoctorService;

import java.time.LocalDate;
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
            appointment.setStatus(status);
            doctorService.getAppointmentRepository().save(appointment);

            logger.info("Successfully updated appointment ID: {} status to: {}", appointmentId, status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Appointment status updated successfully",
                    "newStatus", status));
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
            Model model) {
        try {
            logger.info("Fetching details for appointment ID: {} for doctor ID: {}", appointmentId, doctorId);
            Optional<Appointment> appointment = doctorService.getAppointmentDetails(appointmentId, doctorId);

            if (appointment.isPresent()) {
                Optional<Patient> patient = doctorService.getPatientDetails(appointment.get().getPatientId());
                List<MedicalOrder> medicalOrders = medicalOrderRepository.findByAppointmentIdOrderByOrderDate(appointmentId);

                model.addAttribute("appointment", appointment.get());
                model.addAttribute("patient", patient.orElse(null));
                model.addAttribute("medicalOrders", medicalOrders);
                model.addAttribute("doctorId", doctorId);

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

}