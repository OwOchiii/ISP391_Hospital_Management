// src/main/java/orochi/controller/AdminPatientController.java
package orochi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import orochi.model.PatientForm;
import orochi.model.Patient;
import orochi.service.PatientService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/patients")
@RequiredArgsConstructor
public class AdminPatientController {

    private final PatientService svc;

    @GetMapping
    public String list(
            @RequestParam int adminId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model
    ) {
        Page<Patient> pd = svc.searchPatients(search, statusFilter, page, size);
        model.addAttribute("patients", pd.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pd.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("adminId", adminId);
        model.addAttribute("pageSize", size);
        return "admin/patient/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable int id,
            @RequestParam int adminId,
            Model model
    ) {
        model.addAttribute("patientForm", svc.loadForm(id));
        model.addAttribute("adminId", adminId);
        return "admin/patient/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable int id,
            @RequestParam int adminId,
            @Valid @ModelAttribute("patientForm") PatientForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes flash,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        if (binding.hasErrors()) {
            Page<Patient> pd = svc.searchPatients(search, statusFilter, page, size);
            model.addAttribute("patients", pd.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", pd.getTotalPages());
            model.addAttribute("search", search);
            model.addAttribute("statusFilter", statusFilter);
            model.addAttribute("adminId", adminId);
            return "admin/patient/list";
        }

        svc.saveFromForm(form);
        flash.addFlashAttribute("successMessage", "Lưu bệnh nhân thành công!");
        return "redirect:/admin/patients?adminId=" + adminId
                + (search != null ? "&search=" + search : "")
                + (statusFilter != null ? "&statusFilter=" + statusFilter : "")
                + "&page=" + page + "&size=" + size;
    }
}
