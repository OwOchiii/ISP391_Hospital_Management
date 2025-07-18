package orochi.controller;

import orochi.dto.MedicalResultSummaryDTO;
import orochi.dto.MedicalResultDetailDTO;
import orochi.service.MedicalResultFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/medical-results")
public class AdminMedicalResultController {

    private final MedicalResultFacade facade;

    public AdminMedicalResultController(MedicalResultFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) Integer appointmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam Integer adminId,
            @PageableDefault(size = 10, sort = "resultDate") Pageable pageable,
            Model model
    ) {
        Page<MedicalResultSummaryDTO> page = facade.listSummaries(
                appointmentId, status, doctorName, patientName, dateFrom, dateTo, pageable
        );

        model.addAttribute("page", page);
        model.addAttribute("results", page.getContent());

        // giữ nguyên các giá trị filter để Thymeleaf bind lại vào form
        model.addAttribute("appointmentId", appointmentId);
        model.addAttribute("status", status);
        model.addAttribute("doctorName", doctorName);
        model.addAttribute("patientName", patientName);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("adminId", adminId);
        

        return "admin/medical-result";
    }

    @GetMapping("/{id}/detail-json")
    @ResponseBody
    public MedicalResultDetailDTO detailJson(@PathVariable Integer id) {
        return facade.getDetail(id);
    }
}
