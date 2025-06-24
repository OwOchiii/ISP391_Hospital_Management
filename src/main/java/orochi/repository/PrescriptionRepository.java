package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orochi.model.Prescription;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {
    List<Prescription> findByPatientIdAndPrescriptionDateAfter(Integer patientId, LocalDateTime date);

    //List<Prescription> findByAppointmentId(Integer appointmentId);

    List<Prescription> findByPatientId(Integer patientId);

    List<Prescription> findByDoctorId(Integer doctorId);

    List<Prescription> findByPatientIdOrderByPrescriptionDateDesc(Integer patientId);

    @Query("SELECT p FROM Prescription p " +
            "LEFT JOIN FETCH p.patient pat " +
            "LEFT JOIN FETCH pat.user u " +
            "LEFT JOIN FETCH p.doctor d " +
            "LEFT JOIN FETCH d.user du " +
            "LEFT JOIN FETCH p.medicines m " +
            "LEFT JOIN FETCH m.inventory mi " +
            "WHERE p.appointmentId = :appointmentId")
    List<Prescription> findByAppointmentId(@Param("appointmentId") Integer appointmentId);
}