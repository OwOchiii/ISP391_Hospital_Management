package orochi.controller;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
import orochi.model.Doctor;
import orochi.service.impl.DoctorServiceImpl;

@Controller
@RequestMapping("/admin/specializations")
public class AdminSpecializationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSpecializationController.class); // ADDED: Logger for debugging

    @Autowired
    private SpecializationServiceImpl specializationService;

    @Autowired
    private DoctorServiceImpl doctorService;

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
            RedirectAttributes ra,
            Model model) {

        if (result.hasErrors()) {
            logger.warn("Validation errors: {}", result.getAllErrors());
            Page<Specialization> specPage =
                    specializationService.getSpecializationsPage(page, size, search, symptomFilter);
            model.addAttribute("specializations", specPage.getContent());
            model.addAttribute("currentPage",     page);
            model.addAttribute("totalPages",      specPage.getTotalPages());
            model.addAttribute("pageSize",        size);
            model.addAttribute("adminId",         adminId);
            model.addAttribute("search",          search);
            model.addAttribute("symptomFilter",   symptomFilter);
            model.addAttribute("symptomList",     specializationService.getAllDistinctSymptoms());
            model.addAttribute("isAddMode",       specialization.getSpecId() == null);
            return "admin/specialization/list";
        }

        boolean isNew = (specialization.getSpecId() == null);
        try {
            specializationService.saveSpecialization(specialization);
        } catch (Exception e) {
            logger.error("Error saving specialization: {}", e.getMessage());
            ra.addFlashAttribute("errorMessage", "Cannot save specialty: " + e.getMessage());
            return "redirect:/admin/specializations"
                    + "?adminId="      + adminId
                    + "&page="         + page
                    + "&size="         + size
                    + (search        != null ? "&search="       + search       : "")
                    + (symptomFilter != null ? "&symptomFilter="+ symptomFilter: "");
        }

        ra.addFlashAttribute("successMessage",
                isNew
                        ? "New specialty added successfully!"
                        : "Specialty updated successfully!");
        return "redirect:/admin/specializations"
                + "?adminId="      + adminId
                + "&page="         + page
                + "&size="         + size
                + (search        != null ? "&search="       + search       : "")
                + (symptomFilter != null ? "&symptomFilter="+ symptomFilter: "");
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
            @RequestParam("adminId") Integer adminId,
            RedirectAttributes ra) {

        try {
            specializationService.deleteSpecialization(specId);
            logger.info("Successfully deleted specialty: {}", specId);
            ra.addFlashAttribute("successMessage", "Specialty deleted successfully!");
        } catch (Exception e) {
            logger.error("Error deleting specialty: {}", e.getMessage());
            ra.addFlashAttribute("errorMessage", "Cannot delete specialty: " + e.getMessage());
        }

        return "redirect:/admin/specializations?adminId=" + adminId;
    }

    @GetMapping("/details/{specId}")
    public String showSpecializationDetails(
            @PathVariable("specId") Integer specId,
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="6") int size,
            @RequestParam(value="doctorSearch", required=false) String doctorSearch,
            Model model) {

        Specialization specialization = specializationService.getSpecializationById(specId);
        if (specialization == null) {
            logger.warn("Specialization not found with ID: {}", specId);
            return "redirect:/admin/specializations?adminId=" + adminId;
        }

        // Get doctors in this specialization with pagination
        Page<Doctor> doctorsPage = specializationService.getDoctorsBySpecializationId(
                specId, page, size, doctorSearch);

        // Get available doctors (not in this specialization)
        List<Doctor> availableDoctors = specializationService.getAvailableDoctorsForSpecialization(specId);

        model.addAttribute("specialization", specialization);
        model.addAttribute("doctorsPage", doctorsPage);
        model.addAttribute("availableDoctors", availableDoctors);
        model.addAttribute("adminId", adminId);
        model.addAttribute("doctorSearch", doctorSearch);

        return "admin/specialization/details";
    }

    @PostMapping("/addDoctor")
    public String addDoctorToSpecialization(
            @RequestParam("adminId") Integer adminId,
            @RequestParam("specId") Integer specId,
            @RequestParam("doctorId") Integer doctorId,
            Model model) {

        try {
            specializationService.addDoctorToSpecialization(doctorId, specId);
            logger.info("Successfully added doctor {} to specialization {}", doctorId, specId);
            model.addAttribute("successMessage", "Doctor successfully added to specialization.");
        } catch (Exception e) {
            logger.error("Error adding doctor to specialization: {}", e.getMessage());
            model.addAttribute("errorMessage", "Cannot add doctor to specialization: " + e.getMessage());
        }

        return "redirect:/admin/specializations/details/" + specId + "?adminId=" + adminId;
    }

    @PostMapping("/removeDoctor")
    public String removeDoctorFromSpecialization(
            @RequestParam("adminId") Integer adminId,
            @RequestParam("specId") Integer specId,
            @RequestParam("doctorId") Integer doctorId,
            Model model) {

        try {
            specializationService.removeDoctorFromSpecialization(doctorId, specId);
            logger.info("Successfully removed doctor {} from specialization {}", doctorId, specId);
            model.addAttribute("successMessage", "Doctor successfully removed from specialization.");
        } catch (Exception e) {
            logger.error("Error removing doctor from specialization: {}", e.getMessage());
            model.addAttribute("errorMessage", "Cannot remove doctor from specialization: " + e.getMessage());
        }

        return "redirect:/admin/specializations/details/" + specId + "?adminId=" + adminId;
    }
}