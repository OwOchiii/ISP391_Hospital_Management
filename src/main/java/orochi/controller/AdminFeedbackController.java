package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import orochi.model.Feedback;
import orochi.service.FeedbackService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/feedbacks")
public class AdminFeedbackController {

    private static final List<String> ALLOWED_FEEDBACK_TYPES = Arrays.asList(
            "Quality of medical services",
            "Facilities",
            "Administrative procedures",
            "Online booking & application system",
            "Staff attitude and behavior",
            "Costs & payment",
            "Suggestions & improvements",
            "Timing & progress",
            "Safety & security",
            "Other"
    );

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    public String list(
            @RequestParam("adminId")     Integer adminId,
            @RequestParam(value="keyword",      required=false) String  keyword,
            @RequestParam(value="userId",       required=false) Integer userId,
            @RequestParam(value="feedbackType", required=false) String  feedbackType,
            @RequestParam(value="start",        required=false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(value="end",          required=false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(value="page", defaultValue="0")   int page,
            @RequestParam(value="size", defaultValue="7")   int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean hasKeyword = keyword    != null && !keyword.isBlank();
        boolean hasType    = feedbackType != null && !feedbackType.isBlank();
        boolean hasPeriod  = start      != null && end != null;

        Page<Feedback> feedbackPage;
        if (hasKeyword) {
            feedbackPage = feedbackService.searchFeedback(keyword, pageable);
        }
        else if (userId != null && hasType && hasPeriod) {
            feedbackPage = feedbackService
                    .getFeedbackByUserIdAndTypeAndDateRange(userId, feedbackType, start, end, pageable);
        }
        else if (userId != null && hasType) {
            feedbackPage = feedbackService.getFeedbackByUserIdAndType(userId, feedbackType, pageable);
        }
        else if (userId != null && hasPeriod) {
            feedbackPage = feedbackService.getFeedbackByUserIdAndDateRange(userId, start, end, pageable);
        }
        else if (userId != null) {
            feedbackPage = feedbackService.getFeedbackByUserId(userId, pageable);
        }
        else if (hasType && hasPeriod) {
            feedbackPage = feedbackService.getFeedbackByTypeAndDateRange(feedbackType, start, end, pageable);
        }
        else if (hasType) {
            feedbackPage = feedbackService.getFeedbackByType(feedbackType, pageable);
        }
        else if (hasPeriod) {
            feedbackPage = feedbackService.getFeedbackByDateRange(start, end, pageable);
        }
        else {
            feedbackPage = feedbackService.getAllFeedback(pageable);
        }

        model.addAttribute("adminId",       adminId);
        model.addAttribute("keyword",       keyword);
        model.addAttribute("userId",        userId);
        model.addAttribute("feedbackType",  feedbackType);
        model.addAttribute("start",         start);
        model.addAttribute("end",           end);
        model.addAttribute("feedbackPage",  feedbackPage);
        model.addAttribute("feedbackTypes", ALLOWED_FEEDBACK_TYPES);

        return "admin/feedbackList";
    }
}
