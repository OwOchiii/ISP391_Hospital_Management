// src/main/java/orochi/controller/AdminDoctorController.java
package orochi.controller;

import orochi.model.Doctor;
import orochi.model.DoctorForm;
import orochi.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/doctors")
@RequiredArgsConstructor
public class AdminDoctorController {
    private final DoctorService doctorService;

    @GetMapping
    public String list(
            @RequestParam int adminId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            Model model
    ) {
        var doctors = doctorService.searchDoctors(search, statusFilter);
        model.addAttribute("doctors",      doctors);
        model.addAttribute("adminId",      adminId);
        model.addAttribute("search",       search);
        model.addAttribute("statusFilter", statusFilter);
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

    @PostMapping("/save")
    public String save(@ModelAttribute DoctorForm doctorForm,
                       @RequestParam int adminId) {
        doctorService.saveFromForm(doctorForm);
        return "redirect:/admin/doctors?adminId=" + adminId;
    }

    @PostMapping("/{id}/toggleLock")
    public String toggleLock(@PathVariable int id,
                             @RequestParam int adminId) {
        doctorService.toggleDoctorLock(id);
        return "redirect:/admin/doctors?adminId=" + adminId;
    }
}