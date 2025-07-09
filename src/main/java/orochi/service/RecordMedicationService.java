package orochi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.MedicalRecord;
import orochi.model.RecordMedication;
import orochi.repository.MedicalRecordRepository;
import orochi.repository.RecordMedicationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RecordMedicationService {

    private static final Logger logger = LoggerFactory.getLogger(RecordMedicationService.class);

    @Autowired
    private RecordMedicationRepository medicationRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    /**
     * Get all medications in the system
     * @return List of all medications
     */
    public List<RecordMedication> getAllMedications() {
        logger.info("Fetching all medication records");
        return medicationRepository.findAll();
    }

    /**
     * Get a medication by its ID
     * @param id The ID of the medication
     * @return The medication if found, otherwise empty
     */
    public Optional<RecordMedication> getMedicationById(Integer id) {
        logger.info("Fetching medication record with ID: {}", id);
        return medicationRepository.findById(id);
    }

    /**
     * Get all medications for a specific medical record
     * @param recordId The ID of the medical record
     * @return List of medications for the record
     */
    public List<RecordMedication> getMedicationsByRecordId(Integer recordId) {
        logger.info("Fetching medications for medical record ID: {}", recordId);
        return medicationRepository.findByRecordId(recordId);
    }

    /**
     * Save a new medication or update an existing one
     * @param medication The medication to save
     * @return The saved medication
     */
    @Transactional
    public RecordMedication saveMedication(RecordMedication medication) {
        if (medication.getMedicationId() == null) {
            logger.info("Creating new medication record for medical record ID: {}", medication.getRecordId());
        } else {
            logger.info("Updating medication record ID: {}", medication.getMedicationId());
        }

        // Verify the medical record exists
        Optional<MedicalRecord> record = medicalRecordRepository.findById(medication.getRecordId());
        if (record.isEmpty()) {
            logger.error("Cannot save medication: Medical record with ID {} not found", medication.getRecordId());
            throw new IllegalArgumentException("Medical record not found");
        }

        // Basic validation
        if (medication.getName() == null || medication.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Medication name cannot be empty");
        }
        if (medication.getDosage() == null || medication.getDosage().trim().isEmpty()) {
            throw new IllegalArgumentException("Medication dosage cannot be empty");
        }
        if (medication.getFrequency() == null || medication.getFrequency().trim().isEmpty()) {
            throw new IllegalArgumentException("Medication frequency cannot be empty");
        }
        if (medication.getStartDate() == null) {
            throw new IllegalArgumentException("Medication start date cannot be empty");
        }

        // Validate end date is after start date if provided
        if (medication.getEndDate() != null && medication.getEndDate().isBefore(medication.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        return medicationRepository.save(medication);
    }

    /**
     * Delete a medication by ID
     * @param id The ID of the medication to delete
     */
    @Transactional
    public void deleteMedication(Integer id) {
        logger.info("Deleting medication record with ID: {}", id);
        medicationRepository.deleteById(id);
    }

    /**
     * Get all current medications (those with no end date or end date in the future)
     * @param recordId The ID of the medical record
     * @return List of current medications
     */
    public List<RecordMedication> getCurrentMedications(Integer recordId) {
        logger.info("Fetching current medications for medical record ID: {}", recordId);
        LocalDate today = LocalDate.now();
        return medicationRepository.findByRecordId(recordId).stream()
                .filter(med -> med.getEndDate() == null || !med.getEndDate().isBefore(today))
                .toList();
    }

    /**
     * Get all historical medications (those with end date in the past)
     * @param recordId The ID of the medical record
     * @return List of historical medications
     */
    public List<RecordMedication> getHistoricalMedications(Integer recordId) {
        logger.info("Fetching historical medications for medical record ID: {}", recordId);
        LocalDate today = LocalDate.now();
        return medicationRepository.findByRecordId(recordId).stream()
                .filter(med -> med.getEndDate() != null && med.getEndDate().isBefore(today))
                .toList();
    }
}
