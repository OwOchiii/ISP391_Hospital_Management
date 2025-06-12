package orochi.controller;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import orochi.dto.ScheduleDTO;
import orochi.model.Doctor;
import orochi.model.Patient;
import orochi.model.Room;
import orochi.repository.PatientRepository;
import orochi.repository.RoomRepository;
import orochi.service.ScheduleService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/doctor")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PatientRepository patientRepository;

    /**
     * Display the doctor's schedule page
     */
    @GetMapping("/schedule")
    public String viewSchedule(
            @RequestParam Integer doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Model model) {

        // If weekStart is not provided, use current week's Monday
        if (weekStart == null) {
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        // Get schedule data for the week
        List<ScheduleDTO> schedules = scheduleService.getDoctorScheduleForWeek(doctorId, weekStart);

        // Get schedule statistics
        ScheduleService.ScheduleStatistics stats = scheduleService.getDoctorScheduleStatistics(doctorId, weekStart);

        // Format week date range
        String currentWeek = scheduleService.formatWeekDateRange(weekStart);

        // Add data to model
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("currentWeek", currentWeek);
        model.addAttribute("weeklyAppointments", stats.getWeeklyAppointments());
        model.addAttribute("onCallHours", stats.getOnCallHours());
        model.addAttribute("roomsAssigned", stats.getRoomsAssigned());
        model.addAttribute("totalHours", stats.getTotalHours());
        model.addAttribute("schedules", schedules);
        model.addAttribute("weekStart", weekStart);

        // Get all rooms and patients for the dropdowns
        List<Room> rooms = roomRepository.findAll();
        List<Patient> patients = patientRepository.findAll();
        model.addAttribute("rooms", rooms);
        model.addAttribute("patients", patients);

        return "doctor/schedule";
    }

    /**
     * Handle AJAX request to navigate between weeks
     */
    @GetMapping("/schedule/week")
    @ResponseBody
    public ScheduleWeekResponse getWeekSchedule(
            @RequestParam Integer doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        // Get schedule data for the week
        List<ScheduleDTO> schedules = scheduleService.getDoctorScheduleForWeek(doctorId, weekStart);

        // Get schedule statistics
        ScheduleService.ScheduleStatistics stats = scheduleService.getDoctorScheduleStatistics(doctorId, weekStart);

        // Format week date range
        String currentWeek = scheduleService.formatWeekDateRange(weekStart);

        // Create response object
        return new ScheduleWeekResponse(
                currentWeek,
                stats.getWeeklyAppointments(),
                stats.getOnCallHours(),
                stats.getRoomsAssigned(),
                stats.getTotalHours(),
                schedules
        );
    }

    /**
     * Handle AJAX request to get a specific day's schedule
     */
    @GetMapping("/schedule/day")
    @ResponseBody
    public ScheduleDayResponse getDaySchedule(
            @RequestParam Integer doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // Get schedule data for the day
        List<ScheduleDTO> schedules = scheduleService.getDoctorScheduleForDay(doctorId, date);

        // Create response object
        return new ScheduleDayResponse(schedules);
    }

    /**
     * Save a new schedule or update an existing one
     */
    @PostMapping("/schedule/save")
    @ResponseBody
    public ResponseEntity<?> saveSchedule(@RequestBody ScheduleDTO scheduleDTO) {
        try {
            ScheduleDTO savedSchedule = scheduleService.saveSchedule(scheduleDTO);
            return ResponseEntity.ok(new ScheduleSaveResponse(true, "Schedule saved successfully", savedSchedule));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ScheduleSaveResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Delete a schedule
     */
    @DeleteMapping("/schedule/{scheduleId}")
    @ResponseBody
    public void deleteSchedule(@PathVariable Integer scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
    }

    /**
     * Get a schedule by ID
     */
    @GetMapping("/schedule/{scheduleId}")
    @ResponseBody
    public ScheduleDTO getSchedule(@PathVariable Integer scheduleId) {
        return scheduleService.getScheduleById(scheduleId);
    }

    /**
     * Get all rooms for dropdown
     */
    @GetMapping("/rooms")
    @ResponseBody
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * Get all patients for dropdown
     */
    @GetMapping("/schedule/patients")
    @ResponseBody
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    /**
     * View schedule details for a specific entry
     */
    @GetMapping("/schedule-details/{scheduleId}")
    public String viewScheduleDetails(
            @PathVariable Integer scheduleId,
            @RequestParam Integer doctorId,
            Model model) {

        ScheduleDTO workEntry = scheduleService.getScheduleById(scheduleId);

        // Add data to model
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("workEntry", workEntry);

        return "doctor/schedule-details";
    }

    /**
     * Toggle completion status of a schedule entry
     */
    @PostMapping("/api/schedule/{scheduleId}/toggle-completed")
    @ResponseBody
    public ResponseEntity<?> toggleCompleted(
            @PathVariable Integer scheduleId,
            @RequestBody Map<String, Integer> requestBody) {

        Integer doctorId = requestBody.get("doctorId");

        try {
            boolean isCompleted = scheduleService.toggleScheduleCompletion(scheduleId, doctorId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("completed", isCompleted);
            response.put("message", isCompleted ? "Schedule marked as completed" : "Schedule marked as incomplete");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Toggle completion status of an appointment
     */
    @PostMapping("/api/appointments/{appointmentId}/toggle-completed")
    @ResponseBody
    public ResponseEntity<?> toggleAppointmentCompleted(
            @PathVariable Integer appointmentId,
            @RequestBody Map<String, Integer> requestBody) {

        Integer doctorId = requestBody.get("doctorId");

        try {
            boolean isCompleted = scheduleService.toggleAppointmentCompletion(appointmentId, doctorId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("completed", isCompleted);
            response.put("message", isCompleted ? "Appointment marked as completed" : "Appointment marked as incomplete");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }
    /**
     * Inner class to represent the week schedule response
     */
    @Getter
    private static class ScheduleWeekResponse {
        private final String currentWeek;
        private final int weeklyAppointments;
        private final int onCallHours;
        private final int roomsAssigned;
        private final int totalHours;
        private final List<ScheduleDTO> schedules;

        public ScheduleWeekResponse(String currentWeek, int weeklyAppointments, int onCallHours, int roomsAssigned,
                                   int totalHours, List<ScheduleDTO> schedules) {
            this.currentWeek = currentWeek;
            this.weeklyAppointments = weeklyAppointments;
            this.onCallHours = onCallHours;
            this.roomsAssigned = roomsAssigned;
            this.totalHours = totalHours;
            this.schedules = schedules;
        }
    }

    /**
     * Inner class to represent the day schedule response
     */
    @Getter
    private static class ScheduleDayResponse {
        private final List<ScheduleDTO> schedules;

        public ScheduleDayResponse(List<ScheduleDTO> schedules) {
            this.schedules = schedules;
        }
    }

    /**
     * Inner class to represent the schedule save response
     */
    @Getter
    private static class ScheduleSaveResponse {
        private final boolean success;
        private final String message;
        private final ScheduleDTO schedule;

        public ScheduleSaveResponse(boolean success, String message, ScheduleDTO schedule) {
            this.success = success;
            this.message = message;
            this.schedule = schedule;
        }
    }

    /**
     * Inner class to represent week information for the dropdown
     */
    @Getter
    public static class WeekInfo {
        private final String startDate;
        private final String endDate;
        private final String label;

        public WeekInfo(String startDate, String endDate, String label) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.label = label;
        }
    }
}
