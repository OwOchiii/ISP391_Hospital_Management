package orochi.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import orochi.service.ExcelExportService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private ExcelExportService excelExportService;

    // ... existing code ...

    @GetMapping("/appointments/export")
    public ResponseEntity<InputStreamResource> exportAppointments(HttpServletResponse response) throws IOException {
        // Get current authenticated patient
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.badRequest().build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Integer patientId = userDetails.getPatientId();
        if (patientId == null) {
            return ResponseEntity.badRequest().build();
        }

        // Get patient information
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Get all appointments for this patient
        List<Appointment> appointments = appointmentRepository.findByPatientIdOrderByDateTimeDesc(patientId);

        // Generate Excel file
        ByteArrayInputStream excelStream = excelExportService.exportAppointmentsToExcel(
                appointments,
                patient.getUser().getFullName()
        );

        // Set the filename with current date
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "appointments_" + currentDateTime + ".xlsx";

        // Return the excel file as a downloadable resource
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(excelStream));
    }
}
