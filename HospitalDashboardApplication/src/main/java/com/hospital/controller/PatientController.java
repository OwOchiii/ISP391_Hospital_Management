package com.hospital.controller;

import com.hospital.entity.Patient;
import com.hospital.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PatientController {

    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    @Autowired
    private PatientRepository patientRepository;

    @GetMapping("/patients")
    public String listPatients(Model model) {
        logger.info("Accessing patients page");
        List<Patient> patients = patientRepository.findAll();
        // Lấy trạng thái mới nhất từ appointments cho mỗi patient
        for (Patient patient : patients) {
            if (patient.getAppointments() != null && !patient.getAppointments().isEmpty()) {
                patient.setAppointmentStatus(patient.getAppointments().get(0).getStatus().getName());
            } else {
                patient.setAppointmentStatus("N/A");
            }
        }
        model.addAttribute("patients", patients);
        return "patients";
    }
}