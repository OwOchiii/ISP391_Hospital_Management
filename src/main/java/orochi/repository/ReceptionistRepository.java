package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Users;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReceptionistRepository extends JpaRepository<Users, Integer> {

    // Fetch all Receptionists (RoleID = 3)
    @Query("SELECT u FROM Users u WHERE u.roleId = 3")
    List<Users> findAllReceptionists();

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

    @Query(value = """
        SELECT CAST(DAY(created_at) AS VARCHAR), COUNT(*)
        FROM Users
        WHERE RoleID = 4 AND MONTH(created_at) = MONTH(GETDATE()) AND YEAR(created_at) = YEAR(GETDATE())
        GROUP BY DAY(created_at)
        ORDER BY DAY(created_at)
        """, nativeQuery = true)
    List<Object[]> getPatientStatsByDay();

    // Group by month (trong n?m hi?n t?i)
    @Query(value = """
        SELECT FORMAT(created_at, 'MMM') AS month_name, COUNT(*)
        FROM Users
        WHERE RoleID = 4 AND YEAR(created_at) = YEAR(GETDATE())
        GROUP BY FORMAT(created_at, 'MMM'), MONTH(created_at)
        ORDER BY MONTH(created_at)
        """, nativeQuery = true)
    List<Object[]> getPatientStatsByMonth();

    // Group by year (5 n?m g?n nh?t)
    @Query(value = """
        SELECT CAST(YEAR(created_at) AS VARCHAR), COUNT(*)
        FROM Users
        WHERE RoleID = 4 AND YEAR(created_at) >= YEAR(GETDATE()) - 4
        GROUP BY YEAR(created_at)
        ORDER BY YEAR(created_at)
        """, nativeQuery = true)
    List<Object[]> getPatientStatsByYear();


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
