package orochi.controller;

import orochi.model.Schedule;
import orochi.service.impl.ScheduleServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleServiceImpl scheduleService;

    @GetMapping
    public String showSchedules(@RequestParam("adminId") Integer adminId,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "dateFilter", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFilter,
                                Model model) {
        List<Schedule> schedules = scheduleService.searchSchedules(search, dateFilter);
        model.addAttribute("schedules", schedules);
        model.addAttribute("adminId", adminId);
        model.addAttribute("search", search);
        model.addAttribute("dateFilter", dateFilter);
        model.addAttribute("doctors", scheduleService.getAllDoctors());
        model.addAttribute("rooms", scheduleService.getAllRooms());
        return "admin/schedule/list"; // Sửa ở đây để khớp với admin/schedule/list.html
    }

    @PostMapping("/save")
    public String saveSchedule(@RequestParam("adminId") Integer adminId,
                               @ModelAttribute Schedule schedule) {
        scheduleService.saveSchedule(schedule);
        return "redirect:/admin/schedules?adminId=" + adminId;
    }

    @PostMapping("/{id}/delete")
    public String deleteSchedule(@PathVariable("id") Integer scheduleId,
                                 @RequestParam("adminId") Integer adminId) {
        scheduleService.deleteSchedule(scheduleId);
        return "redirect:/admin/schedules?adminId=" + adminId;
    }
}