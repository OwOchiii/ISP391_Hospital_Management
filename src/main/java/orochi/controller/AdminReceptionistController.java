    package orochi.controller;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.beans.factory.annotation.Qualifier;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.util.StringUtils;
    import org.springframework.web.servlet.mvc.support.RedirectAttributes;

    import orochi.model.Users;
    import orochi.service.UserService;
    import orochi.service.RoleService;
    @Controller
    @RequestMapping("/admin/receptionists")
    public class AdminReceptionistController {

        @Autowired
        @Qualifier("receptionistService")
        private UserService userService;

        @Autowired
        private RoleService roleService;

        @GetMapping
        public String listReceptionists(
                @RequestParam("adminId") Integer adminId,
                @RequestParam(value = "search", required = false) String search,
                @RequestParam(value = "statusFilter", required = false) String statusFilter,
                @RequestParam(value = "page", defaultValue = "0") int page,
                @RequestParam(value = "size", defaultValue = "5") int size,
                Model model) {

            search = StringUtils.hasText(search) ? search.trim() : "";
            statusFilter = StringUtils.hasText(statusFilter) ? statusFilter : null;

            Pageable pageable = PageRequest.of(page, size);
            Page<Users> receptionistPage = userService.getAllReceptionists(search, statusFilter, pageable);

            model.addAttribute("receptionists", receptionistPage.getContent());
            model.addAttribute("adminId", adminId);
            model.addAttribute("search", search);
            model.addAttribute("statusFilter", statusFilter);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", receptionistPage.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("roles", roleService.getAllRoles());

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

        @PostMapping("/{id}/changeRole")
        public String changeReceptionistRole(
                @PathVariable("id") Integer userId,
                @RequestParam("newRoleId") Integer newRoleId,
                @RequestParam("adminId") Integer adminId,
                RedirectAttributes flash) {

            if (userService.hasPatientRecords(userId)) {
                flash.addFlashAttribute("errorMessage",
                        "Không thể đổi quyền: receptionist đã có hồ sơ bệnh nhân.");
                return "redirect:/admin/receptionists?adminId=" + adminId;
            }

            userService.findById(userId).ifPresent(u -> {
                u.setRoleId(newRoleId);
                userService.save(u);
            });
            flash.addFlashAttribute("successMessage", "Đổi quyền thành công!");
            return "redirect:/admin/receptionists?adminId=" + adminId;
        }



        @PostMapping("/save")
        public String saveReceptionist(
                @ModelAttribute("receptionistForm") Users formUser,
                @RequestParam("adminId") Integer adminId) {

            formUser.setRoleId(3);

            if (formUser.getUserId() != null) {
                userService.findById(formUser.getUserId()).ifPresent(orig -> formUser.setPasswordHash(orig.getPasswordHash()));
            } else {
                formUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
            }

            userService.save(formUser);
            return "redirect:/admin/receptionists?adminId=" + adminId + "&page=0&size=5";
        }
    }