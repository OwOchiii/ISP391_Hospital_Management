package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.MedicalResult;

import java.util.List;

public interface MedicalResultRepository extends JpaRepository<MedicalResult, Integer> {

    // Find results by appointment ID
    List<MedicalResult> findByAppointmentIdOrderByResultDateDesc(Integer appointmentId);

    // Paginated version
    Page<MedicalResult> findByAppointmentId(Integer appointmentId, Pageable pageable);

    // Find results by doctor ID
    List<MedicalResult> findByDoctorIdOrderByResultDateDesc(Integer doctorId);

    // Paginated version
    Page<MedicalResult> findByDoctorId(Integer doctorId, Pageable pageable);

    // Find results for a specific medical order
    @Query("SELECT r FROM MedicalResult r JOIN MedicalOrder o ON r.resultId = o.resultId WHERE o.orderId = :orderId")
    MedicalResult findByOrderId(@Param("orderId") Integer orderId);

    // Find all pending results that need to be reviewed by doctors
    List<MedicalResult> findByStatusOrderByResultDateDesc(String status);

    // Paginated version
    Page<MedicalResult> findByStatus(String status, Pageable pageable);

    // Find pending results for a specific department (through related orders)
    @Query("SELECT r FROM MedicalResult r JOIN MedicalOrder o ON r.resultId = o.resultId " +
           "WHERE o.assignedToDeptId = :departmentId AND r.status = :status " +
           "ORDER BY r.resultDate DESC")
    List<MedicalResult> findByDepartmentIdAndStatus(
            @Param("departmentId") Integer departmentId,
            @Param("status") String status);

    // Paginated version
    @Query("SELECT r FROM MedicalResult r JOIN MedicalOrder o ON r.resultId = o.resultId " +
           "WHERE o.assignedToDeptId = :departmentId AND r.status = :status")
    Page<MedicalResult> findByDepartmentIdAndStatus(
            @Param("departmentId") Integer departmentId,
            @Param("status") String status,
            Pageable pageable);
}
