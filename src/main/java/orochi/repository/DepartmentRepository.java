package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    // Find department by ID
    Optional<Department> findByDepartmentId(Integer departmentId);

    // Find department by name
    Optional<Department> findByDeptName(String deptName);

    // Find departments led by a specific doctor
    List<Department> findByHeadDoctorId(Integer headDoctorId);

    // Find departments that have medical orders assigned to them
    @Query("SELECT DISTINCT d FROM Department d JOIN d.medicalOrders mo WHERE mo.status = :status")
    List<Department> findDepartmentsWithOrdersByStatus(@Param("status") String status);

    // Find departments with their medical orders count
    @Query("SELECT d, COUNT(mo) FROM Department d LEFT JOIN d.medicalOrders mo GROUP BY d")
    List<Object[]> findDepartmentsWithOrderCount();

    // Search departments by name containing the given string
    List<Department> findByDeptNameContainingIgnoreCase(String name);

    // Paginated version
    Page<Department> findByDeptNameContainingIgnoreCase(String name, Pageable pageable);

    // Find all departments sorted by name
    List<Department> findAllByOrderByDeptNameAsc();

    // Count medical orders assigned to a department
    @Query("SELECT COUNT(mo) FROM MedicalOrder mo WHERE mo.assignedToDeptId = :departmentId")
    long countMedicalOrdersByDepartmentId(@Param("departmentId") Integer departmentId);

    // Count medical orders assigned to a department with specific status
    @Query("SELECT COUNT(mo) FROM MedicalOrder mo WHERE mo.assignedToDeptId = :departmentId AND mo.status = :status")
    long countMedicalOrdersByDepartmentIdAndStatus(
            @Param("departmentId") Integer departmentId,
            @Param("status") String status);
}