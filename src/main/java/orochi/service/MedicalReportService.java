package orochi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.itextpdf.html2pdf.HtmlConverter;

import orochi.model.*;
import orochi.repository.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MedicalReportService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalReportService.class);

    @Value("${file.upload-dir:/uploads}")
    private String uploadDir;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @Autowired
    private MedicalResultRepository medicalResultRepository;

    @Autowired
    private MedicalReportRepository medicalReportRepository;

    @Autowired
    private ReportResultRepository reportResultRepository;

    /**
     * Generates a PDF medical report for an appointment
     *
     * @param appointmentId The ID of the appointment
     * @param doctorId The ID of the doctor generating the report
     * @param additionalNotes Any additional notes to include in the report
     * @return A map containing success status and the report URL if successful
     */
    @Transactional
    public Map<String, Object> generatePdfReport(Integer appointmentId, Integer doctorId, String additionalNotes) {
        try {
            // Get the appointment details
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (appointmentOpt.isEmpty()) {
                logger.error("Appointment not found for ID: {}", appointmentId);
                return createErrorResponse("Appointment not found");
            }

            Appointment appointment = appointmentOpt.get();

            // Get the doctor details
            Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
            if (doctorOpt.isEmpty()) {
                logger.error("Doctor not found for ID: {}", doctorId);
                return createErrorResponse("Doctor not found");
            }

            Doctor doctor = doctorOpt.get();

            // Get the patient details
            Optional<Patient> patientOpt = patientRepository.findById(appointment.getPatientId());
            if (patientOpt.isEmpty()) {
                logger.error("Patient not found for ID: {}", appointment.getPatientId());
                return createErrorResponse("Patient not found");
            }

            Patient patient = patientOpt.get();

            // Get all medical orders for this appointment
            List<MedicalOrder> orders = medicalOrderRepository.findByAppointmentIdOrderByOrderDate(appointmentId);

            // Get all medical results for this appointment
            List<MedicalResult> results = medicalResultRepository.findByAppointmentIdOrderByResultDateDesc(appointmentId);

            // Create a PDF file
            String fileName = generateFileName(appointmentId);
            String filePath = uploadDir + "/reports/" + fileName;

            // Ensure the directory exists
            createDirectoryIfNotExists(uploadDir + "/reports/");

            // Generate the HTML content using Thymeleaf
            Context context = new Context();
            context.setVariable("appointment", appointment);
            context.setVariable("doctor", doctor);
            context.setVariable("patient", patient);
            context.setVariable("orders", orders);
            context.setVariable("results", results);
            context.setVariable("additionalNotes", additionalNotes);
            context.setVariable("generatedDate", LocalDateTime.now());

            String htmlContent = templateEngine.process("medical/report-template", context);

            // Convert HTML to PDF
            File pdfFile = new File(filePath);
            try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
                HtmlConverter.convertToPdf(htmlContent, outputStream);
            }

            // Create a MedicalReport entity and save it
            MedicalReport report = new MedicalReport();
            report.setAppointmentId(appointmentId);
            report.setDoctorId(doctorId);
            report.setReportDate(LocalDateTime.now());
            report.setSummary(additionalNotes);
            report.setStatus("COMPLETED");
            report.setFileUrl(fileName);

            MedicalReport savedReport = medicalReportRepository.save(report);

            // Associate results with the report
            for (MedicalResult result : results) {
                ReportResult reportResult = new ReportResult();
                reportResult.setReportId(savedReport.getReportId());
                reportResult.setResultId(result.getResultId());
                reportResultRepository.save(reportResult);
            }

            logger.info("Successfully generated PDF report for appointment ID: {}, saved at {}", appointmentId, filePath);

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "PDF report generated successfully");
            response.put("reportId", savedReport.getReportId());
            response.put("fileUrl", "/download/report/" + fileName);

            return response;

        } catch (Exception e) {
            logger.error("Error generating PDF report for appointment ID: {}", appointmentId, e);
            return createErrorResponse("Failed to generate PDF report: " + e.getMessage());
        }
    }

    /**
     * Helper method to generate a unique file name for the PDF report
     */
    private String generateFileName(Integer appointmentId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return "medical_report_" + appointmentId + "_" + timestamp + ".pdf";
    }

    /**
     * Helper method to create a directory if it doesn't exist
     */
    private void createDirectoryIfNotExists(String dirPath) throws Exception {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Helper method to create an error response
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", errorMessage);
        return response;
    }

    /**
     * Get all reports for a specific appointment
     */
    public List<MedicalReport> getReportsByAppointment(Integer appointmentId) {
        return medicalReportRepository.findByAppointmentIdOrderByReportDateDesc(appointmentId);
    }

    /**
     * Get a specific report by ID
     */
    public Optional<MedicalReport> getReportById(Integer reportId) {
        return medicalReportRepository.findById(reportId);
    }
}
