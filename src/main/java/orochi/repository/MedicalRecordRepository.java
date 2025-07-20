package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.MedicalRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Integer> {

    /**
     * Find a medical record by patient ID
     * @param patientId The patient's ID
     * @return The medical record if found
     */
    Optional<MedicalRecord> findByPatientId(Integer patientId);

    /**
     * Find all medical records for a specific patient
     * @param patientId The patient's ID
     * @return List of medical records for the patient
     */
    List<MedicalRecord> findAllByPatientId(Integer patientId);

    /**
     * Find all medical records created by a specific doctor
     * @param doctorId The doctor's ID
     * @return List of medical records created by the doctor
     */
    List<MedicalRecord> findAllByDoctorId(Integer doctorId);

    /**
     * Find medical records for a specific patient created by a specific doctor
     * @param patientId The patient's ID
     * @param doctorId The doctor's ID
     * @return List of medical records matching the criteria
     */
    List<MedicalRecord> findByPatientIdAndDoctorId(Integer patientId, Integer doctorId);
}
