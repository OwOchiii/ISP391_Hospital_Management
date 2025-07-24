package orochi.controller;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.dto.ScheduleDTO;
import orochi.model.Doctor;
import orochi.model.Schedule;
import orochi.repository.DoctorRepository;
import orochi.repository.ScheduleRepository;
import orochi.service.RoomDoctorAssignmentService;
import orochi.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin/schedules")
public class AdminScheduleController {

    @Autowired
    private RoomDoctorAssignmentService roomDoctorAssignmentService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

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
        LocalDate today    = LocalDate.now();
        LocalDate maxDate  = today.plusDays(7);

        model.addAttribute("minDate", today);
        model.addAttribute("maxDate", maxDate);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("eventType",  eventType);
        model.addAttribute("roomId",     roomId);
        model.addAttribute("startDate",  startDateStr);
        model.addAttribute("endDate",    endDateStr);
//        model.addAttribute("startTime",  startTimeStr);
//        model.addAttribute("endTime",    endTimeStr);
        model.addAttribute("adminId",    adminId);

        // danh sách hỗ trợ cho dropdown trong modal
        model.addAttribute("rooms",        scheduleService.getAllRooms());
        model.addAttribute("doctors",      scheduleService.getAllDoctors());
        model.addAttribute("appointments", scheduleService.getAllAppointments());
        model.addAttribute("patients",     scheduleService.getAllPatients());

        return "admin/schedule/list";
    }


    @PostMapping("/{id}/delete")
    public String deleteSchedule(@PathVariable Integer id,
                                 @RequestParam("adminId") Integer adminId) {
        scheduleService.deleteSchedule(id);
        return "redirect:/admin/schedules?adminId=" + adminId + "&page=0&size=6";
    }


    @PostMapping("/save")
    public String saveSchedule(@ModelAttribute ScheduleDTO dto,
                               @RequestParam("adminId") Integer adminId,
                               RedirectAttributes ra) {
        // * Validate ngày tương lai *
        if (dto.getScheduleDate().isBefore(LocalDate.now())) {
            ra.addFlashAttribute("error", "Schedule date must be today or later.");
            return "redirect:/admin/schedules?adminId=" + adminId;
        }

        // * Validate không được thêm 2 ca cùng phòng/ngày/ca sáng-chiều *
        LocalTime shiftStart = dto.getStartTime();

        // 1) Lấy tất cả lịch cùng phòng + cùng ngày
        List<Schedule> allOnThatDay = scheduleRepository
                .findByRoomIdAndScheduleDate(dto.getRoomId(), dto.getScheduleDate());

        // 2) Lọc ra chỉ những lịch có cùng giờ bắt đầu
        List<Schedule> dup = allOnThatDay.stream()
                .filter(s -> s.getStartTime().equals(shiftStart))
                .collect(Collectors.toList());

        if (!dup.isEmpty()) {
            ra.addFlashAttribute("error", "This room has already been assigned for that shift on that date.");
            return "redirect:/admin/schedules?adminId=" + adminId;
        }

        scheduleService.saveSchedule(dto);
        return "redirect:/admin/schedules?adminId=" + adminId + "&page=0&size=6";
    }


    @GetMapping("/eligible-doctors")
    @ResponseBody
    public List<Map<String,Object>> getEligibleDoctors(
            @RequestParam Integer roomId,
            @RequestParam String date,     // yyyy‑MM‑dd
            @RequestParam String shift     // “morning” | “afternoon”
    ) {
        LocalDate ld = LocalDate.parse(date);
        List<Doctor> docs = roomDoctorAssignmentService
                .findEligibleDoctorsForShift(roomId, shift, ld);

        return docs.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("doctorId", d.getDoctorId());
            m.put("doctorName", d.getUser().getFullName());
            return m;
        }).toList();
    }


    @GetMapping("/check-duplicate")
    @ResponseBody
    public boolean isDuplicate(
            @RequestParam Integer roomId,
            @RequestParam String date,
            @RequestParam String shift) {

        LocalDate ld = LocalDate.parse(date);
        LocalTime shiftStart = "morning".equals(shift)
                ? LocalTime.of(8, 0)
                : LocalTime.of(13, 0);

        // Lấy tất cả lịch cùng phòng + ngày rồi kiểm tra trùng ca
        return scheduleRepository
                .findByRoomIdAndScheduleDate(roomId, ld)
                .stream()
                .anyMatch(s -> s.getStartTime().equals(shiftStart));
    }

}