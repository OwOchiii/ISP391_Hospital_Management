// src/main/java/orochi/controller/AdminDoctorController.java
package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;

import orochi.model.Doctor;
import orochi.model.DoctorForm;
import orochi.service.DoctorService;
import orochi.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.util.List;
import orochi.model.Users;
import orochi.service.UserService;

@Controller
@RequestMapping("/admin/doctors")
@RequiredArgsConstructor
public class AdminDoctorController {
    private final DoctorService doctorService;

    @Autowired
    @Qualifier("userServiceImpl")
    private UserService userService;


    @Autowired
    private RoleService roleService;


    @GetMapping
    public String list(
            @RequestParam int adminId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model
    ) {
        Page<Doctor> pageData = doctorService.searchDoctors(search, statusFilter, page, size);
        model.addAttribute("doctors",      pageData.getContent());
        model.addAttribute("currentPage",  page);
        model.addAttribute("totalPages",   pageData.getTotalPages());
        model.addAttribute("pageSize",     size);
        model.addAttribute("adminId",      adminId);
        model.addAttribute("search",       search);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("roles", roleService.getAllRoles());
        model.addAttribute("doctorForm", new DoctorForm());

        return "admin/doctor/list";
    }



    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id,
                           @RequestParam int adminId,
                           Model model) {
        model.addAttribute("doctorForm", doctorService.loadForm(id));
        model.addAttribute("adminId", adminId);
        return "admin/doctor/edit";
    }

    @GetMapping("/add")
    public String addForm(@RequestParam int adminId, Model model) {
        model.addAttribute("doctorForm", new DoctorForm());
        model.addAttribute("adminId", adminId);
        return "admin/doctor/add";
    }

//    @PostMapping("/{id}/edit")
//    public String update(@PathVariable int id,
//                         @RequestParam int adminId,
//                         @ModelAttribute Doctor form) {
//        doctorService.saveDoctor(form);
//        return "redirect:/admin/doctors?adminId=" + adminId;
//    }

    @PostMapping("/{id}/changeRole")
    public String changeDoctorRole(
            @PathVariable("id") int doctorId,
            @RequestParam("newRoleId") int newRoleId,
            @RequestParam("adminId")  int adminId,
            RedirectAttributes flash
    ) {
        Doctor d = doctorService.getDoctorById(doctorId);
        Users u = d.getUser();
        u.setRoleId(newRoleId);
        try {
            userService.save(u);
            flash.addFlashAttribute("successMessage", "Đổi quyền thành công!");
        } catch (DataAccessException ex) {
            // Bắt exception SQLServerException chuyển thành message người dùng
            flash.addFlashAttribute("errorMessage",
                    "Không thể đổi quyền: người dùng này đang có bản ghi bác sĩ. "
                            + "Bạn phải xóa/hủy liên kết bác sĩ trước khi đổi role.");
        }
        return "redirect:/admin/doctors?adminId=" + adminId;
    }



    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("doctorForm") DoctorForm doctorForm,
            BindingResult bindingResult,
            @RequestParam int adminId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model,
            RedirectAttributes flash
    ) {
        if (bindingResult.hasErrors()) {
            // load lại dữ liệu danh sách
            Page<Doctor> pageData = doctorService.searchDoctors(search, statusFilter, page, size);
            model.addAttribute("doctors",      pageData.getContent());
            model.addAttribute("currentPage",  page);
            model.addAttribute("totalPages",   pageData.getTotalPages());
            model.addAttribute("pageSize",     size);
            model.addAttribute("search",       search);
            model.addAttribute("statusFilter", statusFilter);
            model.addAttribute("roles",        roleService.getAllRoles());
            model.addAttribute("adminId",      adminId);
            // doctorForm + bindingResult đã sẵn có
            return "admin/doctor/list";
        }

        doctorService.saveFromForm(doctorForm);
        flash.addFlashAttribute("successMessage", "Lưu bác sĩ thành công!");
        return "redirect:/admin/doctors?adminId=" + adminId
                + (search!=null? "&search="+search:"")
                + (statusFilter!=null? "&statusFilter="+statusFilter:"")
                + "&page="+page+"&size="+size;
    }






    @PostMapping("/{id}/toggleLock")
    public String toggleLock(@PathVariable int id,
                             @RequestParam int adminId) {
        doctorService.toggleDoctorLock(id);
        return "redirect:/admin/doctors?adminId=" + adminId;
    }
}