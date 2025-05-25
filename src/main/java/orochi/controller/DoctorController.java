package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import orochi.model.Appointment;
import orochi.model.MedicalOrder;
import orochi.model.Patient;
import orochi.repository.MedicalOrderRepository;
import orochi.service.DoctorService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorController.class);

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @GetMapping("/dashboard")
    public String getDashboard(@RequestParam Integer doctorId, Model model) {
        try {
            logger.info("Loading dashboard for doctor ID: {}", doctorId);
            List<Appointment> todayAppointments = doctorService.getTodayAppointments(doctorId);
            List<Appointment> upcomingAppointments = doctorService.getUpcomingAppointments(doctorId);
            // Corrected method call to fetch pending orders by doctor ID and status
            List<MedicalOrder> pendingOrders = medicalOrderRepository.findByOrderByIdAndStatus(doctorId, "Pending");
            List<Patient> patients = doctorService.getPatientsWithAppointments(doctorId);

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

    @GetMapping("/appointments")
    public String getAllAppointments(@RequestParam Integer doctorId, Model model) {
        try {
            logger.info("Fetching all appointments for doctor ID: {}", doctorId);
            List<Appointment> appointments = doctorService.getAppointments(doctorId);
            model.addAttribute("appointments", appointments);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("title", "All Appointments");
            logger.debug("Retrieved {} appointments for doctor ID: {}", appointments.size(), doctorId);
            return "doctor/appointments";
        } catch (Exception e) {
            logger.error("Error fetching appointments for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve appointments: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/appointments/today")
    public String getTodayAppointments(@RequestParam Integer doctorId, Model model) {
        try {
            logger.info("Fetching today's appointments for doctor ID: {}", doctorId);
            List<Appointment> appointments = doctorService.getTodayAppointments(doctorId);
            model.addAttribute("appointments", appointments);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("title", "Today's Appointments");
            logger.debug("Retrieved {} today's appointments for doctor ID: {}", appointments.size(), doctorId);
            return "doctor/appointments";
        } catch (Exception e) {
            logger.error("Error fetching today's appointments for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve today's appointments: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/appointments/upcoming")
    public String getUpcomingAppointments(@RequestParam Integer doctorId, Model model) {
        try {
            logger.info("Fetching upcoming appointments for doctor ID: {}", doctorId);
            List<Appointment> appointments = doctorService.getUpcomingAppointments(doctorId);
            model.addAttribute("appointments", appointments);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("title", "Upcoming Appointments");
            logger.debug("Retrieved {} upcoming appointments for doctor ID: {}", appointments.size(), doctorId);
            return "doctor/appointments";
        } catch (Exception e) {
            logger.error("Error fetching upcoming appointments for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve upcoming appointments: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/appointments/date")
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

    @GetMapping("/appointments/status")
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

    @GetMapping("/appointment/{appointmentId}")
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
