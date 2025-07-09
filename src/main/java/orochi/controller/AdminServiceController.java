package orochi.controller;

import orochi.model.MedicalService;
import orochi.model.Specialization;
import orochi.service.impl.ServiceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Controller
@RequestMapping("/admin/services")
public class AdminServiceController {

    @Autowired
    private ServiceServiceImpl serviceService;

    @GetMapping
    public String showServices(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="6") int size,
            Model model) {

        Page<MedicalService> servicePage = serviceService.getServicesPage(page, size);
        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("currentPage", servicePage.getNumber());
        model.addAttribute("totalPages", servicePage.getTotalPages());
        model.addAttribute("pageSize", servicePage.getSize());
        model.addAttribute("adminId", adminId);
        model.addAttribute("specializations", serviceService.getAllSpecializations());
        model.addAttribute("isAddMode", false);
        model.addAttribute("service", new MedicalService()); // THÊM: Đặt đối tượng service mặc định để tránh null
        return "admin/service/list";
    }

    @GetMapping("/add")
    public String showAddForm(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="6") int size,
            Model model) {
        Page<MedicalService> servicePage = serviceService.getServicesPage(page, size);
        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("currentPage", servicePage.getNumber());
        model.addAttribute("totalPages", servicePage.getTotalPages());
        model.addAttribute("pageSize", servicePage.getSize());
        model.addAttribute("adminId", adminId);
        model.addAttribute("specializations", serviceService.getAllSpecializations());
        model.addAttribute("isAddMode", true);
        model.addAttribute("service", new MedicalService());
        return "admin/service/list";
    }

    @GetMapping("/edit/{serviceId}")
    public String showEditForm(
            @PathVariable("serviceId") Integer serviceId,
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="6") int size,
            Model model) {

        MedicalService service = serviceService.getServiceById(serviceId);
        if (service == null) {
            return "redirect:/admin/services?adminId=" + adminId;
        }
        Page<MedicalService> servicePage = serviceService.getServicesPage(page, size);
        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("currentPage", servicePage.getNumber());
        model.addAttribute("totalPages", servicePage.getTotalPages());
        model.addAttribute("pageSize", servicePage.getSize());
        model.addAttribute("adminId", adminId);
        model.addAttribute("specializations", serviceService.getAllSpecializations());
        model.addAttribute("isAddMode", false);
        model.addAttribute("service", service);
        return "admin/service/list";
    }

    @GetMapping("/delete/{serviceId}")
    public String deleteService(
            @PathVariable("serviceId") Integer serviceId,
            @RequestParam("adminId") Integer adminId) {
        serviceService.deleteService(serviceId);
        return "redirect:/admin/services?adminId=" + adminId;
    }

    @PostMapping("/save")
    public String saveService(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="6") int size,
            @ModelAttribute MedicalService service,
            Model model) {
        serviceService.saveService(service);
        return "redirect:/admin/services?adminId=" + adminId + "&page=" + page + "&size=" + size;
    }
}