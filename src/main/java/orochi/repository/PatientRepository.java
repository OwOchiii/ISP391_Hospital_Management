package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Integer> {
    // Basic CRUD operations provided by JpaRepository
    @Query("SELECT DISTINCT p FROM Patient p JOIN Appointment a ON p.patientId = a.patientId " +
            "WHERE a.doctorId = :doctorId")
    List<Patient> findPatientsWithAppointmentsByDoctorId(@Param("doctorId") Integer doctorId);

    @Query("SELECT p FROM Patient p JOIN p.user u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Patient> findByFullNameContainingIgnoreCase(@Param("name") String name);
    // Additional custom methods if needed
    Optional<Patient> findByUserId(Integer userId);

    @Query("""
        SELECT p FROM Patient p JOIN p.user u
        WHERE (:search IS NULL
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:statusFilter IS NULL OR u.status = :statusFilter)
        ORDER BY p.patientId
        """)
    List<Patient> searchPatients(
            @Param("search") String search,
            @Param("statusFilter") String statusFilter
    );

}