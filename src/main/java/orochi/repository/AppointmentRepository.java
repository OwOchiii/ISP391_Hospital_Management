package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    // Get all appointments for a doctor
    List<Appointment> findByDoctorIdOrderByDateTimeDesc(Integer doctorId);

    // Get today's appointments for a doctor
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND CAST(a.dateTime AS LocalDate) = CURRENT_DATE ORDER BY a.dateTime ASC")
    List<Appointment> findTodayAppointmentsForDoctor(@Param("doctorId") Integer doctorId);

    // Get upcoming appointments for a doctor
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.dateTime > :now ORDER BY a.dateTime ASC")
    List<Appointment> findUpcomingAppointmentsForDoctor(@Param("doctorId") Integer doctorId, @Param("now") LocalDateTime now);

    // Get an appointment by id and doctor id (for security)
    Appointment findByAppointmentIdAndDoctorId(Integer appointmentId, Integer doctorId);

    //Get appointments by doctor id and status
    List<Appointment> findByDoctorIdAndStatusOrderByDateTimeDesc(Integer doctorId, String status);

    // Get appointments by patient for a specific doctor
    List<Appointment> findByPatientIdAndDoctorIdOrderByDateTimeDesc(Integer patientId, Integer doctorId);

    // In AppointmentRepository.java
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.dateTime BETWEEN :startDate AND :endDate ORDER BY a.dateTime")
    List<Appointment> findByDoctorIdAndDateTimeBetweenOrderByDateTime(
            @Param("doctorId") Integer doctorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    List<Appointment> findByDoctorIdAndPatientUserFullNameContainingIgnoreCase(Integer doctorId, String fullName);

    List<Appointment> findByPatientIdAndDateTimeAfterOrderByDateTime(Integer patientId, LocalDateTime now);
    List<Appointment> findByPatientIdOrderByDateTimeDesc(Integer patientId);

    // Count appointments for a patient with a specific doctor
    Long countByPatientIdAndDoctorId(Integer patientId, Integer doctorId);

    // Count upcoming appointments for a patient with a specific doctor
    Long countByPatientIdAndDoctorIdAndDateTimeAfter(Integer patientId, Integer doctorId, LocalDateTime dateTime);

    // Find the most recent appointment for a patient with a specific doctor
    Optional<Appointment> findTopByPatientIdAndDoctorIdAndDateTimeBeforeOrderByDateTimeDesc(
            Integer patientId, Integer doctorId, LocalDateTime dateTime);

    // Count all appointments for a patient (regardless of doctor)
    Long countByPatientId(Integer patientId);

    // Count upcoming appointments for a patient (regardless of doctor)
    Long countByPatientIdAndDateTimeAfter(Integer patientId, LocalDateTime dateTime);

    // Find the most recent appointment for a patient (regardless of doctor)
    Optional<Appointment> findTopByPatientIdAndDateTimeBeforeOrderByDateTimeDesc(
            Integer patientId, LocalDateTime dateTime);

    Page<Appointment> findByPatientIdAndDateTimeBefore(
            Integer patientId,
            LocalDateTime dateTime,
            Pageable pageable);

    // In AppointmentRepository.java, add these methods:

    // For doctor-specific appointments
    Page<Appointment> findByPatientIdAndDoctorIdAndDateTimeAfterOrderByDateTimeAsc(
        Integer patientId, Integer doctorId, LocalDateTime dateTime, Pageable pageable);

    Page<Appointment> findByPatientIdAndDoctorIdAndStatusOrderByDateTimeDesc(
        Integer patientId, Integer doctorId, String status, Pageable pageable);

    Page<Appointment> findByPatientIdAndDoctorIdAndStatusInOrderByDateTimeDesc(
        Integer patientId, Integer doctorId, List<String> statuses, Pageable pageable);

    Page<Appointment> findByPatientIdAndDoctorIdOrderByDateTimeDesc(
        Integer patientId, Integer doctorId, Pageable pageable);

    // For patient-specific appointments (across all doctors)
    Page<Appointment> findByPatientIdAndDateTimeAfterOrderByDateTimeAsc(
        Integer patientId, LocalDateTime dateTime, Pageable pageable);

    Page<Appointment> findByPatientIdAndStatusOrderByDateTimeDesc(
        Integer patientId, String status, Pageable pageable);

    Page<Appointment> findByPatientIdAndStatusInOrderByDateTimeDesc(
        Integer patientId, List<String> statuses, Pageable pageable);

    // For counting appointments
    Long countByPatientIdAndStatus(Integer patientId, String status);

    Long countByPatientIdAndStatusIn(Integer patientId, List<String> statuses);
    Page<Appointment> findByPatientId(Integer patientId, Pageable pageable);
    Page<Appointment> findByPatientIdAndDoctorId(Integer patientId, Integer doctorId, Pageable pageable);

    <T> Page<Appointment> findByPatientIdAndDoctorId(Integer patientId, Integer doctorId, List<T> list, Pageable pageable);
}



