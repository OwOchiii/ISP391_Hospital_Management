package orochi.controller;

import orochi.model.Specialization;
import orochi.service.impl.SpecializationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

@Controller
@RequestMapping("/admin/specializations")
public class AdminSpecializationController {

    @Autowired
    private SpecializationServiceImpl specializationService;

    @GetMapping
    public String showSpecializations(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="6") int size,
            Model model) {

        Page<Specialization> specPage = specializationService.getSpecializationsPage(page, size);
        model.addAttribute("specializations", specPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", specPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("adminId", adminId);
        model.addAttribute("isAddMode", false);
        return "admin/specialization/list";
    }

    @GetMapping("/add")
    public String showAddForm(@RequestParam("adminId") Integer adminId, Model model) {
        model.addAttribute("adminId", adminId);
        model.addAttribute("specialization", new Specialization());
        model.addAttribute("isAddMode", true);
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
        }
        model.addAttribute("adminId", adminId);
        model.addAttribute("specialization", specialization);
        model.addAttribute("isAddMode", false);
        return "admin/specialization/list";
    }

    @GetMapping("/delete/{specId}")
    public String deleteSpecialization(@PathVariable("specId") Integer specId,
                                       @RequestParam("adminId") Integer adminId) {
        specializationService.deleteSpecialization(specId);
        return "redirect:/admin/specializations?adminId=" + adminId;
    }
}