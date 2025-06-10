package orochi.controller;

import orochi.model.Users;
import orochi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/receptionists")
public class AdminReceptionistController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listReceptionists(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            Model model) {

        // 1. Lấy toàn bộ receptionist (RoleID = 3)
        List<Users> allReceptionists = userService.getAllReceptionists();

        // 2. Nếu có filter theo search
        if (search != null && !search.isBlank()) {
            String keyword = search.trim().toLowerCase();
            allReceptionists = allReceptionists.stream()
                    .filter(u -> u.getFullName().toLowerCase().contains(keyword)
                            || u.getEmail().toLowerCase().contains(keyword))
                    .toList();
        }

        // 3. Nếu có filter theo status
        if (statusFilter != null && !statusFilter.isBlank()) {
            allReceptionists = allReceptionists.stream()
                    .filter(u -> u.getStatus().equalsIgnoreCase(statusFilter))
                    .toList();
        }

        model.addAttribute("receptionists", allReceptionists);
        model.addAttribute("adminId", adminId);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);
        return "admin/receptionist/list";
    }

    @PostMapping("/{id}/toggleLock")
    public String toggleLockReceptionist(
            @PathVariable("id") Integer userId,
            @RequestParam("adminId") Integer adminId) {

        userService.findById(userId).ifPresent(u -> {
            // Đổi trạng thái giữa ACTIVE <-> LOCKED
            String newStatus = u.getStatus().equalsIgnoreCase("LOCKED") ? "ACTIVE" : "LOCKED";
            u.setStatus(newStatus);
            userService.save(u);
        });

        return "redirect:/admin/receptionists?adminId=" + adminId;
    }

    @PostMapping("/save")
    public String saveReceptionist(
            @ModelAttribute("receptionistForm") Users formUser,
            @RequestParam("adminId") Integer adminId) {

        // Luôn đặt roleId = 3 (Receptionist)
        formUser.setRoleId(3);

        if (formUser.getUserId() != null) {
            // Trường hợp Edit: giữ nguyên mật khẩu cũ
            Optional<Users> existing = userService.findById(formUser.getUserId());
            existing.ifPresent(orig -> formUser.setPasswordHash(orig.getPasswordHash()));
        } else {
            // Trường hợp Add: gán passwordHash tạm (thực tế bạn nên hash bằng PasswordEncoder)
            // Ví dụ gán một chuỗi hash mẫu hoặc mã hóa theo logic riêng:
            formUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
            // → Nếu DB không cho null, bạn cần encode một mật khẩu mặc định:
            //    formUser.setPasswordHash(passwordEncoder.encode("123456"));
        }

        userService.save(formUser);
        return "redirect:/admin/receptionists?adminId=" + adminId;
    }
}
