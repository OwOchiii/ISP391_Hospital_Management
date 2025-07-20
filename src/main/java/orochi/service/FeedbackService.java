package orochi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Feedback;
import orochi.repository.FeedbackRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackService.class);

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Transactional
    public Feedback saveFeedback(Feedback feedback) {
        try {
            feedback.setCreatedAt(LocalDateTime.now());
            Feedback savedFeedback = feedbackRepository.save(feedback);
            logger.info("Feedback saved successfully for user ID: {}", feedback.getUserId());
            return savedFeedback;
        } catch (Exception e) {
            logger.error("Error saving feedback for user ID: {}", feedback.getUserId(), e);
            throw new RuntimeException("Failed to save feedback: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbackByUserId(Integer userId, Pageable pageable) {
        try {
            return feedbackRepository.findByUserId(userId, pageable);
        } catch (Exception e) {
            logger.error("Error retrieving feedback for user ID: {}", userId, e);
            throw new RuntimeException("Failed to retrieve feedback: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbackByUserIdAndType(Integer userId, String feedbackType, Pageable pageable) {
        try {
            return feedbackRepository.findByUserIdAndFeedbackType(userId, feedbackType, pageable);
        } catch (Exception e) {
            logger.error("Error retrieving feedback for user ID: {} and type: {}", userId, feedbackType, e);
            throw new RuntimeException("Failed to retrieve feedback: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbackByUserIdAndDateRange(Integer userId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        try {
            return feedbackRepository.findByUserIdAndCreatedAtBetween(userId, start, end, pageable);
        } catch (Exception e) {
            logger.error("Error retrieving feedback for user ID: {} between {} and {}", userId, start, end, e);
            throw new RuntimeException("Failed to retrieve feedback: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbackByUserIdAndTypeAndDateRange(Integer userId, String feedbackType, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        try {
            return feedbackRepository.findByUserIdAndFeedbackTypeAndCreatedAtBetween(userId, feedbackType, start, end, pageable);
        } catch (Exception e) {
            logger.error("Error retrieving feedback for user ID: {}, type: {}, between {} and {}", userId, feedbackType, start, end, e);
            throw new RuntimeException("Failed to retrieve feedback: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteFeedback(Integer feedbackId) {
        try {
            Optional<Feedback> feedbackOpt = feedbackRepository.findById(feedbackId);
            if (feedbackOpt.isPresent()) {
                feedbackRepository.deleteById(feedbackId);
                logger.info("Feedback ID: {} deleted successfully", feedbackId);
            } else {
                logger.warn("Feedback ID: {} not found for deletion", feedbackId);
                throw new RuntimeException("Feedback not found for ID: " + feedbackId);
            }
        } catch (Exception e) {
            logger.error("Error deleting feedback ID: {}", feedbackId, e);
            throw new RuntimeException("Failed to delete feedback: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Feedback> getAllFeedback(Pageable pageable) {
        try {
            return feedbackRepository.findAll(pageable);
        } catch (Exception e) {
            logger.error("Error retrieving all feedback", e);
            throw new RuntimeException("Failed to retrieve feedback: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Feedback> searchFeedback(String keyword, Pageable pageable) {
        return feedbackRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable);
    }

    // Lọc theo type
    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbackByType(String type, Pageable pageable) {
        return feedbackRepository.findByFeedbackType(type, pageable);
    }

    // Lọc theo date-range
    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbackByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return feedbackRepository.findByCreatedAtBetween(start, end, pageable);
    }

    // Lọc kết hợp type + date-range
    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbackByTypeAndDateRange(
            String type, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return feedbackRepository.findByFeedbackTypeAndCreatedAtBetween(type, start, end, pageable);
    }

    @Transactional(readOnly = true)
    public long countAllFeedback() {
        return feedbackRepository.count();
    }

    /** Thống kê feedback giữa hai mốc thời gian */
    @Transactional(readOnly = true)
    public long countFeedbackBetween(LocalDateTime start, LocalDateTime end) {
        return feedbackRepository.countByCreatedAtBetween(start, end);
    }
}