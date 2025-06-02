package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.Prescription;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {
    List<Prescription> findByPatientIdAndPrescriptionDateAfter(Integer patientId, LocalDateTime date);
}