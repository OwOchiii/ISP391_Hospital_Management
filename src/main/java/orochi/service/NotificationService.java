package orochi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Notification;
import orochi.repository.NotificationRepository;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    public List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId) {
        logger.debug("Fetching notifications for user ID: {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Notification findByIdAndUserId(Integer notificationId, Integer userId) {
        logger.debug("Fetching notification ID: {} for user ID: {}", notificationId, userId);
        return notificationRepository.findByNotificationIdAndUserId(notificationId, userId).orElse(null);
    }

    public Optional<Notification> findById(Integer notificationId) {
        logger.debug("Fetching notification by ID: {}", notificationId);
        return notificationRepository.findById(notificationId);
    }

    @Transactional
    public void save(Notification notification) {
        logger.debug("Saving notification ID: {}, IsRead: {}", notification.getNotificationId(), notification.isRead());
        notificationRepository.save(notification);
        logger.debug("Saved notification ID: {}", notification.getNotificationId());
    }
}