package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import orochi.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    Page<Feedback> findByUserId(Integer userId, Pageable pageable);
    Page<Feedback> findByUserIdAndFeedbackType(Integer userId, String feedbackType, Pageable pageable);
    Page<Feedback> findByUserIdAndCreatedAtBetween(Integer userId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<Feedback> findByUserIdAndFeedbackTypeAndCreatedAtBetween(Integer userId, String feedbackType, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Feedback> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String titleKeyword,
            String descriptionKeyword,
            Pageable pageable
    );

    Page<Feedback> findByFeedbackType(String feedbackType, Pageable pageable);

    Page<Feedback> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Feedback> findByFeedbackTypeAndCreatedAtBetween(
            String feedbackType, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Đếm tất cả feedback
    long count();

    // Đếm feedback trong khoảng createdAt giữa start và end
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}