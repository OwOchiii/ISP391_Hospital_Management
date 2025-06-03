package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT DISTINCT p FROM Patient p JOIN Appointment a ON p.patientId = a.patientId " +
            "WHERE a.doctorId = :doctorId")
    Page<Patient> findPatientsWithAppointmentsByDoctorIdPaginated(@Param("doctorId") Integer doctorId, Pageable pageable);

    @Query("SELECT p FROM Patient p JOIN p.user u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Patient> findByFullNameContainingIgnoreCase(@Param("name") String name);

    // Count methods for patient statuses
    // Additional custom methods if needed
    @Query("SELECT COUNT(p) FROM Patient p JOIN p.user u WHERE UPPER(u.status) = UPPER(:status)")
    long countByUserStatus(@Param("status") String status);

    @Query("SELECT DISTINCT p FROM Patient p JOIN p.user u JOIN Appointment a ON p.patientId = a.patientId " +
            "WHERE a.doctorId = :doctorId AND UPPER(u.status) = UPPER(:status)")
    Page<Patient> findByDoctorIdAndStatus(@Param("doctorId") Integer doctorId,
                                          @Param("status") String status,
                                          Pageable pageable);

    // Find all patients with a specific status
    @Query("SELECT p FROM Patient p JOIN p.user u WHERE UPPER(u.status) = UPPER(:status)")
    Page<Patient> findByStatus(@Param("status") String status, Pageable pageable);

    Optional<Patient> findByUserId(Integer userId);
}

