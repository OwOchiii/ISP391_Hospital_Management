package orochi.service;

import com.itextpdf.html2pdf.HtmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import orochi.model.Doctor;
import orochi.model.MedicalRecord;
import orochi.model.Patient;
import orochi.model.RecordMedication;
import orochi.repository.DoctorRepository;
import orochi.repository.PatientRepository;
import orochi.service.RecordMedicationService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
public class PDFExportService {

    private static final Logger logger = LoggerFactory.getLogger(PDFExportService.class);

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private RecordMedicationService medicationService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Value("${file.upload.directory:uploads}")
    private String uploadDirectory;

    /**
     * Generate a PDF medical record report
     *
     * @param recordId The ID of the medical record to generate a PDF for
     * @return A map containing success status and the report URL if successful
     */
    public Map<String, Object> generateMedicalRecordPdf(Integer recordId) {
        try {
            logger.info("Generating PDF for medical record ID: {}", recordId);

            // Fetch the medical record
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.getMedicalRecordById(recordId);

            if (medicalRecordOpt.isEmpty()) {
                logger.warn("Medical record not found for ID: {}", recordId);
                return createErrorResponse("Medical record not found");
            }

            MedicalRecord medicalRecord = medicalRecordOpt.get();

            // Get the patient information
            Optional<Patient> patientOpt = doctorService.getPatientDetails(medicalRecord.getPatientId());


            if (patientOpt.isEmpty()) {
                logger.warn("Patient not found for medical record ID: {}", recordId);
                return createErrorResponse("Patient information not found");
            }

            Patient patient = patientOpt.get();

            // Get the doctor information
            Optional<Doctor> doctorOpt = doctorRepository.findById(medicalRecord.getDoctorId());

            if (doctorOpt.isEmpty()) {
                logger.warn("Doctor not found for medical record ID: {}", recordId);
                return createErrorResponse("Doctor information not found");
            }

            Doctor doctor = doctorOpt.get();

            // Calculate patient age
            int patientAge = 0;
            if (patient.getDateOfBirth() != null) {
                patientAge = java.time.Period.between(patient.getDateOfBirth(), java.time.LocalDate.now()).getYears();
            }

            // Get medications for the medical record
            List<RecordMedication> medications = medicationService.getMedicationsByRecordId(recordId);

            // Create directory path for medical reports
            String reportsDir = uploadDirectory + File.separator + "medical-results";
            createDirectoryIfNotExists(reportsDir);

            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "medical_record_" + recordId + "_" + timestamp + ".pdf";
            String outputPath = reportsDir + File.separator + filename;

            // Prepare context for the template
            Context context = new Context();
            context.setVariable("medicalRecord", medicalRecord);
            context.setVariable("patient", patient);
            context.setVariable("doctor", doctor);
            context.setVariable("patientAge", patientAge);
            context.setVariable("medications", medications);

            // Process the template
            String processedHtml = templateEngine.process("medical/pdf-medical-record", context);

            // Generate PDF using iText
            generatePdf(processedHtml, outputPath);

            logger.info("PDF generated successfully: {}", outputPath);

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "PDF report generated successfully");
            response.put("fileUrl", "/download/report/" + filename);
            return response;

        } catch (Exception e) {
            logger.error("Failed to generate PDF from template", e);
            return createErrorResponse("PDF generation failed: " + e.getMessage());
        }
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
     * Generate a PDF from HTML content using iText HTML2PDF
     *
     * @param html The HTML content to convert to PDF
     * @param outputPath The file path where the PDF will be saved
     * @throws IOException If there is an error writing to the file
     */
    private void generatePdf(String html, String outputPath) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            // Convert HTML to PDF using iText HTML2PDF
            HtmlConverter.convertToPdf(html, outputStream);
        }
    }

    /**
     * Generate a PDF from a Thymeleaf template (legacy method kept for backward compatibility)
     *
     * @param templateName Name of the Thymeleaf template
     * @param variables Variables to pass to the template
     * @param baseFileName Base name for the generated PDF file
     * @return The file path of the generated PDF
     */
    public String generatePdfFromTemplate(String templateName, Map<String, Object> variables, String baseFileName) {
        try {
            // Create context and process template
            Context context = new Context();
            variables.forEach(context::setVariable);

            String processedHtml = templateEngine.process(templateName, context);

            // Create output directory if it doesn't exist
            String reportsDir = uploadDirectory + File.separator + "medical-results";
            Path reportsDirPath = Paths.get(reportsDir);
            if (!Files.exists(reportsDirPath)) {
                Files.createDirectories(reportsDirPath);
            }

            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
            String filename = baseFileName + "_" + timestamp + "_" + uniqueId + ".pdf";
            String outputPath = reportsDir + File.separator + filename;

            // Generate PDF using iText
            generatePdf(processedHtml, outputPath);

            logger.info("PDF generated successfully: {}", outputPath);
            return filename;

        } catch (Exception e) {
            logger.error("Failed to generate PDF from template", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
}
