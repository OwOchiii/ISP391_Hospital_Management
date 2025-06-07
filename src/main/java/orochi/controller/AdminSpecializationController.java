package orochi.controller;

import orochi.model.Specialization;
import orochi.service.impl.SpecializationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/specializations")
public class AdminSpecializationController {

    @Autowired
    private SpecializationServiceImpl specializationService;

    @GetMapping
    public String showSpecializations(@RequestParam("adminId") Integer adminId, Model model) {
        model.addAttribute("specializations", specializationService.getAllSpecializations());
        model.addAttribute("adminId", adminId);
        return "admin/specialization/list";
    }

    @GetMapping("/add")
    public String showAddForm(@RequestParam("adminId") Integer adminId, Model model) {
        model.addAttribute("adminId", adminId);
        model.addAttribute("specialization", new Specialization()); // Tạo đối tượng mới
        model.addAttribute("isAddMode", true); // Thêm biến để phân biệt chế độ thêm
        return "admin/specialization/list";
    }

    @PostMapping("/save")
    public String saveSpecialization(@RequestParam("adminId") Integer adminId,
                                     @ModelAttribute Specialization specialization) {
        specializationService.saveSpecialization(specialization);
        return "redirect:/admin/specializations?adminId=" + adminId;
    }

    @GetMapping("/edit/{specId}")
    public String showEditForm(@PathVariable("specId") Integer specId,
                               @RequestParam("adminId") Integer adminId,
                               Model model) {
        Specialization specialization = specializationService.getSpecializationById(specId);
        if (specialization == null) {
            return "redirect:/admin/specializations?adminId=" + adminId; // Xử lý nếu không tìm thấy
        }
        model.addAttribute("adminId", adminId);
        model.addAttribute("specialization", specialization);
        model.addAttribute("isAddMode", false); // Thêm biến để phân biệt chế độ chỉnh sửa
        return "admin/specialization/list";
    }

    @GetMapping("/delete/{specId}")
    public String deleteSpecialization(@PathVariable("specId") Integer specId,
                                       @RequestParam("adminId") Integer adminId) {
        specializationService.deleteSpecialization(specId);
        return "redirect:/admin/specializations?adminId=" + adminId;
    }
}