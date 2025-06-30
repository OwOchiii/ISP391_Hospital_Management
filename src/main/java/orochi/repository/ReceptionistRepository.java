package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Users;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReceptionistRepository extends JpaRepository<Users, Integer> {

    // Fetch all Receptionists (RoleID = 3) with pagination and filtering
    @Query("SELECT u FROM Users u WHERE u.roleId = 3 " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:statusFilter IS NULL OR u.status = :statusFilter)")
    Page<Users> findAllReceptionistsFiltered(
            @Param("search") String search,
            @Param("statusFilter") String statusFilter,
            Pageable pageable);

    // Fetch a Receptionist by email for login purposes
    @Query("SELECT u FROM Users u WHERE u.email = :email AND u.roleId = 3")
    Optional<Users> findByEmail(@Param("email") String email);

    // Check if a phone number exists among Receptionists
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Users u WHERE u.phoneNumber = :phoneNumber AND u.roleId = 3")
    boolean existsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    // Count Receptionists by status
    @Query("SELECT COUNT(u) FROM Users u WHERE u.roleId = 3 AND u.status = :status")
    Integer countByStatus(@Param("status") String status);

    @Query(value = "SELECT COUNT(*) FROM Appointment WHERE CAST(DateTime AS date) = CAST(GETDATE() AS date)", nativeQuery = true)
    Integer totalAppointments();

    @Query("SELECT COUNT(u) FROM Users u WHERE u.roleId = 2")
    Integer ourDoctors();

    @Query("SELECT COUNT(u) FROM Users u WHERE u.roleId = 3")
    Integer activeStaff();

    @Query(value = """
        SELECT COUNT(*)
        FROM Users
        WHERE roleId = 4
        AND CONVERT(DATE, created_at) = CONVERT(DATE, GETDATE())
        """, nativeQuery = true)
    Integer newPatients();

    // Patient statistics by day for current month - shows all days including those with 0 patients
    @Query(value = """
        WITH DaysInMonth AS (
            SELECT 1 as day_num
            UNION ALL
            SELECT day_num + 1
            FROM DaysInMonth
            WHERE day_num < DAY(EOMONTH(GETDATE()))
        )
        SELECT 
            CAST(d.day_num AS VARCHAR) as day_label,
            ISNULL(COUNT(u.UserID), 0) as patient_count
        FROM DaysInMonth d
        LEFT JOIN Users u ON DAY(u.created_at) = d.day_num 
            AND MONTH(u.created_at) = MONTH(GETDATE()) 
            AND YEAR(u.created_at) = YEAR(GETDATE())
            AND u.RoleID = 4
        GROUP BY d.day_num
        ORDER BY d.day_num
        """, nativeQuery = true)
    List<Object[]> getPatientStatsByDay();

    @Query(value = """
        SELECT
                 u.FullName AS name,
                 p.gender as gender,
                 u.PhoneNUmber as phone,
                 a.AppointmentID as id,
                 CONVERT(date, a.DateTime) AS date,
                 CONVERT(time, a.DateTime) AS time,
                 a.Status AS status
             FROM Appointment a
             JOIN Users u ON a.PatientID = u.UserID
             Join Patient p on p.UserID = u.UserID
             WHERE a.Status = 'Pending'
                 AND CONVERT(date, a.DateTime) = CONVERT(date, GETDATE())
             ORDER BY a.DateTime DESC
        """, nativeQuery = true)
    List<Map<String, Object>> fetchAppointmentTableData();

    @Modifying
    @Query(value = """
        update Appointment set Status = 'Scheduled' where AppointmentID = :appId
        """, nativeQuery = true)
    int confirmAppointment(@Param("appId") Integer id);

}
