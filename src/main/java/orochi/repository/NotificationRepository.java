package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orochi.model.Notification;

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
}
