package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import orochi.model.Users;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Integer>, JpaSpecificationExecutor<Users> {

    // Basic CRUD operations provided by JpaRepository
    Optional<Users> findByEmail(String email);

    // Check phone number exist in the database
    boolean existsByPhoneNumber(String phoneNumber);

    List<Users> findByRoleId(Integer roleId);

    // Count users created between dates
    @Query("SELECT COUNT(u) FROM Users u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    Integer countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // Count active users
    @Query("SELECT COUNT(u) FROM Users u WHERE u.isGuest = :active")
    Integer countByIsGuest(@Param("active") boolean active);

    @Query("SELECT COUNT(u) > 0 FROM Users u WHERE u.phoneNumber = :phoneNumber AND u.userId != :userId")
    boolean existsByPhoneNumberAndUserIdNot(@Param("phoneNumber") String phoneNumber, @Param("userId") Integer userId);

    @Query("SELECT u FROM Users u WHERE u.roleId = :roleId")
    Page<Users> findAllByRoleId(@Param("roleId") Integer roleId, Pageable pageable);
}