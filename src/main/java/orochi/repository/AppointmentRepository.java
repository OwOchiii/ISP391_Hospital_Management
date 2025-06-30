package orochi.repository;

import com.itextpdf.awt.PdfGraphics2D;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du WHERE a.doctorId = :doctorId ORDER BY a.dateTime DESC")
    List<Appointment> findByDoctorIdOrderByDateTimeDesc(@Param("doctorId") Integer doctorId);


    // Get today's appointments for a doctor
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND CAST(a.dateTime AS LocalDate) = CURRENT_DATE ORDER BY a.dateTime ASC")
    List<Appointment> findTodayAppointmentsForDoctor(@Param("doctorId") Integer doctorId);

    // Get today's appointments for a doctor with specific time range
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.dateTime BETWEEN :startOfDay AND :endOfDay ORDER BY a.dateTime ASC")
    List<Appointment> findTodayAppointmentsForDoctorWithTimeRange(@Param("doctorId") Integer doctorId,
                                                                  @Param("startOfDay") LocalDateTime startOfDay,
                                                                  @Param("endOfDay") LocalDateTime endOfDay);

    // Get upcoming appointments for a doctor
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.dateTime > :now ORDER BY a.dateTime ASC")
    List<Appointment> findUpcomingAppointmentsForDoctor(@Param("doctorId") Integer doctorId, @Param("now") LocalDateTime now);

    // Get an appointment by id and doctor id (for security)
    Appointment findByAppointmentIdAndDoctorId(Integer appointmentId, Integer doctorId);

    //Get appointments by doctor id and status
    List<Appointment> findByDoctorIdAndStatusOrderByDateTimeDesc(Integer doctorId, String status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.dateTime BETWEEN :startDate AND :endDate")
    long countByDateTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Sửa: Sử dụng Appointment.AppointmentStatus
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status")
    long countByStatus(@Param("status") String status);

    // Get appointments by patient for a specific doctor
    List<Appointment> findByPatientIdAndDoctorIdOrderByDateTimeDesc(Integer patientId, Integer doctorId);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du WHERE a.doctorId = :doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')) ORDER BY a.dateTime DESC")
    List<Appointment> findByDoctorIdAndPatientUserFullNameContainingIgnoreCase(@Param("doctorId") Integer doctorId, @Param("fullName") String fullName);

    Long countByPatientIdAndStatusIn(Integer patientId, List<String> statuses);

    Page<Appointment> findByPatientId(Integer patientId, Pageable pageable);

    Page<Appointment> findByPatientIdAndDoctorId(Integer patientId, Integer doctorId, Pageable pageable);

    <T> Page<Appointment> findByPatientIdAndDoctorId(Integer patientId, Integer doctorId, List<T> list, Pageable pageable);


    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du WHERE a.status = :status ORDER BY a.dateTime DESC")
    Page<Appointment> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du ORDER BY a.dateTime DESC")
    Page<Appointment> findAllWithDetails(Pageable pageable);

    Page<Appointment> findByStatusOrderByDateTimeDesc(String status, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.dateTime BETWEEN :start AND :end AND a.appointmentId != :appointmentId ORDER BY a.dateTime")
    List<Appointment> findByDoctorIdAndDateTimeBetweenAndAppointmentIdNotOrderByDateTime(
            @Param("doctorId") Integer doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("appointmentId") Integer appointmentId);

    @Query(value = "SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND CAST(a.dateTime AS DATE) = :date")
    List<Appointment> findByDoctorIdAndDate(@Param("doctorId") Integer doctorId, @Param("date") LocalDate date);

    // Find patient appointment history before a specific date with pagination
    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.dateTime < :dateTime ORDER BY a.dateTime DESC")
    Page<Appointment> findByPatientIdAndDateTimeBefore(@Param("patientId") Integer patientId,
                                                       @Param("dateTime") LocalDateTime dateTime,
                                                       Pageable pageable);

    // Find appointments by doctor and date range ordered by date time
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.dateTime BETWEEN :startDate AND :endDate ORDER BY a.dateTime ASC")
    List<Appointment> findByDoctorIdAndDateTimeBetweenOrderByDateTime(@Param("doctorId") Integer doctorId,
                                                                      @Param("startDate") LocalDateTime startDate,
                                                                      @Param("endDate") LocalDateTime endDate);

    // Count appointments by patient ID and doctor ID
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patientId = :patientId AND a.doctorId = :doctorId")
    long countByPatientIdAndDoctorId(@Param("patientId") Integer patientId, @Param("doctorId") Integer doctorId);

    // Count upcoming appointments by patient ID and doctor ID after a specific date
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patientId = :patientId AND a.doctorId = :doctorId AND a.dateTime > :dateTime")
    Long countByPatientIdAndDoctorIdAndDateTimeAfter(@Param("patientId") Integer patientId, 
                                                     @Param("doctorId") Integer doctorId, 
                                                     @Param("dateTime") LocalDateTime dateTime);

    // Find the most recent appointment for a patient with a doctor before a specific date
    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.doctorId = :doctorId AND a.dateTime < :dateTime ORDER BY a.dateTime DESC")
    Optional<Appointment> findTopByPatientIdAndDoctorIdAndDateTimeBeforeOrderByDateTimeDesc(@Param("patientId") Integer patientId,
                                                                                            @Param("doctorId") Integer doctorId,
                                                                                            @Param("dateTime") LocalDateTime dateTime);

    Long countByPatientId(Integer patientId);

    Long countByPatientIdAndDateTimeAfter(Integer patientId, LocalDateTime now);

    Optional<Appointment> findTopByPatientIdAndDateTimeBeforeOrderByDateTimeDesc(Integer patientId, LocalDateTime now);

    Page<Appointment> findByPatientIdAndDoctorIdAndStatusOrderByDateTimeDesc(Integer patientId, Integer doctorId, String completed, Pageable pageableNoSort);

    Page<Appointment> findByPatientIdAndDoctorIdAndStatusInOrderByDateTimeDesc(Integer patientId, Integer doctorId, List<String> list, Pageable pageableNoSort);

    Page<Appointment> findByPatientIdAndDateTimeAfterOrderByDateTimeAsc(Integer patientId, LocalDateTime now, Pageable pageableNoSort);

    Page<Appointment> findByPatientIdAndStatusOrderByDateTimeDesc(Integer patientId, String completed, Pageable pageableNoSort);

    Page<Appointment> findByPatientIdAndStatusInOrderByDateTimeDesc(Integer patientId, List<String> list, Pageable pageableNoSort);

    Page<Appointment> findByPatientIdAndDoctorIdAndDateTimeAfterOrderByDateTimeAsc(Integer patientId, Integer doctorId, LocalDateTime now, Pageable pageableNoSort);

    long countByPatientIdAndStatus(Integer patientId, String completed);

    List<Appointment> findByDoctorIdAndDateTimeBetween(Integer doctorId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    List<Appointment> findByPatientIdOrderByDateTimeDesc(Integer patientId);

    List<Appointment> findByPatientIdAndDateTimeAfterOrderByDateTime(Integer patientId, LocalDateTime now);
}
