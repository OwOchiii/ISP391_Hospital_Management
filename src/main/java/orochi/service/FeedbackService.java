package orochi.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import orochi.model.Feedback;
import orochi.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback saveFeedback(Feedback feedback) {
        feedback.setCreatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    public Page<Feedback> getFeedbackByUserId(Integer userId, Pageable pageable) {
        return feedbackRepository.findByUserId(userId, pageable);
    }

    public Page<Feedback> getFeedbackByUserIdAndDateRange(Integer userId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return feedbackRepository.findByUserIdAndCreatedAtBetween(userId, start, end, pageable);
    }

    public void deleteFeedback(Integer feedbackId) {
        feedbackRepository.deleteById(feedbackId);
    }
}