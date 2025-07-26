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

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.Users;
import orochi.service.UserService;
import orochi.service.impl.ReceptionistService;

@Controller
@RequestMapping("/admin/receptionists")
public class AdminReceptionistController {
    @Autowired
    private ReceptionistService receptionistService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        if (search != null && search.trim().isEmpty()) {
            search = null;
        }
        // Xử lý tương tự cho statusFilter
        if (statusFilter != null && statusFilter.trim().isEmpty()) {
            statusFilter = null;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Users> receptionistPage =
                userService.getAllReceptionists(search, statusFilter, pageable);

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

        userService.save(formUser);
        return "redirect:/admin/receptionists?adminId=" + adminId + "&page=0&size=5";
    }
}