package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orochi.model.Notification;
import orochi.repository.NotificationRepository;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Get all notifications for a specific user
     * @param userId The ID of the user to get notifications for
     * @return List of notifications for the user
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getNotificationsByUserId(@RequestParam Integer userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Mark a notification as read
     * @param notificationId The ID of the notification to mark as read
     * @return ResponseEntity with status 200 if successful, 404 if notification not found
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Integer notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
