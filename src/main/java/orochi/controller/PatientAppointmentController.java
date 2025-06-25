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
import orochi.model.*;
import orochi.repository.AppointmentRepository;
import orochi.repository.DoctorSpecializationRepository;
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

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorSpecializationRepository doctorSpecializationRepository;

    @Autowired
    private orochi.repository.MedicalReportRepository medicalReportRepository;

    @Autowired
    private orochi.repository.PrescriptionRepository prescriptionRepository;

    @GetMapping("/book-appointment")
    public String showBookAppointmentForm(
            @RequestParam(required = false) Integer patientId,
            @RequestParam(required = false) Integer appointmentId,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (patientId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                patientId = userDetails.getPatientId();
                if (patientId == null) {
                    redirectAttributes.addFlashAttribute("successMessage", "No patient ID found. Please login as a patient.");
                    return "redirect:/patient/appointment-list";
                }
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Authentication required to book an appointment.");
                return "redirect:/patient/appointment-list";
            }
        }

        Integer finalPatientId = patientId;
        Patient patient;
        try {
            patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + finalPatientId));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("successMessage", "Invalid patient ID! Please try again.");
            return "redirect:/patient/appointment-list";
        }

        AppointmentFormDTO appointmentForm;
        boolean isUpdate = appointmentId != null;
        if (isUpdate) {
            try {
                Appointment appointment = appointmentRepository.findById(appointmentId)
                        .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));
                if (!appointment.getPatientId().equals(patientId)) {
                    redirectAttributes.addFlashAttribute("successMessage", "Invalid appointment ID or patient ID! Please try again.");
                    return "redirect:/patient/appointment-list";
                }
                DoctorSpecialization doctorSpecialization = doctorSpecializationRepository.findByDoctorId(appointment.getDoctorId())
                        .orElseThrow(() -> new RuntimeException("Specialization not found for doctor ID: " + appointment.getDoctorId()));
                appointmentForm = new AppointmentFormDTO();
                appointmentForm.setAppointmentId(appointmentId);
                appointmentForm.setPatientId(patientId);
                appointmentForm.setSpecialtyId(doctorSpecialization.getSpecialization().getSpecId());
                appointmentForm.setDoctorId(appointment.getDoctorId());
                appointmentForm.setAppointmentDate(appointment.getDateTime().toLocalDate());
                appointmentForm.setAppointmentTime(appointment.getDateTime().toLocalTime().toString());
                appointmentForm.setEmail(appointment.getEmail() != null ? appointment.getEmail() : patient.getUser().getEmail());
                appointmentForm.setPhoneNumber(appointment.getPhoneNumber() != null ? appointment.getPhoneNumber() : patient.getUser().getPhoneNumber());
                appointmentForm.setDescription(appointment.getDescription());
            } catch (RuntimeException e) {
                redirectAttributes.addFlashAttribute("successMessage", "Invalid appointment ID or patient ID! Please try again.");
                return "redirect:/patient/appointment-list";
            }
        } else {
            appointmentForm = new AppointmentFormDTO();
            appointmentForm.setPatientId(patientId);
            if (patient.getUser() != null) {
                appointmentForm.setEmail(patient.getUser().getEmail());
                appointmentForm.setPhoneNumber(patient.getUser().getPhoneNumber());
            }
        }

        if (!model.containsAttribute("appointmentForm")) {
            model.addAttribute("appointmentForm", appointmentForm);
        }

        List<Specialization> specializations = appointmentService.getAllSpecializations();
        model.addAttribute("specialties", specializations);
        model.addAttribute("patient", patient);
        model.addAttribute("patientId", patientId);
        model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");
        model.addAttribute("isUpdate", isUpdate);

        return "patient/book-appointment";
    }

    @GetMapping("/api/doctors-by-specialty")
    @ResponseBody
    public List<Map<String, Object>> getDoctorsBySpecialty(@RequestParam Integer specialtyId) {
        List<Doctor> doctors = appointmentService.getDoctorsBySpecialization(specialtyId);
        return doctors.stream().map(doctor -> {
            Map<String, Object> result = new HashMap<>();
            result.put("doctorId", doctor.getDoctorId());
            result.put("fullName", doctor.getUser() != null ? doctor.getUser().getFullName() : "Unknown");
            result.put("bioDescription", doctor.getBioDescription());
            return result;
        }).collect(Collectors.toList());
    }

    @GetMapping("/api/booked-times")
    @ResponseBody
    public List<String> getBookedTimes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer doctorId) {
        return appointmentService.getBookedTimeSlots(date, doctorId);
    }

    @GetMapping("/api/doctor-availability")
    @ResponseBody
    public Map<String, List<String>> getDoctorAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer doctorId,
            @RequestParam(required = false) Integer appointmentId) {
        return appointmentService.getDoctorAvailability(date, doctorId, appointmentId);
    }

    @PostMapping("/book-appointment")
    public String bookAppointment(
            @Valid @ModelAttribute("appointmentForm") AppointmentFormDTO appointmentForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            List<Specialization> specializations = appointmentService.getAllSpecializations();
            Patient patient = patientRepository.findById(appointmentForm.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + appointmentForm.getPatientId()));
            model.addAttribute("specialties", specializations);
            model.addAttribute("patient", patient);
            model.addAttribute("patientId", appointmentForm.getPatientId());
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");
            model.addAttribute("isUpdate", appointmentForm.getAppointmentId() != null);
            return "patient/book-appointment";
        }

        try {
            Appointment appointment;
            if (appointmentForm.getAppointmentId() != null) {
                appointment = appointmentService.updateAppointment(
                        appointmentForm.getAppointmentId(),
                        appointmentForm.getPatientId(),
                        appointmentForm.getDoctorId(),
                        appointmentForm.getAppointmentDate(),
                        appointmentForm.getAppointmentTime(),
                        appointmentForm.getEmail(),
                        appointmentForm.getPhoneNumber(),
                        appointmentForm.getDescription()
                );
                redirectAttributes.addFlashAttribute("successMessage", "Your appointment has been successfully updated.");
            } else {
                appointment = appointmentService.bookAppointment(
                        appointmentForm.getPatientId(),
                        appointmentForm.getDoctorId(),
                        appointmentForm.getAppointmentDate(),
                        appointmentForm.getAppointmentTime(),
                        appointmentForm.getEmail(),
                        appointmentForm.getPhoneNumber(),
                        appointmentForm.getDescription()
                );
                redirectAttributes.addFlashAttribute("successMessage",
                        "Your appointment has been successfully booked for " +
                                appointmentForm.getAppointmentDate() + " at " +
                                formatTimeForDisplay(appointmentForm.getAppointmentTime()));
            }
            return "redirect:/patient/appointment-list";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            List<Specialization> specializations = appointmentService.getAllSpecializations();
            Patient patient = patientRepository.findById(appointmentForm.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + appointmentForm.getPatientId()));
            model.addAttribute("specialties", specializations);
            model.addAttribute("patient", patient);
            model.addAttribute("patientId", appointmentForm.getPatientId());
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");
            model.addAttribute("isUpdate", appointmentForm.getAppointmentId() != null);
            return "patient/book-appointment";
        }
    }

    private String formatTimeForDisplay(String time) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
        return String.format("%d:%02d %s", displayHour, minute, period);
    }

    /**
     * View latest medical report for an appointment
     * @param id The appointment ID
     * @param patientId The patient ID for authorization
     * @return The medical report PDF or an error page
     */
    @GetMapping("/appointment-list-legacy/{id}/report")
    public String viewLatestMedicalReport(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer patientId,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validate patient authorization
        if (patientId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                patientId = userDetails.getPatientId();
            }
        }

        if (patientId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Patient authentication required");
            return "redirect:/patient/appointment-list";
        }

        // Check if appointment exists and belongs to the patient
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found");
            return "redirect:/patient/appointment-list";
        }

        if (!appointment.getPatientId().equals(patientId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access to this appointment");
            return "redirect:/patient/appointment-list";
        }

        // Get the latest medical report for this appointment
        List<MedicalReport> reports = medicalReportRepository.findByAppointmentIdOrderByReportDateDesc(id);
        MedicalReport latestReport = reports.isEmpty() ? null : reports.get(0);

        // Get the latest prescription for this appointment
        List<Prescription> prescriptions = prescriptionRepository.findByAppointmentIdOrderByPrescriptionDateDesc(id);
        Prescription latestPrescription = prescriptions.isEmpty() ? null : prescriptions.get(0);

        // Prepare the model for the report template
        Patient patient = patientRepository.findById(patientId).orElse(null);
        String patientName = patient != null && patient.getUser() != null ? patient.getUser().getFullName() : "Patient";

        model.addAttribute("appointment", appointment);
        model.addAttribute("medicalReport", latestReport);
        model.addAttribute("prescription", latestPrescription);
        model.addAttribute("patientId", patientId);
        model.addAttribute("patientName", patientName);

        return "patient/medical-report";
    }

    /**
     * Download medical report PDF for an appointment
     * @param id The appointment ID
     * @param patientId The patient ID for authorization
     * @return The medical report PDF or an error page
     */
    @GetMapping("/appointment-list/{id}/download-report")
    public String downloadMedicalReportPdf(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer patientId,
            RedirectAttributes redirectAttributes) {

        // Validate patient authorization
        if (patientId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                patientId = userDetails.getPatientId();
            }
        }

        if (patientId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Patient authentication required");
            return "redirect:/patient/appointment-list";
        }

        // Check if appointment exists and belongs to the patient
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found");
            return "redirect:/patient/appointment-list";
        }

        if (!appointment.getPatientId().equals(patientId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access to this appointment");
            return "redirect:/patient/appointment-list";
        }

        // Get the latest medical report for this appointment
        List<MedicalReport> reports = medicalReportRepository.findByAppointmentIdOrderByReportDateDesc(id);
        if (reports.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No medical report available for this appointment");
            return "redirect:/patient/appointment-list";
        }

        MedicalReport latestReport = reports.get(0);
        if (latestReport.getFileUrl() == null || latestReport.getFileUrl().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Medical report file not found");
            return "redirect:/patient/appointment-list";
        }

        // Redirect to the file download controller to serve the PDF
        return "redirect:/download/report/" + latestReport.getReportId() + "?inline=true";
    }
}
