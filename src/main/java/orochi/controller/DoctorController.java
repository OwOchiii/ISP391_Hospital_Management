package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import orochi.model.Appointment;
import orochi.model.Doctor;
import orochi.model.MedicalOrder;
import orochi.model.Patient;
import orochi.repository.DoctorRepository;
import orochi.repository.MedicalOrderRepository;
import orochi.service.DoctorService;
import orochi.config.CustomUserDetails;

import java.time.LocalDate;
import java.util.*;

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
// Make sure to initialize empty lists rather than null
            model.addAttribute("doctorName", doctor.getUser().getFullName());
            List<Appointment> todayAppointments = doctorService.getTodayAppointments(doctorId);
            if (todayAppointments == null) {
                todayAppointments = new ArrayList<>();
            }

            List<Appointment> upcomingAppointments = doctorService.getUpcomingAppointments(doctorId);
            if (upcomingAppointments == null) {
                upcomingAppointments = new ArrayList<>();
            }

            List<MedicalOrder> pendingOrders = medicalOrderRepository.findByOrderByIdAndStatus(doctorId, "Pending");
            if (pendingOrders == null) {
                pendingOrders = new ArrayList<>();
            }

            List<Patient> patients = doctorService.getPatientsWithAppointments(doctorId);
            if (patients == null) {
                patients = new ArrayList<>();
            }

            model.addAttribute("todayAppointments", todayAppointments);
            model.addAttribute("upcomingAppointments", upcomingAppointments);
            model.addAttribute("pendingOrders", pendingOrders);
            model.addAttribute("patientCount", patients.size());
            model.addAttribute("doctorId", doctorId);

            logger.debug("Dashboard loaded successfully for doctor ID: {}", doctorId);
            return "doctor/dashboard";
        } catch (Exception e) {
            logger.error("Error loading dashboard for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "An error occurred while loading the dashboard: " + e.getMessage());
            return "error";
        }
    }


    @GetMapping("/patients")
    public String getPatientsWithAppointments(@RequestParam Integer doctorId, Model model) {
        try {
            logger.info("Fetching patients with appointments for doctor ID: {}", doctorId);
            List<Patient> patients = doctorService.getPatientsWithAppointments(doctorId);
            model.addAttribute("patients", patients);
            model.addAttribute("doctorId", doctorId);

            logger.debug("Retrieved {} patients with appointments for doctor ID: {}", patients.size(), doctorId);
            return "doctor/patients";
        } catch (Exception e) {
            logger.error("Error fetching patients with appointments for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve patients: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/patient/{patientId}")
    public String getPatientDetails(
            @PathVariable Integer patientId,
            @RequestParam Integer doctorId,
            Model model) {
        try {
            logger.info("Fetching details for patient ID: {} for doctor ID: {}", patientId, doctorId);
            Optional<Patient> patient = doctorService.getPatientDetails(patientId);

            if (patient.isPresent()) {
                List<Appointment> appointmentHistory = doctorService.getAppointmentRepository()
                    .findByPatientIdAndDoctorIdOrderByDateTimeDesc(patientId, doctorId);

                model.addAttribute("patient", patient.get());
                model.addAttribute("appointmentHistory", appointmentHistory);
                model.addAttribute("doctorId", doctorId);

                logger.debug("Successfully retrieved patient details for patient ID: {} with {} appointments",
                    patientId, appointmentHistory.size());
                return "doctor/patient-details";
            } else {
                logger.warn("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found.");
                return "error";
            }
        } catch (Exception e) {
            logger.error("Error fetching patient details for patient ID: {} and doctor ID: {}", patientId, doctorId, e);
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
    public String getDoctorMedicalOrders(@RequestParam Integer doctorId, Model model) {
        try {
            logger.info("Fetching all medical orders for doctor ID: {}", doctorId);
            List<MedicalOrder> orders = medicalOrderRepository.findByOrderById(doctorId);
            model.addAttribute("medicalOrders", orders);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("title", "All Medical Orders");

            logger.debug("Retrieved {} medical orders for doctor ID: {}", orders.size(), doctorId);
            return "doctor/medical-orders";
        } catch (Exception e) {
            logger.error("Error fetching medical orders for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve medical orders: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/medical-orders/status")
    public String getMedicalOrdersByStatus(
            @RequestParam Integer doctorId,
            @RequestParam String status,
            Model model) {
        try {
            logger.info("Fetching medical orders for doctor ID: {} with status: {}", doctorId, status);
            List<MedicalOrder> orders = medicalOrderRepository.findByOrderByIdAndStatus(doctorId, status);
            model.addAttribute("medicalOrders", orders);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("title", status + " Medical Orders");

            logger.debug("Retrieved {} medical orders for doctor ID: {} with status: {}", orders.size(), doctorId, status);
            return "doctor/medical-orders";
        } catch (Exception e) {
            logger.error("Error fetching medical orders for doctor ID: {} with status: {}", doctorId, status, e);
            model.addAttribute("errorMessage", "Failed to retrieve " + status + " medical orders: " + e.getMessage());
            return "error";
        }
    }
}
