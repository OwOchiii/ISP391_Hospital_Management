package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Users;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Integer> {
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
}

