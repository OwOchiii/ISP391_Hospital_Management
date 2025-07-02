package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.config.CustomUserDetails;
import orochi.model.Doctor;
import orochi.model.MedicalRecord;
import orochi.model.Patient;
import orochi.model.RecordMedication;
import orochi.repository.DoctorRepository;
import orochi.repository.NotificationRepository;
import orochi.service.DoctorService;
import orochi.service.MedicalRecordService;
import orochi.service.PDFExportService;
import orochi.service.RecordMedicationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/doctor/medical-records")
public class MedicalRecordController {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordController.class);

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PDFExportService pdfExportService;

    @Autowired
    private RecordMedicationService medicationService;

    /**
     * Display the medical records page for a specific patient
     *
     * @param patientId The ID of the patient
     * @param doctorId The ID of the doctor
     * @param model The Spring model
     * @param authentication The current authentication object
     * @return The medical records page template
     */
    @GetMapping
    public String getMedicalRecords(
            @RequestParam Integer patientId,
            @RequestParam(required = false) Integer doctorId,
            Model model,
            Authentication authentication) {
        try {
            logger.info("Fetching medical records for patient ID: {}", patientId);

            // If doctorId is not provided, try to get it from authentication
            if (doctorId == null && authentication != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    doctorId = userDetails.getDoctorId();
                    logger.info("Retrieved doctorId {} from authentication", doctorId);
                }
            }

            // Fetch patient details
            Optional<Patient> patientOpt = doctorService.getPatientDetails(patientId);

            if (patientOpt.isEmpty()) {
                logger.warn("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found.");
                return "error";
            }

            Patient patient = patientOpt.get();
            model.addAttribute("patient", patient);

            // Get doctor info
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
            if (doctor != null && doctor.getUser() != null) {
                model.addAttribute("doctorName", doctor.getUser().getFullName());
            }
            model.addAttribute("doctorId", doctorId);

            // Get unread notifications count
            if (doctor != null) {
                List<orochi.model.Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(doctor.getUserId());
                if (notifications != null) {
                    long unreadNotifications = notifications.stream()
                            .filter(notification -> !notification.isRead())
                            .count();
                    model.addAttribute("unreadNotifications", unreadNotifications);
                }
            }

            // Fetch existing medical record if available
            Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.getMedicalRecordByPatientId(patientId);

            if (medicalRecordOpt.isPresent()) {
                MedicalRecord medicalRecord = medicalRecordOpt.get();
                model.addAttribute("medicalRecord", medicalRecord);
                model.addAttribute("medications", medicalRecord.getMedications());
            } else {
                model.addAttribute("medications", new ArrayList<RecordMedication>());
            }

            logger.debug("Successfully retrieved medical records for patient ID: {}", patientId);
            return "doctor/medical-records";
        } catch (Exception e) {
            logger.error("Error fetching medical records for patient ID: {}", patientId, e);
            model.addAttribute("errorMessage", "Failed to retrieve medical records: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Save a medical record
     *
     * @param medicalRecord The medical record to save
     * @param doctorId The ID of the doctor
     * @param redirectAttributes RedirectAttributes for flash messages
     * @return Redirect to the medical records page
     */
    @PostMapping("/save")
    public String saveMedicalRecord(
            @ModelAttribute MedicalRecord medicalRecord,
            @RequestParam Integer doctorId,
            RedirectAttributes redirectAttributes) {
        try {
            logger.info("Saving medical record for patient ID: {}", medicalRecord.getPatientId());

            // Set the doctor ID
            medicalRecord.setDoctorId(doctorId);

            // Update timestamps
            medicalRecord.setLastUpdated(LocalDateTime.now());

            // Check if we're updating an existing record or creating a new one
            if (medicalRecord.getRecordId() == null) {
                // Look for existing record for this patient
                Optional<MedicalRecord> existingRecord = medicalRecordService.getMedicalRecordByPatientId(medicalRecord.getPatientId());

                if (existingRecord.isPresent()) {
                    // Update the existing record instead of creating a new one
                    medicalRecord.setRecordId(existingRecord.get().getRecordId());
                    medicalRecord.setRecordDate(existingRecord.get().getRecordDate());
                    logger.info("Updating existing medical record with ID: {}", medicalRecord.getRecordId());
                } else {
                    // This is genuinely a new record
                    medicalRecord.setRecordDate(LocalDateTime.now());
                    logger.info("Creating new medical record for patient ID: {}", medicalRecord.getPatientId());
                }
            }

            // Extract medications
            List<RecordMedication> medications = medicalRecord.getMedications();

            // Validate medication dates
            if (medications != null) {
                for (RecordMedication medication : medications) {
                    if (medication.getStartDate() != null && medication.getEndDate() != null) {
                        if (medication.getStartDate().isAfter(medication.getEndDate())) {
                            throw new IllegalArgumentException("Medication '" + medication.getName() +
                                "' has start date that is later than end date");
                        }
                    }
                }
            }

            // If this is an existing record, first save without modifying medications to avoid orphan removal issues
            if (medicalRecord.getRecordId() != null) {
                // Load the existing record with medications to avoid orphan removal
                Optional<MedicalRecord> existingRecordWithMeds = medicalRecordService.getMedicalRecordById(medicalRecord.getRecordId());
                if (existingRecordWithMeds.isPresent()) {
                    // Preserve the existing medications to avoid orphan removal
                    medicalRecord.setMedications(existingRecordWithMeds.get().getMedications());
                }
            }

            // Save the medical record first to ensure we have an ID
            MedicalRecord savedRecord = medicalRecordService.saveMedicalRecord(medicalRecord);

            // Now handle the medications
            if (medications != null && !medications.isEmpty()) {
                // Clear existing medications and add the new ones
                if (savedRecord.getMedications() != null) {
                    savedRecord.getMedications().clear();
                } else {
                    savedRecord.setMedications(new ArrayList<>());
                }

                // Set the RecordID for each medication and add to the collection
                for (RecordMedication medication : medications) {
                    medication.setRecordId(savedRecord.getRecordId());
                    savedRecord.getMedications().add(medication);
                }

                // Save again with the updated medications
                savedRecord = medicalRecordService.saveMedicalRecord(savedRecord);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Medical record saved successfully!");
            logger.debug("Medical record saved successfully for patient ID: {}", medicalRecord.getPatientId());

            return "redirect:/doctor/medical-records?patientId=" + medicalRecord.getPatientId() + "&doctorId=" + doctorId;
        } catch (Exception e) {
            logger.error("Error saving medical record for patient ID: {}", medicalRecord.getPatientId(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save medical record: " + e.getMessage());
            return "redirect:/doctor/medical-records?patientId=" + medicalRecord.getPatientId() + "&doctorId=" + doctorId;
        }
    }

    /**
     * Get a patient's medical record history
     *
     * @param patientId The ID of the patient
     * @param doctorId The ID of the doctor
     * @param model The Spring model
     * @return The medical record history page template
     */
    @GetMapping("/history")
    public String getMedicalRecordHistory(
            @RequestParam Integer patientId,
            @RequestParam Integer doctorId,
            Model model) {
        try {
            logger.info("Fetching medical record history for patient ID: {}", patientId);

            // Fetch patient details
            Optional<Patient> patientOpt = doctorService.getPatientDetails(patientId);

            if (patientOpt.isEmpty()) {
                logger.warn("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found.");
                return "error";
            }

            Patient patient = patientOpt.get();
            model.addAttribute("patient", patient);

            // Get doctor info
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
            if (doctor != null && doctor.getUser() != null) {
                model.addAttribute("doctorName", doctor.getUser().getFullName());
            }
            model.addAttribute("doctorId", doctorId);

            // Fetch all medical records for the patient
            List<MedicalRecord> medicalRecords = medicalRecordService.getAllMedicalRecordsByPatientId(patientId);
            model.addAttribute("medicalRecords", medicalRecords);

            logger.debug("Successfully retrieved medical record history for patient ID: {}", patientId);
            return "doctor/medical-record-history";
        } catch (Exception e) {
            logger.error("Error fetching medical record history for patient ID: {}", patientId, e);
            model.addAttribute("errorMessage", "Failed to retrieve medical record history: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Generate a PDF of the medical record
     *
     * @param recordId The ID of the medical record to generate a PDF for
     * @return Redirect to the generated PDF file
     */
    @GetMapping("/export-pdf/{recordId}")
    public String exportMedicalRecordPdf(@PathVariable Integer recordId, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Generating PDF for medical record ID: {}", recordId);

            // Generate the PDF using the service
            Map<String, Object> result = pdfExportService.generateMedicalRecordPdf(recordId);

            if ((Boolean) result.get("success")) {
                logger.info("PDF generated successfully for medical record ID: {}", recordId);
                redirectAttributes.addFlashAttribute("successMessage", "Medical record PDF generated successfully!");

                // Redirect to the file download URL
                return "redirect:" + result.get("fileUrl");
            } else {
                logger.error("Error generating PDF for medical record ID: {}: {}", recordId, result.get("message"));
                redirectAttributes.addFlashAttribute("errorMessage", result.get("message"));

                // Handle the case where we can't get the medical record safely
                try {
                    Optional<MedicalRecord> record = medicalRecordService.getMedicalRecordById(recordId);
                    if (record.isPresent()) {
                        return "redirect:/doctor/medical-records?patientId=" + record.get().getPatientId();
                    }
                } catch (Exception ex) {
                    logger.error("Error retrieving patient ID for redirect after PDF generation failure", ex);
                }

                return "redirect:/doctor/dashboard";
            }
        } catch (Exception e) {
            logger.error("Error generating PDF for medical record ID: {}", recordId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to generate PDF: " + e.getMessage());
            return "redirect:/doctor/dashboard";
        }
    }
}
