package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.RecordMedication;

import java.util.List;

@Repository
public interface RecordMedicationRepository extends JpaRepository<RecordMedication, Integer> {

    /**
     * Find all medications associated with a specific medical record
     * @param recordId The ID of the medical record
     * @return List of medications for the medical record
     */
    List<RecordMedication> findByRecordId(Integer recordId);
}
