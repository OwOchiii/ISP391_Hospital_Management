package orochi.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.config.CustomUserDetails;
import orochi.dto.AppointmentFormDTO;
import orochi.model.Appointment;
import orochi.model.Doctor;
import orochi.model.Patient;
import orochi.model.Specialization;
import orochi.repository.PatientRepository;
import orochi.service.AppointmentService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/patient")
public class PatientAppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientRepository patientRepository;

    /**
     * Display the appointment booking form
     */
    @GetMapping("/book-appointment")
    public String showBookAppointmentForm(@RequestParam(required = false) Integer patientId, Model model) {
        // If patientId is not provided, get it from the authenticated user
        if (patientId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                patientId = userDetails.getPatientId();

                // If still null, redirect to error page
                if (patientId == null) {
                    return "redirect:/error?message=No patient ID found. Please login as a patient.";
                }
            } else {
                // Not authenticated as a user with patient details
                return "redirect:/error?message=Authentication required to book an appointment.";
            }
        }

        // Get all specializations for the dropdown
        List<Specialization> specializations = appointmentService.getAllSpecializations();

        // Get patient information
        Integer finalPatientId = patientId;
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + finalPatientId));

        // Prepare form backing object
        if (!model.containsAttribute("appointmentForm")) {
            AppointmentFormDTO appointmentForm = new AppointmentFormDTO();
            appointmentForm.setPatientId(patientId);

            // Pre-populate contact info if available
            if (patient.getUser() != null) {
                appointmentForm.setEmail(patient.getUser().getEmail());
                appointmentForm.setPhoneNumber(patient.getUser().getPhoneNumber());
            }

            model.addAttribute("appointmentForm", appointmentForm);
        }

        model.addAttribute("specialties", specializations);
        model.addAttribute("patient", patient);
        model.addAttribute("patientId", patientId);
        model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");

        return "patient/book-appointment";
    }

    /**
     * API endpoint to get doctors by specialty
     */
    @GetMapping("/api/doctors-by-specialty")
    @ResponseBody
    public List<Map<String, Object>> getDoctorsBySpecialty(@RequestParam Integer specialtyId) {
        List<Doctor> doctors = appointmentService.getDoctorsBySpecialization(specialtyId);

        // Convert to simplified objects to avoid circular references
        return doctors.stream().map(doctor -> {
            Map<String, Object> result = new HashMap<>();
            result.put("doctorId", doctor.getDoctorId());

            // Add user details if available
            if (doctor.getUser() != null) {
                result.put("fullName", doctor.getUser().getFullName());
            } else {
                result.put("fullName", "Unknown");
            }

            // Add bio if available
            result.put("bioDescription", doctor.getBioDescription());

            return result;
        }).collect(Collectors.toList());
    }

    /**
     * API endpoint to get booked time slots for a specific date and doctor
     */
    @GetMapping("/api/booked-times")
    @ResponseBody
    public List<String> getBookedTimes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer doctorId) {
        return appointmentService.getBookedTimeSlots(date, doctorId);
    }

    /**
     * API endpoint to get doctor availability for a specific date
     */
    @GetMapping("/api/doctor-availability")
    @ResponseBody
    public Map<String, List<String>> getDoctorAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer doctorId) {
        return appointmentService.getDoctorAvailability(date, doctorId);
    }

    /**
     * Handle appointment form submission
     */
    @PostMapping("/book-appointment")
    public String bookAppointment(
            @Valid @ModelAttribute("appointmentForm") AppointmentFormDTO appointmentForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // Get all specializations for the dropdown
            List<Specialization> specializations = appointmentService.getAllSpecializations();

            // Get patient information
            Patient patient = patientRepository.findById(appointmentForm.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + appointmentForm.getPatientId()));

            model.addAttribute("specialties", specializations);
            model.addAttribute("patient", patient);
            model.addAttribute("patientId", appointmentForm.getPatientId());
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");

            return "patient/book-appointment";
        }

        try {
            // Book the appointment
            Appointment bookedAppointment = appointmentService.bookAppointment(
                    appointmentForm.getPatientId(),
                    appointmentForm.getDoctorId(),
                    appointmentForm.getAppointmentDate(),
                    appointmentForm.getAppointmentTime(),
                    appointmentForm.getEmail(),
                    appointmentForm.getPhoneNumber(),
                    appointmentForm.getDescription(),
                    appointmentForm.getEmergencyContact()
            );

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your appointment has been successfully booked for " +
                            appointmentForm.getAppointmentDate() + " at " +
                            formatTimeForDisplay(appointmentForm.getAppointmentTime()));

            // Redirect to the same page to avoid form resubmission
            return "redirect:/patient/appointment-list";

        } catch (Exception e) {
            // Handle any errors
            model.addAttribute("errorMessage", e.getMessage());

            // Get all specializations for the dropdown
            List<Specialization> specializations = appointmentService.getAllSpecializations();

            // Get patient information
            Patient patient = patientRepository.findById(appointmentForm.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + appointmentForm.getPatientId()));

            model.addAttribute("specialties", specializations);
            model.addAttribute("patient", patient);
            model.addAttribute("patientId", appointmentForm.getPatientId());
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");

            return "patient/book-appointment";
        }
    }

    /**
     * Format time string for display (convert 24-hour format to 12-hour AM/PM format)
     */
    private String formatTimeForDisplay(String time) {
        // Time string will be in format "HH:mm:ss"
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);

        return String.format("%d:%02d %s", displayHour, minute, period);
    }
}