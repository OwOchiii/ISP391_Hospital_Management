package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import orochi.model.PatientContact;

import java.util.List;

public interface PatientContactRepository extends JpaRepository<PatientContact, Integer> {
    List<PatientContact> findByPatientIdOrderByContactIdAsc(Integer patientId);

    // Additional methods for PatientContact can be defined here if needed
}
