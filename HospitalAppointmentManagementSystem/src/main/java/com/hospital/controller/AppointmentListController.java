package com.hospital.controller;

import com.hospital.model.Appointment;
import com.hospital.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/patient")
public class AppointmentListController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/appointments")
    public String showAppointmentList(Model model) {
        int patientId = 3; // Hardcoded for now
        List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patientId);
        model.addAttribute("appointments", appointments);
        return "appointmentList";
    }

    @GetMapping("/appointments/details/{id}")
    public String showAppointmentDetails(@PathVariable("id") int id) {
        return "appointmentDetails"; // Placeholder for Iteration 1
    }
}