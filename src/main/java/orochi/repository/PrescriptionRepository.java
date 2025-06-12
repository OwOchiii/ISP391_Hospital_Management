package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.Prescription;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {
    List<Prescription> findByPatientIdAndPrescriptionDateAfter(Integer patientId, LocalDateTime date);

    List<Prescription> findByAppointmentId(Integer appointmentId);

    List<Prescription> findByPatientId(Integer patientId);

    List<Prescription> findByDoctorId(Integer doctorId);

    List<Prescription> findByPatientIdOrderByPrescriptionDateDesc(Integer patientId);
}