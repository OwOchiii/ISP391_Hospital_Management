// src/main/java/orochi/repository/RoomRepository.java
package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Room;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    // Tìm theo roomNumber hoặc roomName (dùng cho search)
    @Query("SELECT r FROM Room r WHERE " +
            "LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
            "LOWER(r.roomName) LIKE LOWER(CONCAT('%', :kw, '%'))")
    List<Room> searchByNumberOrName(@Param("kw") String keyword);

    // Đếm số phòng có trạng thái “Available” (có thể dùng nếu cần)
    @Query("SELECT COUNT(r) FROM Room r WHERE r.status = :status")
    Integer countByStatus(@Param("status") String status);

    // Find rooms by department ID
    @Query("SELECT r FROM Room r WHERE r.departmentId = :departmentId AND r.status = 'Available'")
    List<Room> findByDepartmentIdAndStatusAvailable(@Param("departmentId") Integer departmentId);

    // Find all available rooms - overload method để handle null departmentId
    @Query("SELECT r FROM Room r WHERE r.status = 'Available'")
    List<Room> findAllAvailableRooms();

    // Find all rooms by department ID
    List<Room> findByDepartmentId(Integer departmentId);

    // Get rooms by doctor and specialty - implements the SQL logic from your requirements
    @Query(value = "SELECT DISTINCT " +
            "s.SpecName AS SpecializationName, " +
            "u.FullName AS DoctorName, " +
            "r.RoomID, " +
            "r.RoomNumber, " +
            "d.DeptName AS DepartmentName " +
            "FROM Specialization s " +
            "INNER JOIN DoctorSpecialization ds ON s.SpecID = ds.SpecID " +
            "INNER JOIN Doctor doc ON ds.DoctorID = doc.DoctorID " +
            "INNER JOIN Users u ON doc.UserID = u.UserID " +
            "LEFT JOIN Schedule sch ON doc.DoctorID = sch.DoctorID " +
            "LEFT JOIN Room r ON sch.RoomID = r.RoomID " +
            "LEFT JOIN Department d ON r.DepartmentID = d.DepartmentID " +
            "ORDER BY u.FullName, s.SpecName, r.RoomNumber",
            nativeQuery = true)
    List<Object[]> findRoomsByDoctorAndSpecialty(@Param("doctorId") Integer doctorId, @Param("specialtyId") Integer specialtyId);

    // Find rooms by specialty ID - FIXED: Using correct entity relationships
    @Query("SELECT DISTINCT r FROM Room r " +
           "JOIN r.department d " +
           "WHERE EXISTS (" +
           "    SELECT 1 FROM Doctor doc " +
           "    JOIN doc.specializations spec " +
           "    WHERE spec.specId = :specialtyId " +
           "    AND (d.headDoctor = doc OR " +
           "         EXISTS (SELECT 1 FROM Schedule s WHERE s.doctor = doc AND s.room = r))" +
           ") " +
           "AND r.status = 'Available' " +
           "ORDER BY r.roomNumber")
    List<Room> findRoomsBySpecialtyId(@Param("specialtyId") Integer specialtyId);

    // Alternative method using native query - FIXED
    @Query(value = "SELECT DISTINCT r.* FROM Room r " +
                   "INNER JOIN Department d ON r.DepartmentID = d.DepartmentID " +
                   "WHERE EXISTS (" +
                   "    SELECT 1 FROM Doctor doc " +
                   "    INNER JOIN DoctorSpecialization ds ON doc.DoctorID = ds.DoctorID " +
                   "    WHERE ds.SpecID = :specialtyId " +
                   "    AND (d.HeadDoctorID = doc.DoctorID OR " +
                   "         EXISTS (SELECT 1 FROM Schedule s WHERE s.DoctorID = doc.DoctorID AND s.RoomID = r.RoomID))" +
                   ") " +
                   "AND r.Status = 'Available' " +
                   "ORDER BY r.RoomNumber",
           nativeQuery = true)
    List<Room> findRoomsBySpecialtyIdNative(@Param("specialtyId") Integer specialtyId);

    // Get all rooms for doctors with specific specialty - FIXED
    @Query(value = "SELECT DISTINCT r.RoomID, r.RoomNumber, r.RoomName, r.Type, " +
                   "r.Capacity, r.Status, r.DepartmentID, r.Notes, d.DeptName " +
                   "FROM Room r " +
                   "INNER JOIN Department d ON r.DepartmentID = d.DepartmentID " +
                   "WHERE EXISTS (" +
                   "    SELECT 1 FROM Doctor doc " +
                   "    INNER JOIN DoctorSpecialization ds ON doc.DoctorID = ds.DoctorID " +
                   "    WHERE ds.SpecID = :specialtyId " +
                   "    AND (d.HeadDoctorID = doc.DoctorID OR " +
                   "         EXISTS (SELECT 1 FROM Schedule s WHERE s.DoctorID = doc.DoctorID AND s.RoomID = r.RoomID))" +
                   ") " +
                   "AND r.Status = 'Available' " +
                   "ORDER BY r.RoomNumber",
           nativeQuery = true)
    List<Object[]> findDetailedRoomsBySpecialtyId(@Param("specialtyId") Integer specialtyId);

    // Get rooms based on DepartmentID, HeadDoctorID, DoctorID, and SpecID from DoctorSpecialization table
    @Query(value = "SELECT DISTINCT " +
                   "r.RoomID, " +
                   "r.RoomNumber, " +
                   "r.RoomName, " +
                   "r.Type, " +
                   "r.Capacity, " +
                   "r.Status, " +
                   "d.DeptName AS DepartmentName, " +
                   "d.DepartmentID, " +
                   "doc.DoctorID, " +
                   "u.FullName AS DoctorName, " +
                   "s.SpecName AS SpecializationName, " +
                   "s.SpecID " +
                   "FROM Room r " +
                   "INNER JOIN Department d ON r.DepartmentID = d.DepartmentID " +
                   "INNER JOIN DoctorSpecialization ds ON ds.DoctorID = :doctorId AND ds.SpecID = :specialtyId " +
                   "INNER JOIN Doctor doc ON ds.DoctorID = doc.DoctorID " +
                   "INNER JOIN Users u ON doc.UserID = u.UserID " +
                   "INNER JOIN Specialization s ON ds.SpecID = s.SpecID " +
                   "WHERE (d.HeadDoctorID = :doctorId OR " +
                   "       EXISTS (SELECT 1 FROM Schedule sch WHERE sch.DoctorID = :doctorId AND sch.RoomID = r.RoomID)) " +
                   "AND r.Status = 'Available' " +
                   "ORDER BY r.RoomNumber",
           nativeQuery = true)
    List<Object[]> findRoomsByDoctorAndSpecialtyWithDepartment(@Param("doctorId") Integer doctorId, @Param("specialtyId") Integer specialtyId);

    // Alternative method that checks both HeadDoctor and regular Doctor assignments
    @Query(value = "SELECT DISTINCT " +
                   "r.RoomID, " +
                   "r.RoomNumber, " +
                   "r.RoomName, " +
                   "r.Type, " +
                   "r.Capacity, " +
                   "r.Status, " +
                   "d.DeptName AS DepartmentName, " +
                   "d.DepartmentID, " +
                   "doc.DoctorID, " +
                   "u.FullName AS DoctorName, " +
                   "s.SpecName AS SpecializationName, " +
                   "s.SpecID " +
                   "FROM Room r " +
                   "INNER JOIN Department d ON r.DepartmentID = d.DepartmentID " +
                   "INNER JOIN DoctorSpecialization ds ON ds.SpecID = :specialtyId AND ds.DoctorID = :doctorId " +
                   "INNER JOIN Doctor doc ON ds.DoctorID = doc.DoctorID " +
                   "INNER JOIN Users u ON doc.UserID = u.UserID " +
                   "INNER JOIN Specialization s ON ds.SpecID = s.SpecID " +
                   "WHERE (d.HeadDoctorID = :doctorId OR " +
                   "       EXISTS (SELECT 1 FROM Schedule sch WHERE sch.DoctorID = :doctorId AND sch.RoomID = r.RoomID)) " +
                   "AND r.Status = 'Available' " +
                   "ORDER BY r.RoomNumber",
           nativeQuery = true)
    List<Object[]> findRoomsByDoctorSpecialtyAndDepartment(@Param("doctorId") Integer doctorId, @Param("specialtyId") Integer specialtyId);
}
