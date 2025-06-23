// src/main/java/orochi/service/DepartmentService.java
package orochi.service;

import orochi.model.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentService {
    /**
     * Lấy về tất cả phòng ban
     */
    List<Department> getAllDepartments();

    /**
     * Tìm một phòng ban theo ID
     */
    Optional<Department> findById(Integer id);
}
