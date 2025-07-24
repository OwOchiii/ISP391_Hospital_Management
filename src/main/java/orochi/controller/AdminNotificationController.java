package orochi.controller;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.Notification;
import orochi.model.NotificationType;
import orochi.model.Users;
import orochi.service.NotificationService;
import orochi.service.UserService;
import orochi.service.RoleService;
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

    @Autowired
    private RoleService roleService;

    /** 1. List với pagination, filter như trước */
    @GetMapping
    public String list(
            @RequestParam Integer adminId,
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
        int totalPages  = pageData.getTotalPages();
        int currentPage = pageData.getNumber();
        int startPage   = Math.max(0, currentPage - 2);
        int endPage     = Math.min(totalPages - 1, currentPage + 2);

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage",   endPage);
        model.addAttribute("pageData",     pageData);
        model.addAttribute("search",       search);
        model.addAttribute("typeFilter",   type);
        model.addAttribute("isReadFilter", isRead);
        model.addAttribute("fromDate",     fromDate);
        model.addAttribute("adminId", adminId);
        model.addAttribute("toDate",       toDate);
        model.addAttribute("users",        userService.findAll());
        model.addAttribute("types",        NotificationType.values());
        model.addAttribute("notification", new Notification());
        model.addAttribute("roles", roleService.getAllRoles());
        return "admin/notification_list";
    }


    @PostMapping("/create")
    public String create(
            @ModelAttribute("notification") Notification notification,
            @RequestParam("target") String target,
            @RequestParam(value="role", required=false) String role,
            RedirectAttributes ra
    ) {
        // 1) Manual validate chỉ message và type
        if (notification.getType() == null || notification.getMessage() == null || notification.getMessage().isBlank()) {
            ra.addFlashAttribute("errorMessage", "Type và Message là bắt buộc.");
            return "redirect:/admin/notifications";
        }

        // 2) Xác định recipients như cũ
        List<Users> recipients;
        switch (target) {
            case "ALL":  recipients = userService.findAll();        break;
            case "ROLE": recipients = userService.findByRole(role); break;
            default:     /* USER */
                Users u = userService.findById(notification.getUserId()).orElse(null);
                recipients = u != null ? List.of(u) : List.of();
        }

        // 3) Tạo và lưu
        LocalDateTime now = LocalDateTime.now();
        for (Users u : recipients) {
            Notification n = new Notification();
            n.setUserId(u.getUserId());
            n.setType(notification.getType());
            n.setMessage(notification.getMessage());
            n.setCreatedAt(now);
            notificationService.save(n);
        }

        // 4) Đẩy flash‑attributes cho view
        ra.addFlashAttribute("successMessage",
                "Gửi thông báo thành công tới " + recipients.size() + " tài khoản!");
        ra.addFlashAttribute("targetFlag", target);
        ra.addFlashAttribute("roleFlag", role);

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
