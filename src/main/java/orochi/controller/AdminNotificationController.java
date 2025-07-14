package orochi.controller;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.Notification;
import orochi.model.NotificationType;
import orochi.model.Users;
import orochi.service.NotificationService;
import orochi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;

    // Inject service để lấy danh sách user
    @Autowired
    private UserService userService;

    /** 1. List với pagination, filter như trước */
    @GetMapping
    public String list(
            @RequestParam(value = "search",   required = false) String search,
            @RequestParam(value = "type",     required = false) NotificationType type,
            @RequestParam(value = "isRead",   required = false) Boolean isRead,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(value = "toDate",   required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model
    ) {
        Page<Notification> pageData = notificationService.searchAndFilter(
                (search != null && !search.isBlank()) ? search.trim() : null,
                type, isRead, fromDate, toDate, page
        );
        model.addAttribute("pageData",     pageData);
        model.addAttribute("search",       search);
        model.addAttribute("typeFilter",   type);
        model.addAttribute("isReadFilter", isRead);
        model.addAttribute("fromDate",     fromDate);
        model.addAttribute("toDate",       toDate);
        model.addAttribute("users",        userService.findAll());
        model.addAttribute("types",        NotificationType.values());
        model.addAttribute("notification", new Notification());
        return "admin/notification_list";
    }


    /** 3. Xử lý lưu mới */
    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("notification") Notification notification,
            BindingResult br,
            RedirectAttributes ra   // thêm RedirectAttributes
    ) {
        if (br.hasErrors()) {
            // gán thông điệp lỗi để hiển thị trên trang list
            ra.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại dữ liệu nhập.");
            return "redirect:/admin/notifications";
        }
        notificationService.saveAndReturn(notification);
        ra.addFlashAttribute("successMessage", "Thêm notification thành công!");
        return "redirect:/admin/notifications";
    }


    /** 5. Xử lý lưu sửa */
    @PostMapping("/edit/{id}")
    public String edit(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("notification") Notification notification,
            BindingResult br,
            RedirectAttributes ra   // thêm RedirectAttributes
    ) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại dữ liệu chỉnh sửa.");
            return "redirect:/admin/notifications";
        }
        notification.setNotificationId(id);
        notificationService.saveAndReturn(notification);
        ra.addFlashAttribute("successMessage", "Cập nhật notification thành công!");
        return "redirect:/admin/notifications";
    }
}
