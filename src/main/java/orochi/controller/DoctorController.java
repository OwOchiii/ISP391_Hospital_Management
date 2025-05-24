package orochi.controller;

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

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @GetMapping("/dashboard")
    public String getDashboard(@RequestParam Integer doctorId, Model model) {
        try {
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

            return "doctor/dashboard";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred while loading the dashboard.");
            return "error";
        }
    }

    @GetMapping("/appointments")
    public String getAllAppointments(@RequestParam Integer doctorId, Model model) {
        List<Appointment> appointments = doctorService.getAppointments(doctorId);
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("title", "All Appointments");
        return "doctor/appointments";
    }

    @GetMapping("/appointments/today")
    public String getTodayAppointments(@RequestParam Integer doctorId, Model model) {
        List<Appointment> appointments = doctorService.getTodayAppointments(doctorId);
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("title", "Today's Appointments");
        return "doctor/appointments";
    }

    @GetMapping("/appointments/upcoming")
    public String getUpcomingAppointments(@RequestParam Integer doctorId, Model model) {
        List<Appointment> appointments = doctorService.getUpcomingAppointments(doctorId);
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("title", "Upcoming Appointments");
        return "doctor/appointments";
    }

    @GetMapping("/appointments/date")
    public String getAppointmentsByDate(
            @RequestParam Integer doctorId,
            @RequestParam LocalDate date,
            Model model) {
        List<Appointment> appointments = doctorService.getAppointmentsByDate(doctorId, date);
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("selectedDate", date);
        model.addAttribute("title", "Appointments on " + date);
        return "doctor/appointments";
    }

    @GetMapping("/appointments/status")
    public String getAppointmentsByStatus(
            @RequestParam Integer doctorId,
            @RequestParam String status,
            Model model) {
        List<Appointment> appointments = doctorService.getAppointmentsByStatus(doctorId, status);
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("title", status + " Appointments");
        return "doctor/appointments";
    }

    @GetMapping("/appointment/{appointmentId}")
    public String getAppointmentDetails(
            @PathVariable Integer appointmentId,
            @RequestParam Integer doctorId,
            Model model) {
        Optional<Appointment> appointment = doctorService.getAppointmentDetails(appointmentId, doctorId);
        if (appointment.isPresent()) {
            Optional<Patient> patient = doctorService.getPatientDetails(appointment.get().getPatientId());
            List<MedicalOrder> medicalOrders = medicalOrderRepository.findByAppointmentIdOrderByOrderDate(appointmentId);

            model.addAttribute("appointment", appointment.get());
            model.addAttribute("patient", patient.orElse(null));
            model.addAttribute("medicalOrders", medicalOrders);
            model.addAttribute("doctorId", doctorId);
            return "doctor/appointment-details";
        } else {
            model.addAttribute("errorMessage", "Appointment not found or access denied.");
            return "error";
        }
    }

    @GetMapping("/patients")
    public String getPatientsWithAppointments(@RequestParam Integer doctorId, Model model) {
        List<Patient> patients = doctorService.getPatientsWithAppointments(doctorId);
        model.addAttribute("patients", patients);
        model.addAttribute("doctorId", doctorId);
        return "doctor/patients";
    }

    @GetMapping("/patient/{patientId}")
    public String getPatientDetails(
            @PathVariable Integer patientId,
            @RequestParam Integer doctorId,
            Model model) {
        Optional<Patient> patient = doctorService.getPatientDetails(patientId);
        if (patient.isPresent()) {
            List<Appointment> appointmentHistory = doctorService.getAppointmentRepository()
                .findByPatientIdAndDoctorIdOrderByDateTimeDesc(patientId, doctorId);

            model.addAttribute("patient", patient.get());
            model.addAttribute("appointmentHistory", appointmentHistory);
            model.addAttribute("doctorId", doctorId);
            return "doctor/patient-details";
        } else {
            model.addAttribute("errorMessage", "Patient not found.");
            return "error";
        }
    }

    @GetMapping("/patients/search")
    public String searchPatients(
            @RequestParam String name,
            @RequestParam Integer doctorId,
            Model model) {
        List<Patient> patients = doctorService.searchPatientsByName(name);
        model.addAttribute("patients", patients);
        model.addAttribute("searchTerm", name);
        model.addAttribute("doctorId", doctorId);
        return "doctor/patient-search-results";
    }

    @GetMapping("/medical-orders")
    public String getDoctorMedicalOrders(@RequestParam Integer doctorId, Model model) {
        List<MedicalOrder> orders = medicalOrderRepository.findByOrderById(doctorId);
        model.addAttribute("medicalOrders", orders);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("title", "All Medical Orders");
        return "doctor/medical-orders";
    }

    @GetMapping("/medical-orders/status")
    public String getMedicalOrdersByStatus(
            @RequestParam Integer doctorId,
            @RequestParam String status,
            Model model) {
        List<MedicalOrder> orders = medicalOrderRepository.findByOrderByIdAndStatus(doctorId, status);
        model.addAttribute("medicalOrders", orders);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("title", status + " Medical Orders");
        return "doctor/medical-orders";
    }
}