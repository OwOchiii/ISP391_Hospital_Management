package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orochi.model.Schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    // Existing methods (unchanged)
    List<Schedule> findByDoctorId(Integer doctorId);

    List<Schedule> findByDoctorIdAndScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(
            Integer doctorId, LocalDate startDate, LocalDate endDate);

    List<Schedule> findByDoctorIdAndScheduleDateOrderByStartTimeAsc(
            Integer doctorId, LocalDate date);

    List<Schedule> findByPatientId(Integer patientId);

    Optional<Schedule> findByAppointmentId(Integer appointmentId);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.doctorId = :doctorId AND s.eventType = 'appointment' AND s.scheduleDate BETWEEN :startDate AND :endDate")
    Integer countAppointmentsInDateRange(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT COALESCE(SUM(DATEDIFF(MINUTE, CONCAT(ScheduleDate, ' ', startTime), CONCAT(ScheduleDate, ' ', endTime)) / 60.0), 0) FROM Schedule " +
            "WHERE DoctorID = :doctorId AND EventType = 'oncall' AND ScheduleDate BETWEEN :startDate AND :endDate", nativeQuery = true)
    Integer sumOnCallHoursInDateRange(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(DISTINCT s.roomId) FROM Schedule s WHERE s.doctorId = :doctorId AND s.scheduleDate BETWEEN :startDate AND :endDate")
    Integer countDistinctRoomsInDateRange(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT COALESCE(SUM(DATEDIFF(MINUTE, CONCAT(ScheduleDate, ' ', startTime), CONCAT(ScheduleDate, ' ', endTime)) / 60.0), 0) FROM Schedule " +
            "WHERE DoctorID = :doctorId AND ScheduleDate BETWEEN :startDate AND :endDate", nativeQuery = true)
    Integer sumTotalHoursInDateRange(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE " +
            "(LOWER(s.eventType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT d FROM Doctor d JOIN d.user u WHERE d.doctorId = s.doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT p FROM Patient p JOIN p.user u WHERE p.patientId = s.patientId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT r FROM Room r WHERE r.roomId = s.roomId AND LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "AND s.scheduleDate BETWEEN :startDate AND :endDate " +
            "ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByKeywordAndDateRange(@Param("keyword") String keyword, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE " +
            "(LOWER(s.eventType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT d FROM Doctor d JOIN d.user u WHERE d.doctorId = s.doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT p FROM Patient p JOIN p.user u WHERE p.patientId = s.patientId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT r FROM Room r WHERE r.roomId = s.roomId AND LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "AND (:date IS NULL OR s.scheduleDate = :date) " +
            "ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByKeywordAndDate(@Param("keyword") String keyword, @Param("date") LocalDate date);

    List<Schedule> findByDoctorIdAndEventType(Integer doctorId, String eventType);

    List<Schedule> findByDoctorIdAndIsCompleted(Integer doctorId, Boolean isCompleted);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.doctorId = :doctorId AND s.isCompleted = true AND s.scheduleDate BETWEEN :startDate AND :endDate")
    Integer countCompletedSchedulesInDateRange(@Param("doctorId") Integer doctorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // New pagination methods
    @Query("SELECT s FROM Schedule s WHERE " +
            "(LOWER(s.eventType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT d FROM Doctor d JOIN d.user u WHERE d.doctorId = s.doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT p FROM Patient p JOIN p.user u WHERE p.patientId = s.patientId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT r FROM Room r WHERE r.roomId = s.roomId AND LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "AND s.scheduleDate BETWEEN :startDate AND :endDate " +
            "ORDER BY s.scheduleDate, s.startTime")
    Page<Schedule> findByKeywordAndDateRangePaginated(@Param("keyword") String keyword, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    Page<Schedule> findByDateRangePaginated(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE " +
            "(LOWER(s.eventType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT d FROM Doctor d JOIN d.user u WHERE d.doctorId = s.doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT p FROM Patient p JOIN p.user u WHERE p.patientId = s.patientId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT r FROM Room r WHERE r.roomId = s.roomId AND LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "AND (:date IS NULL OR s.scheduleDate = :date) " +
            "ORDER BY s.scheduleDate, s.startTime")
    Page<Schedule> findByKeywordAndDatePaginated(@Param("keyword") String keyword, @Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE " +
            "(LOWER(s.eventType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT d FROM Doctor d JOIN d.user u WHERE d.doctorId = s.doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT p FROM Patient p JOIN p.user u WHERE p.patientId = s.patientId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT r FROM Room r WHERE r.roomId = s.roomId AND LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "AND s.scheduleDate BETWEEN :startDate AND :endDate")
    long countByKeywordAndDateRange(@Param("keyword") String keyword, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.scheduleDate BETWEEN :startDate AND :endDate")
    long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE " +
            "(LOWER(s.eventType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT d FROM Doctor d JOIN d.user u WHERE d.doctorId = s.doctorId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT p FROM Patient p JOIN p.user u WHERE p.patientId = s.patientId AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "EXISTS (SELECT r FROM Room r WHERE r.roomId = s.roomId AND LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "AND (:date IS NULL OR s.scheduleDate = :date)")
    long countByKeywordAndDate(@Param("keyword") String keyword, @Param("date") LocalDate date);

    // New methods for advanced filtering
    @Query("SELECT s FROM Schedule s WHERE s.scheduleId = :scheduleId")
    Optional<Schedule> findByScheduleId(@Param("scheduleId") Integer scheduleId);

    @Query("SELECT s FROM Schedule s WHERE s.eventType = :eventType AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByEventTypeAndDateRange(@Param("eventType") String eventType, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE s.roomId = :roomId AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByRoomIdAndDateRange(@Param("roomId") Integer roomId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE s.startTime = :startTime AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByStartTimeAndDateRange(@Param("startTime") LocalTime startTime, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE s.endTime = :endTime AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    List<Schedule> findByEndTimeAndDateRange(@Param("endTime") LocalTime endTime, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE s.scheduleId = :scheduleId AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    Page<Schedule> findByScheduleIdAndDateRangePaginated(@Param("scheduleId") Integer scheduleId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.eventType = :eventType AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    Page<Schedule> findByEventTypeAndDateRangePaginated(@Param("eventType") String eventType, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.roomId = :roomId AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    Page<Schedule> findByRoomIdAndDateRangePaginated(@Param("roomId") Integer roomId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.startTime = :startTime AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    Page<Schedule> findByStartTimeAndDateRangePaginated(@Param("startTime") LocalTime startTime, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.endTime = :endTime AND s.scheduleDate BETWEEN :startDate AND :endDate ORDER BY s.scheduleDate, s.startTime")
    Page<Schedule> findByEndTimeAndDateRangePaginated(@Param("endTime") LocalTime endTime, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.scheduleId = :scheduleId AND s.scheduleDate BETWEEN :startDate AND :endDate")
    long countByScheduleIdAndDateRange(@Param("scheduleId") Integer scheduleId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.eventType = :eventType AND s.scheduleDate BETWEEN :startDate AND :endDate")
    long countByEventTypeAndDateRange(@Param("eventType") String eventType, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.roomId = :roomId AND s.scheduleDate BETWEEN :startDate AND :endDate")
    long countByRoomIdAndDateRange(@Param("roomId") Integer roomId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.startTime = :startTime AND s.scheduleDate BETWEEN :startDate AND :endDate")
    long countByStartTimeAndDateRange(@Param("startTime") LocalTime startTime, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.endTime = :endTime AND s.scheduleDate BETWEEN :startDate AND :endDate")
    long countByEndTimeAndDateRange(@Param("endTime") LocalTime endTime, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}