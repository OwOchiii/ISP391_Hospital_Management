package orochi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Notification;
import orochi.model.NotificationType;
import orochi.repository.NotificationRepository;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
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

    public Page<Notification> findPageForAdmin(int pageNumber) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                7,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public Notification saveAndReturn(Notification notification) {
        if (notification.getNotificationId() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        logger.debug("Saving (and returning) notification ID: {}, isRead: {}",
                notification.getNotificationId(),
                notification.isRead());
        return notificationRepository.save(notification);
    }

    public Page<Notification> searchAndFilter(
            String search, NotificationType type,
            Boolean isRead, LocalDateTime fromDate, LocalDateTime toDate,
            int pageNumber
    ) {
        Pageable pg = PageRequest.of(pageNumber, 7, Sort.by("createdAt").descending());
        return notificationRepository.findByFilter(
                search == null || search.isBlank() ? null : search,
                type,
                isRead,
                fromDate,
                toDate,
                pg
        );
    }

    // New method to count unread notifications
    public int countUnreadNotifications(Integer userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}