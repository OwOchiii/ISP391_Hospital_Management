package com.hospital.controller;

import com.hospital.entity.Appointment;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.StatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private StatusRepository statusRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        logger.info("Accessing dashboard");

        // Fetch total counts
        long totalPatients = patientRepository.count();
        long totalDoctors = doctorRepository.count();
        long totalAppointments = appointmentRepository.count();
        double totalRevenue = totalAppointments * 1_000_000; // Assume 1,000,000 VNĐ per appointment

        // Fetch recent appointments (latest 5)
        List<Appointment> recentAppointments = appointmentRepository.findTop5ByOrderByIdDesc();

        // Add attributes to model
        model.addAttribute("totalPatients", totalPatients);
        model.addAttribute("totalDoctors", totalDoctors);
        model.addAttribute("totalOperations", totalAppointments); // Proxy for surgeries
        model.addAttribute("totalIncome", totalRevenue);
        model.addAttribute("appointments", recentAppointments);

        // Fetch status counts for chart
        List<Object[]> statusCounts = appointmentRepository.countAppointmentsByStatus();
        model.addAttribute("statusCounts", statusCounts);

        return "dashboard";
    }

    @PostMapping("/appointments/{id}/verify")
    @ResponseBody
    public String verifyAppointment(@PathVariable Long id, @RequestParam String statusName) {
        logger.debug("Verifying appointment id={} with statusName={}", id, statusName);
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            logger.error("Appointment not found: id={}", id);
            return "error: Appointment not found";
        }
        Status status = statusRepository.findByName(statusName);
        if (status == null) {
            logger.error("Status not found: name={}", statusName);
            return "error: Invalid status";
        }
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
        logger.info("Appointment id={} updated to status={}", id, statusName);
        // Cập nhật trạng thái cho patient liên quan
        if (appointment.getPatient() != null) {
            Patient patient = appointment.getPatient();
            patient.setAppointmentStatus(statusName);
            patientRepository.save(patient);
        }
        return "success";
    }

    @DeleteMapping("/appointments/{id}")
    @ResponseBody
    public String deleteAppointment(@PathVariable Long id) {
        logger.debug("Deleting appointment id={}", id);
        if (appointmentRepository.existsById(id)) {
            appointmentRepository.deleteById(id);
            logger.info("Appointment id={} deleted", id);
            return "success";
        }
        logger.error("Appointment not found: id={}", id);
        return "error: Appointment not found";
    }
}