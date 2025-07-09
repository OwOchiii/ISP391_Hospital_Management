package orochi.repository;

import orochi.model.Appointment;
import com.itextpdf.awt.PdfGraphics2D;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    // =====================================================================
    // Doctor-specific queries
    // =====================================================================

    /** 1) Tất cả appointments của bác sĩ, có join fetch details */
    @Query("""
      SELECT a
        FROM Appointment a
        JOIN FETCH a.patient p
        JOIN FETCH p.user pu
        JOIN FETCH a.doctor d
        JOIN FETCH d.user du
       WHERE d.doctorId = :doctorId
       ORDER BY a.dateTime DESC
    """)
    List<Appointment> findByDoctorIdOrderByDateTimeDesc(@Param("doctorId") Integer doctorId);

    /** 2) Lịch hôm nay của bác sĩ (dạng đơn giản) */
    @Query(value = """
      SELECT a.*
        FROM Appointment a
       WHERE a.DoctorID = :doctorId
        AND CONVERT(DATE, a.DateTime) = CONVERT(DATE, GETDATE())
      ORDER BY a.DateTime ASC
    """, nativeQuery = true)
    List<Appointment> findTodayAppointmentsForDoctor(@Param("doctorId") Integer doctorId);

    /** 3) Lịch hôm nay với khoảng thời gian */
    @Query("""
      SELECT a
        FROM Appointment a
       WHERE a.doctorId = :doctorId
         AND a.dateTime BETWEEN :startOfDay AND :endOfDay
       ORDER BY a.dateTime ASC
    """)
    List<Appointment> findTodayAppointmentsForDoctorWithTimeRange(
            @Param("doctorId")   Integer doctorId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay")   LocalDateTime endOfDay
    );

    /** 4) Các cuộc hẹn sắp tới */
    @Query("""
      SELECT a
        FROM Appointment a
       WHERE a.doctorId = :doctorId
         AND a.dateTime > :now
       ORDER BY a.dateTime ASC
    """)
    List<Appointment> findUpcomingAppointmentsForDoctor(
            @Param("doctorId") Integer doctorId,
            @Param("now")      LocalDateTime now
    );

    /** 5) Tìm theo tên bệnh nhân (dùng trong searchAppointmentsByDoctorId) */
    @Query("""
      SELECT a
        FROM Appointment a
        JOIN a.patient p
        JOIN p.user u
       WHERE a.doctorId = :doctorId
         AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
       ORDER BY a.dateTime DESC
    """)
    List<Appointment> findByDoctorIdAndPatientUserFullNameContainingIgnoreCase(
            @Param("doctorId") Integer doctorId,
            @Param("search")   String search
    );

    /** 6) Lấy tất cả slots trong ngày (không phân biệt status) */
    @Query("""
      SELECT a
        FROM Appointment a
       WHERE a.doctorId = :doctorId
         AND a.dateTime BETWEEN :start AND :end
       ORDER BY a.dateTime
    """)
    List<Appointment> findByDoctorIdAndDateTimeBetweenOrderByDateTime(
            @Param("doctorId") Integer doctorId,
            @Param("start")    LocalDateTime start,
            @Param("end")      LocalDateTime end
    );

    /** 7) Lấy tất cả slots trong ngày, ngoại trừ appointmentId cho case update */
    @Query("""
      SELECT a
        FROM Appointment a
       WHERE a.doctorId = :doctorId
         AND a.dateTime BETWEEN :start AND :end
         AND a.appointmentId <> :excludeId
       ORDER BY a.dateTime
    """)
    List<Appointment> findByDoctorIdAndDateTimeBetweenAndAppointmentIdNotOrderByDateTime(
            @Param("doctorId") Integer doctorId,
            @Param("start")    LocalDateTime start,
            @Param("end")      LocalDateTime end,
            @Param("excludeId")Integer excludeId
    );


    // =====================================================================
    // Status & counting
    // =====================================================================

    Appointment findByAppointmentIdAndDoctorId(Integer appointmentId, Integer doctorId);

    List<Appointment> findByDoctorIdAndStatusOrderByDateTimeDesc(Integer doctorId, String status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.dateTime BETWEEN :start AND :end")
    long countByDateTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status")
    long countByStatus(@Param("status") String status);


    // =====================================================================
    // Patient-specific queries (giữ nguyên)
    // =====================================================================

    List<Appointment> findByPatientIdAndDoctorIdOrderByDateTimeDesc(Integer patientId, Integer doctorId);

    List<Appointment> findByPatientIdAndDateTimeAfterOrderByDateTime(Integer patientId, LocalDateTime now);

    List<Appointment> findByPatientIdOrderByDateTimeDesc(Integer patientId);

    Long countByPatientIdAndDoctorId(Integer patientId, Integer doctorId);

    Long countByPatientIdAndDoctorIdAndDateTimeAfter(Integer patientId, Integer doctorId, LocalDateTime now);

    Optional<Appointment> findTopByPatientIdAndDoctorIdAndDateTimeBeforeOrderByDateTimeDesc(
            Integer patientId, Integer doctorId, LocalDateTime now
    );

    Long countByPatientId(Integer patientId);

    Long countByPatientIdAndDateTimeAfter(Integer patientId, LocalDateTime now);

    Optional<Appointment> findTopByPatientIdAndDateTimeBeforeOrderByDateTimeDesc(
            Integer patientId, LocalDateTime now
    );

    Page<Appointment> findByPatientIdAndDateTimeBefore(
            Integer patientId, LocalDateTime now, Pageable pageable
    );

    Page<Appointment> findByPatientIdAndDoctorIdAndDateTimeAfterOrderByDateTimeAsc(
            Integer patientId, Integer doctorId, LocalDateTime now, Pageable pageable
    );

    Page<Appointment> findByPatientIdAndDoctorIdAndStatusOrderByDateTimeDesc(
            Integer patientId, Integer doctorId, String status, Pageable pageable
    );

    Page<Appointment> findByPatientIdAndDoctorIdAndStatusInOrderByDateTimeDesc(
            Integer patientId, Integer doctorId, List<String> statuses, Pageable pageable
    );

    Page<Appointment> findByPatientIdAndDoctorIdOrderByDateTimeDesc(
            Integer patientId, Integer doctorId, Pageable pageable
    );

    Page<Appointment> findByPatientIdAndDateTimeAfterOrderByDateTimeAsc(
            Integer patientId, LocalDateTime now, Pageable pageable
    );

    Page<Appointment> findByPatientIdAndStatusOrderByDateTimeDesc(
            Integer patientId, String status, Pageable pageable
    );

    Page<Appointment> findByPatientIdAndStatusInOrderByDateTimeDesc(
            Integer patientId, List<String> statuses, Pageable pageable
    );

    Long countByPatientIdAndStatus(Integer patientId, String status);

    Long countByPatientIdAndStatusIn(Integer patientId, List<String> statuses);

    Page<Appointment> findByPatientId(Integer patientId, Pageable pageable);

    Page<Appointment> findByPatientIdAndDoctorId(
            Integer patientId, Integer doctorId, Pageable pageable
    );

    <T> Page<Appointment> findByPatientIdAndDoctorId(
            Integer patientId, Integer doctorId, List<T> list, Pageable pageable
    );


    // =====================================================================
    // With‐details, dynamic filtering & statistics (giữ nguyên)
    // =====================================================================

    @Query("""
      SELECT a
        FROM Appointment a
        JOIN FETCH a.patient p
        JOIN FETCH p.user pu
        JOIN FETCH a.doctor d
        JOIN FETCH d.user du
       WHERE a.status = :status
       ORDER BY a.dateTime DESC
    """)
    Page<Appointment> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("""
      SELECT a
        FROM Appointment a
        JOIN FETCH a.patient p
        JOIN FETCH p.user pu
        JOIN FETCH a.doctor d
        JOIN FETCH d.user du
       ORDER BY a.dateTime DESC
    """)
    Page<Appointment> findAllWithDetails(Pageable pageable);

    Page<Appointment> findByStatusOrderByDateTimeDesc(String status, Pageable pageable);

    @Query("""
      SELECT DISTINCT a
        FROM Appointment a
        JOIN a.doctor d
        LEFT JOIN d.specializations s
        JOIN a.patient p
        JOIN p.user pu
       WHERE (:doctorId IS NULL OR d.doctorId = :doctorId)
         AND (:specId   IS NULL OR s.specId  = :specId)
         AND (:status   = 'ALL' OR a.status = :status)
         AND (:search   IS NULL OR LOWER(pu.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')))
         AND a.dateTime BETWEEN :start AND :end
       ORDER BY a.dateTime DESC
    """)
    Page<Appointment> findWithFilters(
            @Param("doctorId") Integer doctorId,
            @Param("specId")   Integer specId,
            @Param("status")   String status,
            @Param("search")   String search,
            @Param("start")    LocalDateTime start,
            @Param("end")      LocalDateTime end,
            Pageable pageable
    );

    @Query(value = """
      SELECT CONVERT(varchar(7), a.DateTime, 120) AS period, COUNT(*) AS cnt
        FROM Appointment a
       WHERE a.DateTime BETWEEN :start AND :end
         AND (:status = 'ALL' OR a.Status = :status)
       GROUP BY CONVERT(varchar(7), a.DateTime, 120)
       ORDER BY period
    """, nativeQuery = true)
    List<Object[]> findMonthlyCounts(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end,
            @Param("status") String status
    );

    @Query(value = """
  SELECT CONCAT(CAST(YEAR(a.DateTime) AS varchar(4)), '-Q', CAST(DATEPART(QUARTER,a.DateTime) AS varchar(1))) AS period, COUNT(*) AS cnt
  FROM Appointment a
  WHERE a.DateTime BETWEEN :start AND :end
    AND a.Status = :status
  GROUP BY YEAR(a.DateTime), DATEPART(QUARTER,a.DateTime)
  ORDER BY YEAR(a.DateTime), DATEPART(QUARTER,a.DateTime)
""", nativeQuery = true)
    List<Object[]> findQuarterlyCountsByStatus(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end,
            @Param("status") String status
    );

    @Query("SELECT a FROM Appointment a " +
            "JOIN a.doctor d " +
            "JOIN d.user u " +
            "WHERE a.patientId = :patientId " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:fromDate IS NULL OR a.dateTime >= :fromDate) " +
            "AND (:toDate IS NULL OR a.dateTime <= :toDate)")
    Page<Appointment> findByPatientIdWithFilters(
            @Param("patientId") Integer patientId,
            @Param("search") String search,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);


    @Query(value = """
      SELECT CONCAT(CAST(YEAR(a.DateTime) AS varchar(4)), '-Q', CAST(DATEPART(QUARTER,a.DateTime) AS varchar(1))) AS period, COUNT(*) AS cnt
      FROM Appointment a
      WHERE a.DateTime BETWEEN :start AND :end
        AND a.Status = :status
      GROUP BY YEAR(a.DateTime), DATEPART(QUARTER,a.DateTime)
      ORDER BY YEAR(a.DateTime), DATEPART(QUARTER,a.DateTime)
    """, nativeQuery = true)
    List<Object[]> findQuarterlyCounts(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end,
            @Param("status") String status
    );

    @Query("""
      SELECT FUNCTION('YEAR', a.dateTime) AS period, COUNT(a) AS cnt
        FROM Appointment a
       WHERE a.dateTime BETWEEN :start AND :end
         AND (:status = 'ALL' OR a.status = :status)
       GROUP BY FUNCTION('YEAR', a.dateTime)
       ORDER BY FUNCTION('YEAR', a.dateTime)
    """)
    List<Object[]> findYearlyCounts(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end,
            @Param("status") String status
    );


    @Query("""
      SELECT FUNCTION('YEAR', a.dateTime) AS period, COUNT(a) AS cnt
        FROM Appointment a
       WHERE a.dateTime BETWEEN :start AND :end
         AND a.status = :status
       GROUP BY FUNCTION('YEAR', a.dateTime)
       ORDER BY FUNCTION('YEAR', a.dateTime)
    """)
    List<Object[]> findYearlyCountsByStatus(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end,
            @Param("status") String status
    );


    @Query(value = "SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND CAST(a.dateTime AS DATE) = :date")
    List<Appointment> findByDoctorIdAndDate(@Param("doctorId") Integer doctorId, @Param("date") LocalDate date);


    @Query("SELECT a FROM Appointment a WHERE a.dateTime BETWEEN :startDate AND :endDate ORDER BY a.dateTime ASC")
    List<Appointment> findByDateTimeBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    List<Appointment> findByDoctorIdAndDateTimeBetween(Integer doctorId, LocalDateTime startOfDay, LocalDateTime endOfDay);

}
