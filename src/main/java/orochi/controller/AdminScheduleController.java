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
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "6") int size,
                                Model model) {
        System.out.println("Accessed /admin/schedules with adminId: " + adminId + ", page: " + page + ", size: " + size);
        LocalDate startDate = null;
        LocalDate endDate = null;

        // Kiểm tra và phân tích startDateStr
        if (startDateStr != null && !startDateStr.isEmpty()) {
            startDate = LocalDate.parse(startDateStr);
        }
        // Kiểm tra và phân tích endDateStr
        if (endDateStr != null && !endDateStr.isEmpty()) {
            endDate = LocalDate.parse(endDateStr);
        }

        List<ScheduleDTO> schedules = scheduleService.searchSchedulesPaginated(search, startDate, endDate, page, size);
        long totalSchedules = scheduleService.countSchedules(search, startDate, endDate);
        int totalPages = (int) Math.ceil((double) totalSchedules / size);

        System.out.println("Total Schedules: " + totalSchedules);
        System.out.println("Total Pages: " + totalPages);
        System.out.println("Current Page: " + page);
        System.out.println("Page Size: " + size);

        model.addAttribute("schedules", schedules);
        model.addAttribute("adminId", adminId);
        model.addAttribute("search", search);
        model.addAttribute("startDate", startDateStr);
        model.addAttribute("endDate", endDateStr);
        model.addAttribute("doctors", scheduleService.getAllDoctors());
        model.addAttribute("rooms", scheduleService.getAllRooms());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        return "admin/schedule/list";
    }

    @GetMapping("/{id}/delete")
    public String deleteSchedule(@PathVariable Integer id, @RequestParam("adminId") Integer adminId, Model model) {
        scheduleService.deleteSchedule(id);
        return "redirect:/admin/schedules?adminId=" + adminId + "&page=0&size=6";
    }

    @PostMapping("/save")
    public String saveSchedule(@ModelAttribute ScheduleDTO scheduleDTO, @RequestParam("adminId") Integer adminId, Model model) {
        scheduleService.saveSchedule(scheduleDTO);
        return "redirect:/admin/schedules?adminId=" + adminId + "&page=0&size=6";
    }
}