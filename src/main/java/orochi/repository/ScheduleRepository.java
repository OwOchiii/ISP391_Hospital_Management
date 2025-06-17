package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orochi.model.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    // Find schedules by doctor ID
    List<Schedule> findByDoctorId(Integer doctorId);

    // Find schedules by doctor ID and date range
    List<Schedule> findByDoctorIdAndScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(
            Integer doctorId, LocalDate startDate, LocalDate endDate);

    // Find schedules by doctor ID and specific date
    List<Schedule> findByDoctorIdAndScheduleDateOrderByStartTimeAsc(
            Integer doctorId, LocalDate date);

    // Find schedules by patient ID (new column)
    List<Schedule> findByPatientId(Integer patientId);

    // Find schedule by appointment ID (new column)
    Optional<Schedule> findByAppointmentId(Integer appointmentId);

    // Count appointments for a doctor in a date range (using AppointmentID)
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.doctorId = :doctorId AND s.appointmentId IS NOT NULL AND s.scheduleDate BETWEEN :startDate AND :endDate")
    Integer countAppointmentsInDateRange(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Calculate total on-call hours for a doctor in a date range (using EventType)
    @Query(value = "SELECT COALESCE(SUM(DATEDIFF(MINUTE, CONCAT(ScheduleDate, ' ', startTime), CONCAT(ScheduleDate, ' ', endTime)) / 60.0), 0) FROM Schedule " +
            "WHERE DoctorID = :doctorId AND EventType = 'oncall' AND ScheduleDate BETWEEN :startDate AND :endDate", nativeQuery = true)
    Integer sumOnCallHoursInDateRange(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Count distinct rooms assigned to a doctor in a date range
    @Query("SELECT COUNT(DISTINCT s.roomId) FROM Schedule s WHERE s.doctorId = :doctorId AND s.scheduleDate BETWEEN :startDate AND :endDate")
    Integer countDistinctRoomsInDateRange(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Calculate total working hours for a doctor in a date range
    @Query(value = "SELECT COALESCE(SUM(DATEDIFF(MINUTE, CONCAT(ScheduleDate, ' ', startTime), CONCAT(ScheduleDate, ' ', endTime)) / 60.0), 0) FROM Schedule " +
            "WHERE DoctorID = :doctorId AND ScheduleDate BETWEEN :startDate AND :endDate", nativeQuery = true)
    Integer sumTotalHoursInDateRange(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Search schedules by keyword and date range (using Description)
    @Query("SELECT s FROM Schedule s WHERE " +
            "(LOWER(s.eventType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT d FROM Doctor d JOIN d.user u WHERE d.doctorId = s.doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT p FROM Patient p JOIN p.user u WHERE p.patientId = s.patientId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT r FROM Room r WHERE r.roomId = s.roomId AND LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "AND s.scheduleDate BETWEEN :startDate AND :endDate " +
            "ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByKeywordAndDateRange(@Param("keyword") String keyword, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Search schedules by date range only
    @Query("SELECT s FROM Schedule s WHERE s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Search schedules by keyword and specific date (or all dates if date is null)
    @Query("SELECT s FROM Schedule s WHERE " +
            "(LOWER(s.eventType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT d FROM Doctor d JOIN d.user u WHERE d.doctorId = s.doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT p FROM Patient p JOIN p.user u WHERE p.patientId = s.patientId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT r FROM Room r WHERE r.roomId = s.roomId AND LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "AND (:date IS NULL OR s.scheduleDate = :date) " +
            "ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByKeywordAndDate(@Param("keyword") String keyword, @Param("date") LocalDate date);
}