package orochi.controller;

import orochi.model.Specialization;
import orochi.service.impl.SpecializationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
@RequestMapping("/admin/specializations")
public class AdminSpecializationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSpecializationController.class); // ADDED: Logger for debugging

    @Autowired
    private SpecializationServiceImpl specializationService;

    @GetMapping
    public String showSpecializations(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="6") int size,
            @RequestParam(value="search", required=false) String search,
            @RequestParam(value="symptomFilter", required=false) String symptomFilter,
            Model model) {

        Page<Specialization> specPage =
                specializationService.getSpecializationsPage(page, size, search, symptomFilter);

        List<String> symptomList = specializationService.getAllDistinctSymptoms();

        model.addAttribute("specializations", specPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", specPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("adminId", adminId);
        model.addAttribute("search", search);
        model.addAttribute("symptomFilter", symptomFilter);
        model.addAttribute("symptomList", symptomList);
        model.addAttribute("isAddMode", false);
        model.addAttribute("specialization", new Specialization());
        return "admin/specialization/list";
    }

    @GetMapping("/add")
    public String showAddForm(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page, // ADDED: Support pagination
            @RequestParam(value="size", defaultValue="6") int size,
            @RequestParam(value="search", required=false) String search,
            @RequestParam(value="symptomFilter", required=false) String symptomFilter,  // ADDED: Support pagination
            Model model) {
        Page<Specialization> specPage =
                specializationService.getSpecializationsPage(page, size, search, symptomFilter); // ADDED: Fetch specialization list
        model.addAttribute("specializations", specPage.getContent()); // ADDED: Add specialization
        model.addAttribute("search", search);
        model.addAttribute("symptomFilter", symptomFilter);
        model.addAttribute("currentPage", page); // ADDED: Add pagination info
        model.addAttribute("totalPages", specPage.getTotalPages()); // ADDED: Add pagination info
        model.addAttribute("pageSize", size); // ADDED: Add pagination info
        model.addAttribute("adminId", adminId);
        model.addAttribute("specialization", new Specialization());
        model.addAttribute("isAddMode", true);
        return "admin/specialization/list";
    }

    @PostMapping("/save")
    public String saveSpecialization(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="6") int size,
            @RequestParam(value="search", required=false) String search,
            @RequestParam(value="symptomFilter", required=false) String symptomFilter,
            @Valid @ModelAttribute Specialization specialization,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            logger.warn("Validation errors: {}", result.getAllErrors());
            Page<Specialization> specPage =
                    specializationService.getSpecializationsPage(page, size, search, symptomFilter);
            model.addAttribute("specializations", specPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", specPage.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("adminId", adminId);
            model.addAttribute("search", search);
            model.addAttribute("symptomFilter", symptomFilter);
            model.addAttribute("symptomList", specializationService.getAllDistinctSymptoms());
            model.addAttribute("isAddMode", specialization.getSpecId() == null);
            return "admin/specialization/list";
        }

        try {
            specializationService.saveSpecialization(specialization);
        } catch (Exception e) {
            logger.error("Error saving specialization: {}", e.getMessage());
            model.addAttribute("errorMessage", "Cannot save specialization: " + e.getMessage());
            Page<Specialization> specPage =
                    specializationService.getSpecializationsPage(page, size, search, symptomFilter);
            model.addAttribute("specializations", specPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", specPage.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("adminId", adminId);
            model.addAttribute("search", search);
            model.addAttribute("symptomFilter", symptomFilter);
            model.addAttribute("symptomList", specializationService.getAllDistinctSymptoms());
            model.addAttribute("isAddMode", specialization.getSpecId() == null);
            return "admin/specialization/list";
        }

        return "redirect:/admin/specializations"
                + "?adminId=" + adminId
                + "&page=" + page
                + "&size=" + size
                + (search != null ? "&search=" + search : "")
                + (symptomFilter != null ? "&symptomFilter=" + symptomFilter : "");
    }

    @GetMapping("/edit/{specId}")
    public String showEditForm(
            @PathVariable("specId") Integer specId,
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page, // ADDED: Support pagination
            @RequestParam(value="size", defaultValue="6") int size, // ADDED: Support pagination
            Model model) {
        Specialization specialization = specializationService.getSpecializationById(specId);
        if (specialization == null) {
            logger.warn("Specialization not found with ID: {}", specId); // ADDED: Log missing specialization
            return "redirect:/admin/specializations?adminId=" + adminId; // MODIFIED: Redirect if not found
        }
        Page<Specialization> specPage = specializationService.getSpecializationsPage(page, size); // ADDED: Fetch specialization list
        model.addAttribute("specializations", specPage.getContent()); // ADDED: Add specialization list
        model.addAttribute("currentPage", page); // ADDED: Add pagination info
        model.addAttribute("totalPages", specPage.getTotalPages()); // ADDED: Add pagination info
        model.addAttribute("pageSize", size); // ADDED: Add pagination info
        model.addAttribute("adminId", adminId);
        model.addAttribute("specialization", specialization);
        model.addAttribute("isAddMode", false);
        return "admin/specialization/list";
    }

    @GetMapping("/delete/{specId}")
    public String deleteSpecialization(
            @PathVariable("specId") Integer specId,
            @RequestParam("adminId") Integer adminId) {
        try {
            specializationService.deleteSpecialization(specId);
            logger.info("Successfully deleted specialization: {}", specId); // ADDED: Log success
        } catch (Exception e) {
            logger.error("Error deleting specialization: {}", e.getMessage()); // ADDED: Log error
        }
        return "redirect:/admin/specializations?adminId=" + adminId;
    }
}