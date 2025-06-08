package orochi.controller;

import orochi.model.MedicalService;
import orochi.model.Specialization;
import orochi.service.impl.ServiceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/services")
public class AdminServiceController {

    @Autowired
    private ServiceServiceImpl serviceService;

    @GetMapping
    public String showServices(@RequestParam("adminId") Integer adminId, Model model) {
        model.addAttribute("services", serviceService.getAllServices());
        model.addAttribute("specializations", serviceService.getAllSpecializations()); // Thêm dòng này
        model.addAttribute("adminId", adminId);
        model.addAttribute("isAddMode", false);
        return "admin/service/list";
    }

    @GetMapping("/add")
    public String showAddForm(@RequestParam("adminId") Integer adminId, Model model) {
        model.addAttribute("adminId", adminId);
        model.addAttribute("service", new MedicalService());
        model.addAttribute("specializations", serviceService.getAllSpecializations());
        model.addAttribute("isAddMode", true);
        return "admin/service/list";
    }

    @PostMapping("/save")
    public String saveService(@RequestParam("adminId") Integer adminId,
                              @ModelAttribute MedicalService service) {
        serviceService.saveService(service);
        return "redirect:/admin/services?adminId=" + adminId;
    }

    @GetMapping("/edit/{serviceId}")
    public String showEditForm(@PathVariable("serviceId") Integer serviceId,
                               @RequestParam("adminId") Integer adminId,
                               Model model) {
        MedicalService service = serviceService.getServiceById(serviceId);
        if (service == null) {
            return "redirect:/admin/services?adminId=" + adminId;
        }
        model.addAttribute("adminId", adminId);
        model.addAttribute("service", service);
        model.addAttribute("specializations", serviceService.getAllSpecializations());
        model.addAttribute("isAddMode", false);
        return "admin/service/list";
    }

    @GetMapping("/delete/{serviceId}")
    public String deleteService(@PathVariable("serviceId") Integer serviceId,
                                @RequestParam("adminId") Integer adminId) {
        serviceService.deleteService(serviceId);
        return "redirect:/admin/services?adminId=" + adminId;
    }
}