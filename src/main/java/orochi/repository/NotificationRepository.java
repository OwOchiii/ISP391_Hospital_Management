package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orochi.model.Notification;

import org.springframework.data.domain.Pageable;
import orochi.model.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    /**
     * Find all notifications for a specific user, ordered by creation date (newest first)
     * @param userId The ID of the user to find notifications for
     * @return List of notifications ordered by creation date in descending order
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<Notification> findByUserId(Integer userId);

    @Query("SELECT n FROM Notification n WHERE n.notificationId = :notificationId AND n.userId = :userId")
    Optional<Notification> findByNotificationIdAndUserId(Integer notificationId, Integer userId);

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
  SELECT n FROM Notification n
   WHERE (:search IS NULL 
          OR LOWER(n.message) LIKE LOWER(CONCAT('%',:search,'%'))
          OR CAST(n.notificationId AS string) LIKE CONCAT('%',:search,'%'))
     AND (:type IS NULL OR n.type = :type)
     AND (:isRead IS NULL OR n.isRead = :isRead)
     AND (:fromDate IS NULL OR n.createdAt >= :fromDate)
     AND (:toDate IS NULL OR n.createdAt <= :toDate)
""")
    Page<Notification> findByFilter(
            @Param("search") String search,
            @Param("type") NotificationType type,
            @Param("isRead") Boolean isRead,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    // New method to count unread notifications
    int countByUserIdAndIsReadFalse(Integer userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
            "AND (:isRead IS NULL OR n.isRead = :isRead) " +
            "AND (:searchTerm IS NULL OR LOWER(n.type) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR CONCAT('', n.notificationId) LIKE CONCAT('%', :searchTerm, '%')) " +
            "AND (:fromDate IS NULL OR n.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR n.createdAt <= :toDate) " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findFilteredNotifications(
            @Param("userId") Integer userId,
            @Param("isRead") Boolean isRead,
            @Param("searchTerm") String searchTerm,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    long countByUserId(Integer userId);

    long countByUserIdAndIsRead(Integer userId, boolean isRead);
}
