package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.Users;
import orochi.model.Receptionist;
import orochi.service.UserService;
import orochi.service.FileStorageService;
import orochi.service.impl.ReceptionistService;

@Controller
@RequestMapping("/admin/receptionists")
public class AdminReceptionistController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ReceptionistService receptionistService;

    @Value("${app.default.receptionist.password}")
    private String defaultPassword;

    @GetMapping
    public String listReceptionists(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model) {

        // Tạo Pageable object cho phân trang
        Pageable pageable = PageRequest.of(page, size);

        // Lấy danh sách lễ tân theo phân trang và lọc
        Page<Users> receptionistPage = userService.getAllReceptionists(search, statusFilter, pageable);

        model.addAttribute("receptionists", receptionistPage.getContent());
        model.addAttribute("adminId", adminId);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", receptionistPage.getTotalPages());
        model.addAttribute("pageSize", size);
        return "admin/receptionist/list";
    }

    @PostMapping("/{id}/toggleLock")
    public String toggleLockReceptionist(
            @PathVariable("id") Integer userId,
            @RequestParam("adminId") Integer adminId) {

        userService.findById(userId).ifPresent(u -> {
            String newStatus = u.getStatus().equalsIgnoreCase("LOCKED") ? "ACTIVE" : "LOCKED";
            u.setStatus(newStatus);
            userService.save(u);
        });

        return "redirect:/admin/receptionists?adminId=" + adminId + "&page=0&size=5";
    }

    @PostMapping("/save")
    public String saveReceptionist(
            @ModelAttribute("receptionistForm") Users formUser,
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            RedirectAttributes redirectAttributes) {

        formUser.setRoleId(3);

        if (formUser.getUserId() != null) {
            userService.findById(formUser.getUserId())
                    .ifPresent(orig -> formUser.setPasswordHash(orig.getPasswordHash()));
        } else {
            String encoded = passwordEncoder.encode(defaultPassword);
            formUser.setPasswordHash(encoded);
            redirectAttributes.addFlashAttribute("newReceptionistPassword", defaultPassword);
        }

        // Lưu user trước
        Users savedUser = userService.save(formUser);

        // Xử lý upload ảnh avatar nếu có
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String imageUrl = fileStorageService.storeFile(avatarFile, "receptionist-avatars");

                // Tìm hoặc tạo mới record Receptionist
                Receptionist receptionist = receptionistService.findByUserId(savedUser.getUserId());
                if (receptionist == null) {
                    receptionist = new Receptionist();
                    receptionist.setUserId(savedUser.getUserId());
                }
                receptionist.setImageUrl(imageUrl);
                receptionistService.save(receptionist);

                redirectAttributes.addFlashAttribute("message", "Lưu thông tin và ảnh đại diện thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lưu thông tin thành công nhưng không thể upload ảnh: " + e.getMessage());
            }
        }

        return "redirect:/admin/receptionists?adminId=" + adminId + "&page=0&size=5";
    }

    @PostMapping("/upload-avatar")
    @ResponseBody
    public String uploadAvatar(
            @RequestParam("userId") Integer userId,
            @RequestParam("avatarFile") MultipartFile avatarFile) {

        try {
            if (avatarFile.isEmpty()) {
                return "{\"success\": false, \"message\": \"Vui lòng chọn file ảnh\"}";
            }

            // Kiểm tra định dạng file
            String contentType = avatarFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return "{\"success\": false, \"message\": \"Chỉ chấp nhận file ảnh\"}";
            }

            // Upload file
            String imageUrl = fileStorageService.storeFile(avatarFile, "receptionist-avatars");

            // Cập nhật thông tin receptionist
            Receptionist receptionist = receptionistService.findByUserId(userId);
            if (receptionist == null) {
                receptionist = new Receptionist();
                receptionist.setUserId(userId);
            }

            // Xóa ảnh cũ nếu có
            if (receptionist.getImageUrl() != null) {
                fileStorageService.deleteFile(receptionist.getImageUrl());
            }

            receptionist.setImageUrl(imageUrl);
            receptionistService.save(receptionist);

            return "{\"success\": true, \"imageUrl\": \"" + imageUrl + "\", \"message\": \"Upload ảnh thành công!\"}";

        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"Lỗi upload ảnh: " + e.getMessage() + "\"}";
        }
    }
}