package com.hospital.controller;

import com.hospital.entity.Appointment;
import com.hospital.entity.Status;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.StatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private StatusRepository statusRepository;

    @GetMapping("/appointments")
    public String listAppointments(Model model) {
        logger.info("Accessing appointments page");
        List<Appointment> appointments = appointmentRepository.findAll();
        model.addAttribute("appointments", appointments);
        return "appointments";
    }

    @PostMapping("/appointments/update")
    @ResponseBody
    public String updateAppointmentStatus(@RequestParam Long appointmentId, @RequestParam String status) {
        logger.debug("Updating appointment id={} with status={}", appointmentId, status);
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            logger.error("Appointment not found: id={}", appointmentId);
            return "error: Appointment not found";
        }
        Status newStatus = statusRepository.findByName(status);
        if (newStatus == null) {
            logger.error("Status not found: name={}", status);
            return "error: Invalid status";
        }
        appointment.setStatus(newStatus);
        appointmentRepository.save(appointment);
        logger.info("Appointment id={} updated to status={}", appointmentId, status);
        // Cập nhật trạng thái cho patient liên quan
        if (appointment.getPatient() != null) {
            Patient patient = appointment.getPatient();
            patient.setAppointmentStatus(status);
            patientRepository.save(patient);
        }
        return "success";
    }
}