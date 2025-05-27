package com.hospital.controller;

import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.model.Patient;
import com.hospital.model.Specialty;
import com.hospital.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/patient")
public class BookAppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/book")
    public String showBookAppointmentForm(Model model) {
        Appointment appointment = new Appointment();
        model.addAttribute("appointment", appointment);
        model.addAttribute("specialties", appointmentService.getAllSpecialties());
        // Hardcoded patient data for now
        // Fetch or create patient with id = 1 for display
        Patient patient = appointmentService.findPatientById(1);
        if (patient == null) {
            patient = new Patient("John Doe", 'M', new Date(631152000000L), "1234567890", "john@example.com");
            patient.setPatientId(1);
            patient = appointmentService.savePatient(patient);
        }
        model.addAttribute("patient", patient);
        return "bookAppointment";
    }

    @PostMapping("/book")
    public String submitAppointment(@Valid @ModelAttribute("appointment") Appointment appointment,
                                    BindingResult result,
                                    @RequestParam("fullName") String fullName,
                                    @RequestParam("gender") String gender,
                                    @RequestParam("birthdate") String birthdateStr,
                                    @RequestParam("phoneNumber") String phoneNumber,
                                    @RequestParam("email") String email,
                                    Model model) {
        if (result.hasErrors()) {
            model.addAttribute("specialties", appointmentService.getAllSpecialties());
            model.addAttribute("patient", new Patient(fullName, gender.charAt(0), parseDate(birthdateStr), phoneNumber, email));
            return "bookAppointment";
        }

        // Fetch or create patient with id = 1
        Patient patient = appointmentService.findPatientById(1);
        if (patient == null) {
            patient = new Patient(fullName, gender.charAt(0), parseDate(birthdateStr), phoneNumber, email);
            patient.setPatientId(1);
        } else {
            // Update existing patient's details
            patient.setFullName(fullName);
            patient.setGender(gender.charAt(0));
            patient.setBirthdate(parseDate(birthdateStr));
            patient.setPhoneNumber(phoneNumber);
            patient.setEmail(email);
        }
        patient = appointmentService.savePatient(patient);

        appointment.setPatient(patient);
        appointment.setCreatedDate(new Date());
        appointment.setStatus("Pending");

        // Fetch Specialty and Doctor from repositories
        Specialty specialty = appointmentService.getAllSpecialties().stream()
                .filter(s -> s.getSpecialtyId().equals(appointment.getSpecialtyId()))
                .findFirst().orElse(null);
        appointment.setSpecialty(specialty);

        if (appointment.getDoctorId() != null) {
            Doctor doctor = appointmentService.getDoctorsBySpecialty(appointment.getSpecialtyId()).stream()
                    .filter(d -> d.getDoctorId().equals(appointment.getDoctorId()))
                    .findFirst().orElse(null);
            appointment.setDoctor(doctor);
        }

        appointmentService.saveAppointment(appointment);
        return "redirect:/patient/appointments";
    }

    @GetMapping("/doctors")
    @ResponseBody
    public List<Doctor> getDoctorsBySpecialty(@RequestParam("specialtyId") int specialtyId) {
        return appointmentService.getDoctorsBySpecialty(specialtyId);
    }

    // Helper method to parse date
    private Date parseDate(String dateStr) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.parse(dateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format for birthdate: " + dateStr, e);
        }
    }

    // New endpoint to fetch booked times for a specific date
    @GetMapping("/booked-times")
    @ResponseBody
    public List<String> getBookedTimes(@RequestParam("date") String dateStr) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = dateFormat.parse(dateStr);
            List<Appointment> appointments = appointmentService.getAppointmentsByDate(date);
            return appointments.stream()
                    .map(appointment -> appointment.getAppointmentTime().toString())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr, e);
        }
    }
}