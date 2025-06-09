package orochi.repository;

import orochi.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du WHERE a.doctorId = :doctorId ORDER BY a.dateTime DESC")
    List<Appointment> findByDoctorIdOrderByDateTimeDesc(@Param("doctorId") Integer doctorId);

    // Sửa: Sử dụng Appointment.AppointmentStatus thay vì String
    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du WHERE a.doctorId = :doctorId AND a.status = :status ORDER BY a.dateTime DESC")
    List<Appointment> findByDoctorIdAndStatusOrderByDateTimeDesc(@Param("doctorId") Integer doctorId, @Param("status") Appointment.AppointmentStatus status);

    // Sửa: Sử dụng Appointment.AppointmentStatus
    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du WHERE a.status = :status ORDER BY a.dateTime DESC")
    Page<Appointment> findByStatus(@Param("status") Appointment.AppointmentStatus status, Pageable pageable);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du ORDER BY a.dateTime DESC")
    Page<Appointment> findAllWithDetails(Pageable pageable);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du WHERE a.patientId = :patientId AND a.dateTime > :now ORDER BY a.dateTime")
    List<Appointment> findByPatientIdAndDateTimeAfterOrderByDateTime(@Param("patientId") Integer patientId, @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du WHERE a.patientId = :patientId ORDER BY a.dateTime DESC")
    List<Appointment> findByPatientIdOrderByDateTimeDesc(@Param("patientId") Integer patientId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.dateTime BETWEEN :startDate AND :endDate")
    long countByDateTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Sửa: Sử dụng Appointment.AppointmentStatus
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status")
    long countByStatus(@Param("status") Appointment.AppointmentStatus status);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient p JOIN FETCH p.user u JOIN FETCH a.doctor d JOIN FETCH d.user du WHERE a.doctorId = :doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')) ORDER BY a.dateTime DESC")
    List<Appointment> findByDoctorIdAndPatientUserFullNameContainingIgnoreCase(@Param("doctorId") Integer doctorId, @Param("fullName") String fullName);

    // Thêm: Phương thức bị thiếu trong AppointmentService
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.dateTime BETWEEN :startDate AND :endDate ORDER BY a.dateTime")
    List<Appointment> findByDoctorIdAndDateTimeBetweenOrderByDateTime(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
