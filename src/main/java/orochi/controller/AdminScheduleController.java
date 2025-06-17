package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import orochi.dto.ScheduleDTO;
import orochi.model.Doctor;
import orochi.model.Room;
import orochi.service.impl.ScheduleServiceImpl;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/schedules")
public class AdminScheduleController {

    @Autowired
    private ScheduleServiceImpl scheduleService;

    @GetMapping
    public String showSchedules(@RequestParam(value = "adminId", required = true) Integer adminId,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "startDate", required = false) String startDateStr,
                                @RequestParam(value = "endDate", required = false) String endDateStr,
                                Model model) {
        LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : null;
        LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : null;

        List<ScheduleDTO> schedules = scheduleService.searchSchedules(search, startDate, endDate);
        model.addAttribute("schedules", schedules);
        model.addAttribute("adminId", adminId);
        model.addAttribute("search", search);
        model.addAttribute("startDate", startDateStr);
        model.addAttribute("endDate", endDateStr);
        model.addAttribute("doctors", scheduleService.getAllDoctors());
        model.addAttribute("rooms", scheduleService.getAllRooms());
        return "admin/schedule/list";
    }

    @GetMapping("/{id}/delete")
    public String deleteSchedule(@PathVariable Integer id, @RequestParam("adminId") Integer adminId, Model model) {
        scheduleService.deleteSchedule(id);
        return "redirect:/admin/schedules?adminId=" + adminId;
    }

    @PostMapping("/save")
    public String saveSchedule(@ModelAttribute ScheduleDTO scheduleDTO, @RequestParam("adminId") Integer adminId, Model model) {
        scheduleService.saveSchedule(scheduleDTO);
        return "redirect:/admin/schedules?adminId=" + adminId;
    }
}