// src/main/java/orochi/controller/AdminPatientController.java
package orochi.controller;

import lombok.RequiredArgsConstructor;
import orochi.model.Patient;
import orochi.service.AdminPatientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/patients")
@RequiredArgsConstructor
public class AdminPatientController {
    private final AdminPatientService patientService;

    /**
     * Hiển thị danh sách bệnh nhân.
     * Nếu không có search/statusFilter, sẽ gọi getAllPatients().
     * Ngược lại gọi searchPatients().
     */
    @GetMapping
    public String list(
            @RequestParam int adminId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            Model m
    ) {
        List<Patient> patients;
        boolean hasSearch = (search != null && !search.isBlank());
        boolean hasStatus = (statusFilter != null && !statusFilter.isBlank());

        if (!hasSearch && !hasStatus) {
            patients = patientService.getAllPatients();
        } else {
            patients = patientService.searchPatients(search, statusFilter);
        }

        m.addAttribute("patients", patients);
        m.addAttribute("adminId", adminId);
        m.addAttribute("search", search);
        m.addAttribute("statusFilter", statusFilter);
        return "admin/patient/list";
    }

    /**
     * Form edit thông tin bệnh nhân.
     */
    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable int id,
            @RequestParam int adminId,
            Model m
    ) {
        m.addAttribute("patient", patientService.getPatientById(id));
        m.addAttribute("adminId", adminId);
        return "admin/patient/edit";
    }

    /**
     * Xử lý POST khi lưu thông tin edit.
     */
    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable int id,
            @RequestParam int adminId,
            @ModelAttribute Patient form
    ) {
        patientService.savePatient(form);
        return "redirect:/admin/patients?adminId=" + adminId;
    }

    /**
     * Lock hoặc unlock tài khoản bệnh nhân.
     */
    @PostMapping("/{id}/toggleLock")
    public String toggleLock(
            @PathVariable int id,
            @RequestParam int adminId
    ) {
        patientService.togglePatientLock(id);
        return "redirect:/admin/patients?adminId=" + adminId;
    }
}
