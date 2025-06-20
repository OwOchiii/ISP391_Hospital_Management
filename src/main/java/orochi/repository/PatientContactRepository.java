package orochi.repository;

import orochi.model.PatientContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientContactRepository extends JpaRepository<PatientContact, Integer> {
    // Find all contacts for a specific patient by PatientID
    List<PatientContact> findByPatientId(Integer patientId);

    // Find the first contact for a specific patient by PatientID (useful for primary address)
    Optional<PatientContact> findFirstByPatientId(Integer patientId);

    List<PatientContact> findByPatientIdOrderByContactIdAsc(Integer patientId);

    // Additional methods for PatientContact can be defined here if needed
}
