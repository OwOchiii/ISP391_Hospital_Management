package orochi.controller;

import orochi.dto.ScheduleDTO;
import orochi.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin")
public class AdminScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    /**
     * Hiển thị danh sách lịch với filter và phân trang
     */
    @GetMapping("/schedules")
    public String showSchedules(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(name = "scheduleId", required = false) Integer scheduleId,
            @RequestParam(name = "eventType",  required = false) String  eventType,
            @RequestParam(name = "roomId",     required = false) Integer roomId,
            @RequestParam(name = "startDate",  required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate",    required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "startTime",  required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(name = "endTime",    required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "6") int size,
            Model model
    ) {

        if (!StringUtils.hasText(eventType) || "All".equalsIgnoreCase(eventType.trim())) {
            eventType = null;
        }
        if (roomId != null && roomId < 0) {
            roomId = null;
        }

        Page<ScheduleDTO> schedulesPage = scheduleService.findSchedulesFiltered(
                scheduleId, eventType, roomId,
                startDate, endDate,
                startTime, endTime,
                page, size
        );

        model.addAttribute("schedules",   schedulesPage.getContent());
        model.addAttribute("totalPages",  schedulesPage.getTotalPages());
        model.addAttribute("currentPage", schedulesPage.getNumber());
        model.addAttribute("pageSize",    schedulesPage.getSize());

        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("eventType",  eventType);
        model.addAttribute("roomId",     roomId);
        model.addAttribute("startDate",  startDate);
        model.addAttribute("endDate",    endDate);
        model.addAttribute("startTime",  startTime);
        model.addAttribute("endTime",    endTime);

        model.addAttribute("doctors",      scheduleService.getAllDoctors());
        model.addAttribute("rooms",        scheduleService.getAllRooms());
        model.addAttribute("appointments", scheduleService.getAllAppointments());
        model.addAttribute("patients",     scheduleService.getAllPatients());
        model.addAttribute("adminId",      adminId);

        return "admin/schedule/list";
    }

    @PostMapping("/schedules/save")
    public String saveSchedule(
            @ModelAttribute ScheduleDTO dto,
            @RequestParam("adminId") Integer adminId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "6") int size
    ) {
        // Lưu schedule
        scheduleService.saveSchedule(dto);
        return "redirect:/admin/schedules?"
                + "adminId=" + adminId
                + "&page="   + page
                + "&size="   + size
                + "#schedule-" + dto.getScheduleId();
    }



    @PostMapping("/schedules/{id}/delete")
    public String deleteSchedule(
            @PathVariable("id") Integer id,
            @RequestParam("adminId") Integer adminId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "6") int size
    ) {
        scheduleService.deleteSchedule(id);
        return "redirect:/admin/schedules?adminId=" + adminId
                + "&page=" + page
                + "&size=" + size;
    }

}
