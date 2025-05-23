package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;

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

    // Get appointments by patient for a specific doctor
    List<Appointment> findByPatientIdAndDoctorIdOrderByDateTimeDesc(Integer patientId, Integer doctorId);
}