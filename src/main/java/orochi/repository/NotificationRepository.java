package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    /**
     * Find all notifications for a specific user, ordered by creation date (newest first)
     * @param userId The ID of the user to find notifications for
     * @return List of notifications ordered by creation date in descending order
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
