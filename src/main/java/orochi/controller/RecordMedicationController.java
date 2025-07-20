package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.MedicalRecord;
import orochi.model.RecordMedication;
import orochi.service.MedicalRecordService;
import orochi.service.RecordMedicationService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/medical/medications")
public class RecordMedicationController {

    private static final Logger logger = LoggerFactory.getLogger(RecordMedicationController.class);

    @Autowired
    private RecordMedicationService medicationService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    /**
     * Display medications for a specific medical record
     */
    @GetMapping("/record/{recordId}")
    public String viewMedicationsForRecord(@PathVariable Integer recordId, Model model) {
        logger.info("Viewing medications for medical record ID: {}", recordId);

        // Get the medical record
        Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.getMedicalRecordById(recordId);
        if (medicalRecordOpt.isEmpty()) {
            logger.error("Medical record not found with ID: {}", recordId);
            return "redirect:/medical/records";
        }
        MedicalRecord medicalRecord = medicalRecordOpt.get();

        // Get current and historical medications
        List<RecordMedication> currentMedications = medicationService.getCurrentMedications(recordId);
        List<RecordMedication> historicalMedications = medicationService.getHistoricalMedications(recordId);

        model.addAttribute("medicalRecord", medicalRecord);
        model.addAttribute("currentMedications", currentMedications);
        model.addAttribute("historicalMedications", historicalMedications);
        model.addAttribute("newMedication", new RecordMedication());

        return "medical/medications";
    }

    /**
     * Display form to add a new medication
     */
    @GetMapping("/add/{recordId}")
    public String showAddMedicationForm(@PathVariable Integer recordId, Model model) {
        logger.info("Showing form to add medication for medical record ID: {}", recordId);

        Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.getMedicalRecordById(recordId);
        if (medicalRecordOpt.isEmpty()) {
            logger.error("Medical record not found with ID: {}", recordId);
            return "redirect:/medical/records";
        }

        RecordMedication newMedication = new RecordMedication();
        newMedication.setRecordId(recordId);

        model.addAttribute("medicalRecord", medicalRecordOpt.get());
        model.addAttribute("medication", newMedication);

        return "medical/medication-form";
    }

    /**
     * Handle submission of the add medication form
     */
    @PostMapping("/save")
    public String saveMedication(@ModelAttribute RecordMedication medication,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        logger.info("Saving medication for medical record ID: {}", medication.getRecordId());

        if (result.hasErrors()) {
            logger.error("Validation errors when saving medication: {}", result.getAllErrors());
            return "medical/medication-form";
        }

        try {
            medicationService.saveMedication(medication);
            redirectAttributes.addFlashAttribute("successMessage", "Medication saved successfully");
        } catch (IllegalArgumentException e) {
            logger.error("Error saving medication: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving medication: " + e.getMessage());
            return "redirect:/medical/medications/add/" + medication.getRecordId();
        }

        return "redirect:/medical/medications/record/" + medication.getRecordId();
    }

    /**
     * Display form to edit an existing medication
     */
    @GetMapping("/edit/{id}")
    public String showEditMedicationForm(@PathVariable Integer id, Model model) {
        logger.info("Showing form to edit medication ID: {}", id);

        Optional<RecordMedication> medicationOpt = medicationService.getMedicationById(id);
        if (medicationOpt.isEmpty()) {
            logger.error("Medication not found with ID: {}", id);
            return "redirect:/medical/records";
        }

        RecordMedication medication = medicationOpt.get();
        Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.getMedicalRecordById(medication.getRecordId());

        model.addAttribute("medication", medication);
        medicalRecordOpt.ifPresent(record -> model.addAttribute("medicalRecord", record));

        return "medical/medication-form";
    }

    /**
     * Delete a medication
     */
    @GetMapping("/delete/{id}")
    public String deleteMedication(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        logger.info("Deleting medication ID: {}", id);

        Optional<RecordMedication> medicationOpt = medicationService.getMedicationById(id);
        if (medicationOpt.isEmpty()) {
            logger.error("Medication not found with ID: {}", id);
            return "redirect:/medical/records";
        }

        Integer recordId = medicationOpt.get().getRecordId();

        try {
            medicationService.deleteMedication(id);
            redirectAttributes.addFlashAttribute("successMessage", "Medication deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting medication: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting medication: " + e.getMessage());
        }

        return "redirect:/medical/medications/record/" + recordId;
    }

    /**
     * API endpoint to get medications for a medical record
     */
    @GetMapping("/api/record/{recordId}")
    @ResponseBody
    public ResponseEntity<List<RecordMedication>> getMedicationsForRecord(@PathVariable Integer recordId) {
        logger.info("API request for medications of medical record ID: {}", recordId);
        List<RecordMedication> medications = medicationService.getMedicationsByRecordId(recordId);
        return ResponseEntity.ok(medications);
    }

    /**
     * API endpoint to get a specific medication
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<RecordMedication> getMedicationById(@PathVariable Integer id) {
        logger.info("API request for medication ID: {}", id);
        Optional<RecordMedication> medication = medicationService.getMedicationById(id);
        return medication
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * API endpoint to create or update a medication
     */
    @PostMapping("/api/save")
    @ResponseBody
    public ResponseEntity<?> apiSaveMedication(@RequestBody RecordMedication medication) {
        logger.info("API request to save medication for record ID: {}", medication.getRecordId());

        try {
            RecordMedication saved = medicationService.saveMedication(medication);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            logger.error("Error in API save medication: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in API save medication: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * API endpoint to delete a medication
     */
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDeleteMedication(@PathVariable Integer id) {
        logger.info("API request to delete medication ID: {}", id);

        try {
            medicationService.deleteMedication(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error in API delete medication: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting the medication: " + e.getMessage());
        }
    }
}
