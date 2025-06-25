package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import orochi.model.Users;

import java.util.Optional;

public interface ReceptionistRepository extends JpaRepository<Users, Integer> {

    // Fetch all Receptionists (RoleID = 3) with pagination and filtering
    @Query("""
        SELECT u
          FROM Users u
         WHERE u.roleId = 3
           AND (
               (:search IS NULL OR :search = '')
               AND (:statusFilter IS NULL OR u.status = :statusFilter)
               OR (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
           )
        """)
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
}