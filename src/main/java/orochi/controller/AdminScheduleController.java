package orochi.controller;

import orochi.dto.ScheduleDTO;
import orochi.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin/schedules")
public class AdminScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping
    public String showSchedules(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value="scheduleId", required=false) Integer scheduleId,
            @RequestParam(value="eventType",  required=false) String  eventType,
            @RequestParam(value="roomId",     required=false) Integer roomId,
            @RequestParam(value="startTime",  required=false) String  startTimeStr,
            @RequestParam(value="endTime",    required=false) String  endTimeStr,
            @RequestParam(value="startDate",  required=false) String  startDateStr,
            @RequestParam(value="endDate",    required=false) String  endDateStr,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="6") int size,
            Model model) {

        // parse params
        LocalDate startDate = StringUtils.hasText(startDateStr) ? LocalDate.parse(startDateStr) : null;
        LocalDate endDate   = StringUtils.hasText(endDateStr)   ? LocalDate.parse(endDateStr)   : null;
        LocalTime startTime = StringUtils.hasText(startTimeStr) ? LocalTime.parse(startTimeStr) : null;
        LocalTime endTime   = StringUtils.hasText(endTimeStr)   ? LocalTime.parse(endTimeStr)   : null;

        // gọi service duy nhất với Specification + pagination
        Page<ScheduleDTO> pageResult = scheduleService.findSchedulesFiltered(
                scheduleId, eventType, roomId,
                startTime, endTime,
                startDate, endDate,
                page, size
        );

        List<ScheduleDTO> schedules = pageResult.getContent();

        // đẩy data lên view
        model.addAttribute("schedules",   schedules);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  pageResult.getTotalPages());
        model.addAttribute("pageSize",    size);

        // giữ lại giá trị filter trên form
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("eventType",  eventType);
        model.addAttribute("roomId",     roomId);
        model.addAttribute("startDate",  startDateStr);
        model.addAttribute("endDate",    endDateStr);
        model.addAttribute("startTime",  startTimeStr);
        model.addAttribute("endTime",    endTimeStr);
        model.addAttribute("adminId",    adminId);

        // danh sách hỗ trợ cho dropdown trong modal
        model.addAttribute("rooms",        scheduleService.getAllRooms());
        model.addAttribute("doctors",      scheduleService.getAllDoctors());
        model.addAttribute("appointments", scheduleService.getAllAppointments());
        model.addAttribute("patients",     scheduleService.getAllPatients());

        return "admin/schedule/list";
    }


    @GetMapping("/{id}/delete")
    public String deleteSchedule(@PathVariable Integer id,
                                 @RequestParam("adminId") Integer adminId) {
        scheduleService.deleteSchedule(id);
        return "redirect:/admin/schedules?adminId=" + adminId + "&page=0&size=6";
    }

    @PostMapping("/save")
    public String saveSchedule(@ModelAttribute ScheduleDTO dto,
                               @RequestParam("adminId") Integer adminId) {
        scheduleService.saveSchedule(dto);
        return "redirect:/admin/schedules?adminId=" + adminId + "&page=0&size=6";
    }
}
