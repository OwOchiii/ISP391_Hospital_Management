package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Patient;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Integer> {
    // Basic CRUD operations provided by JpaRepository
    @Query("SELECT DISTINCT p FROM Patient p JOIN Appointment a ON p.patientId = a.patientId " +
            "WHERE a.doctorId = :doctorId")
    List<Patient> findPatientsWithAppointmentsByDoctorId(@Param("doctorId") Integer doctorId);

    List<Patient> findByFullNameContainingIgnoreCase(String name);
    // Additional custom methods if needed
}