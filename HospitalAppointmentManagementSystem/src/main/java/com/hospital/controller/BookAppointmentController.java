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
                                    @RequestParam("phoneNumber") String phoneNumber,
                                    @RequestParam("email") String email,
                                    Model model) {
        // Validate Specialty
        if (appointment.getSpecialtyId() == null) {
            result.rejectValue("specialtyId", "error.appointment", "Specialty is required");
        }

        // Validate Doctor
        if (appointment.getDoctorId() == null) {
            result.rejectValue("doctorId", "error.appointment", "Doctor is required");
        }

        if (result.hasErrors()) {
            model.addAttribute("specialties", appointmentService.getAllSpecialties());
            model.addAttribute("patient", appointmentService.findPatientById(1));
            return "bookAppointment";
        }

        if (appointment.getAppointmentTime() == null) {
            model.addAttribute("specialties", appointmentService.getAllSpecialties());
            model.addAttribute("patient", appointmentService.findPatientById(1));
            model.addAttribute("error", "Please select an appointment time.");
            return "bookAppointment";
        }

        // Fetch patient but do not update their info
        Patient patient = appointmentService.findPatientById(1);
        if (patient == null) {
            patient = new Patient("John Doe", 'M', new Date(631152000000L), "1234567890", "john@example.com");
            patient.setPatientId(1);
            patient = appointmentService.savePatient(patient);
        }

        // Set patient and other appointment details
        appointment.setPatient(patient);
        appointment.setCreatedDate(new Date());
        appointment.setStatus("Pending");

        // Store phoneNumber and email in Appointment
        appointment.setPhoneNumber(phoneNumber);
        appointment.setEmail(email);

        Specialty specialty = appointmentService.getAllSpecialties().stream()
                .filter(s -> s.getSpecialtyId().equals(appointment.getSpecialtyId()))
                .findFirst().orElse(null);
        appointment.setSpecialty(specialty);

        Doctor doctor = appointmentService.getDoctorsBySpecialty(appointment.getSpecialtyId()).stream()
                .filter(d -> d.getDoctorId().equals(appointment.getDoctorId()))
                .findFirst().orElse(null);
        appointment.setDoctor(doctor);

        appointmentService.saveAppointment(appointment);
        return "redirect:/patient/appointments";
    }

    @GetMapping("/doctors")
    @ResponseBody
    public List<Doctor> getDoctorsBySpecialty(@RequestParam("specialtyId") int specialtyId) {
        return appointmentService.getDoctorsBySpecialty(specialtyId);
    }

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