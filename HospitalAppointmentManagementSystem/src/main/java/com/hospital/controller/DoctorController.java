package com.hospital.controller;

import com.hospital.model.Doctor;
import com.hospital.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DoctorController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/doctors")
    public List<Doctor> getDoctorsBySpecialty(@RequestParam int specialtyId) {
        return appointmentService.getDoctorsBySpecialty(specialtyId);
    }
}