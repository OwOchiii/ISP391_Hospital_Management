package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import orochi.model.Appointment;
import orochi.model.MedicalOrder;
import orochi.model.Patient;
import orochi.model.Prescription;
import orochi.repository.AppointmentRepository;
import orochi.repository.MedicalOrderRepository;
import orochi.repository.PatientRepository;
import orochi.config.CustomUserDetails;
import orochi.service.PatientService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/patient")
public class PatientDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(PatientDashboardController.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @Autowired
    private PatientService patientService;

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
            }
            return "patient/search-doctor";
        } catch (Exception e) {
            logger.error("Error loading search doctor page", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/book-appointment")
    public String bookAppointment(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId != null) {
                model.addAttribute("patientId", patientId);
            }
            return "patient/book-appointment";
        } catch (Exception e) {
            logger.error("Error loading book appointment page", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/appointment-list")
    public String appointmentList(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required");
                return "error";
            }

            List<Appointment> appointments = appointmentRepository.findByPatientIdOrderByDateTimeDesc(patientId);
            model.addAttribute("appointments", appointments);
            model.addAttribute("patientId", patientId);

            return "patient/appointment-list";
        } catch (Exception e) {
            logger.error("Error loading appointment list", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
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

            Optional<Patient> patient = patientRepository.findById(patientId);
            if (patient.isPresent()) {
                model.addAttribute("patient", patient.get());
            }

            return "patient/profile";
        } catch (Exception e) {
            logger.error("Error loading profile", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/customer-support")
    public String customerSupport(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId != null) {
                model.addAttribute("patientId", patientId);
            }
            return "patient/customer-support";
        } catch (Exception e) {
            logger.error("Error loading customer support page", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/feedback")
    public String feedback(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId != null) {
                model.addAttribute("patientId", patientId);
            }
            return "patient/feedback";
        } catch (Exception e) {
            logger.error("Error loading feedback page", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
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
}