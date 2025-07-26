// src/main/java/orochi/controller/AdminServiceController.java
package orochi.controller;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.MedicalService;
import orochi.service.impl.ServiceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/services")
public class AdminServiceController {

    @Autowired
    private ServiceServiceImpl serviceService;

    @GetMapping
    public String showServices(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page",       defaultValue="0") int page,
            @RequestParam(value="size",       defaultValue="6") int size,
            @RequestParam(value="search",     required=false) String search,
            @RequestParam(value="specFilter", required=false) Integer specFilter,
            Model model
    ) {
        Page<MedicalService> servicePage =
                serviceService.getServicesPage(page, size, search, specFilter);

        model.addAttribute("services",      servicePage.getContent());
        model.addAttribute("currentPage",   servicePage.getNumber());
        model.addAttribute("totalPages",    servicePage.getTotalPages());
        model.addAttribute("pageSize",      servicePage.getSize());
        model.addAttribute("search",        search);
        model.addAttribute("specFilter",    specFilter);
        model.addAttribute("specializations", serviceService.getAllSpecializations());
        model.addAttribute("adminId",       adminId);
        model.addAttribute("service",       new MedicalService());
        model.addAttribute("isAddMode",     true);
        return "admin/service/list";
    }

    @GetMapping("/edit/{serviceId}")
    public String showEditForm(
            @PathVariable Integer serviceId,
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page",       defaultValue="0") int page,
            @RequestParam(value="size",       defaultValue="6") int size,
            @RequestParam(value="search",     required=false) String search,
            @RequestParam(value="specFilter", required=false) Integer specFilter,
            Model model
    ) {
        MedicalService svc = serviceService.getServiceById(serviceId);
        if (svc == null) {
            return "redirect:/admin/services?adminId=" + adminId;
        }
        Page<MedicalService> servicePage =
                serviceService.getServicesPage(page, size, search, specFilter);

        model.addAttribute("services",      servicePage.getContent());
        model.addAttribute("currentPage",   servicePage.getNumber());
        model.addAttribute("totalPages",    servicePage.getTotalPages());
        model.addAttribute("pageSize",      servicePage.getSize());
        model.addAttribute("search",        search);
        model.addAttribute("specFilter",    specFilter);
        model.addAttribute("specializations", serviceService.getAllSpecializations());
        model.addAttribute("adminId",       adminId);
        model.addAttribute("service",       svc);
        model.addAttribute("isAddMode",     false);
        return "admin/service/list";
    }

    @PostMapping("/save")
    public String saveService(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page",       defaultValue="0") int page,
            @RequestParam(value="size",       defaultValue="6") int size,
            @RequestParam(value="search",     required=false) String search,
            @RequestParam(value="specFilter", required=false) Integer specFilter,

            @Valid @ModelAttribute("service") MedicalService service,
            BindingResult result,
            RedirectAttributes ra,
            Model model
    ) {
        // Nếu có lỗi validation, trả lại view để hiển thị form + lỗi
        if (result.hasErrors()) {
            Page<MedicalService> servicePage =
                    serviceService.getServicesPage(page, size, search, specFilter);

            model.addAttribute("services",        servicePage.getContent());
            model.addAttribute("currentPage",     servicePage.getNumber());
            model.addAttribute("totalPages",      servicePage.getTotalPages());
            model.addAttribute("pageSize",        servicePage.getSize());
            model.addAttribute("search",          search);
            model.addAttribute("specFilter",      specFilter);
            model.addAttribute("specializations", serviceService.getAllSpecializations());
            model.addAttribute("adminId",         adminId);
            model.addAttribute("isAddMode",       service.getServiceId() == null);
            return "admin/service/list";
        }

        // Xác định là thêm mới hay cập nhật
        boolean isNew = (service.getServiceId() == null);

        // Lưu service
        serviceService.saveService(service);

        // Đẩy flash‑message
        ra.addFlashAttribute("successMessage",
                isNew
                        ? "New service created successfully!"
                        : "Service updated successfully!");

        // Redirect về danh sách kèm paging và filter giữ nguyên
        return "redirect:/admin/services?adminId=" + adminId
                + "&page="     + page
                + "&size="     + size
                + (search     != null ? "&search="     + search     : "")
                + (specFilter != null ? "&specFilter=" + specFilter : "");
    }

    @GetMapping("/delete/{serviceId}")
    public String deleteService(
            @PathVariable Integer serviceId,
            @RequestParam("adminId") Integer adminId,
            RedirectAttributes ra
    ) {
        // Xóa service
        serviceService.deleteService(serviceId);

        // Đẩy flash‑message
        ra.addFlashAttribute("successMessage", "Service deleted successfully!");

        // Redirect về danh sách
        return "redirect:/admin/services?adminId=" + adminId;
    }

}
