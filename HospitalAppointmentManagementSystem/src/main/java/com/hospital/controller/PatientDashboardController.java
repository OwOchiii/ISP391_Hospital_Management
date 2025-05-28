package com.hospital.controller;

import com.hospital.model.Doctor;
import com.hospital.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/patient")
public class PatientDashboardController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        int patientId = 1; // Hardcoded for now
        model.addAttribute("patientId", patientId);
        return "patientDashboard";
    }

    @GetMapping("/search")
    public String searchDoctors(@RequestParam("query") String query, Model model) {
        List<Doctor> doctors = appointmentService.searchDoctors(query);
        model.addAttribute("doctors", doctors);
        return "fragments/doctorSearchResults :: doctorResults";
    }

    @GetMapping("/profile")
    public String showProfile() {
        return "profile";
    }

    @GetMapping("/support")
    public String showSupport() {
        return "support";
    }

    @GetMapping("/feedback")
    public String showFeedback() {
        return "feedback";
    }

    @GetMapping("/doctor-search")
    public String showDoctorSearch() {
        return "doctorSearch"; // Redirect to hardcoded doctor search page
    }
}