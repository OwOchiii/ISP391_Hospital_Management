package orochi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.MedicalRecord;
import orochi.repository.MedicalRecordRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MedicalRecordService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordService.class);

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    /**
     * Get a medical record by ID
     * @param recordId The medical record ID
     * @return The medical record if found
     */
    public Optional<MedicalRecord> getMedicalRecordById(Integer recordId) {
        logger.info("Fetching medical record with ID: {}", recordId);
        return medicalRecordRepository.findById(recordId);
    }

    /**
     * Get the most recent medical record for a specific patient
     * @param patientId The patient's ID
     * @return The most recent medical record if found
     */
    public Optional<MedicalRecord> getMedicalRecordByPatientId(Integer patientId) {
        logger.info("Fetching most recent medical record for patient ID: {}", patientId);
        List<MedicalRecord> records = medicalRecordRepository.findAllByPatientId(patientId);

        if (records.isEmpty()) {
            return Optional.empty();
        }

        // Sort by lastUpdated descending to get the most recent record
        records.sort((r1, r2) -> r2.getLastUpdated().compareTo(r1.getLastUpdated()));
        return Optional.of(records.get(0));
    }

    /**
     * Get all medical records for a specific patient
     * @param patientId The patient's ID
     * @return List of medical records for the patient
     */
    public List<MedicalRecord> getAllMedicalRecordsByPatientId(Integer patientId) {
        logger.info("Fetching all medical records for patient ID: {}", patientId);
        return medicalRecordRepository.findAllByPatientId(patientId);
    }

    /**
     * Get all medical records created by a specific doctor
     * @param doctorId The doctor's ID
     * @return List of medical records created by the doctor
     */
    public List<MedicalRecord> getAllMedicalRecordsByDoctorId(Integer doctorId) {
        logger.info("Fetching all medical records created by doctor ID: {}", doctorId);
        return medicalRecordRepository.findAllByDoctorId(doctorId);
    }

    /**
     * Get medical records for a specific patient created by a specific doctor
     * @param patientId The patient's ID
     * @param doctorId The doctor's ID
     * @return List of medical records matching the criteria
     */
    public List<MedicalRecord> getMedicalRecordsByPatientAndDoctor(Integer patientId, Integer doctorId) {
        logger.info("Fetching medical records for patient ID: {} created by doctor ID: {}", patientId, doctorId);
        return medicalRecordRepository.findByPatientIdAndDoctorId(patientId, doctorId);
    }

    /**
     * Create or update a medical record
     * @param medicalRecord The medical record to save
     * @return The saved medical record
     */
    @Transactional
    public MedicalRecord saveMedicalRecord(MedicalRecord medicalRecord) {
        logger.info("Saving medical record for patient ID: {}", medicalRecord.getPatientId());

        // Set the updated timestamp
        medicalRecord.setLastUpdated(LocalDateTime.now());

        // If it's a new record, set the record date
        if (medicalRecord.getRecordId() == null) {
            medicalRecord.setRecordDate(LocalDateTime.now());
        }

        return medicalRecordRepository.save(medicalRecord);
    }

    /**
     * Delete a medical record
     * @param recordId The ID of the medical record to delete
     */
    @Transactional
    public void deleteMedicalRecord(Integer recordId) {
        logger.info("Deleting medical record with ID: {}", recordId);
        medicalRecordRepository.deleteById(recordId);
    }
}
