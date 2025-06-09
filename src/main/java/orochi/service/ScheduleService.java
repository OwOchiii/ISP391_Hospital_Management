package orochi.service;

import orochi.dto.ScheduleDTO;
import orochi.model.Doctor;
import orochi.model.Room;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    /**
     * Get all schedules for a doctor within a specified week
     */
    List<ScheduleDTO> getDoctorScheduleForWeek(Integer doctorId, LocalDate weekStart);

    /**
     * Get all schedules for a doctor on a specific day
     */
    List<ScheduleDTO> getDoctorScheduleForDay(Integer doctorId, LocalDate date);

    /**
     * Get schedule statistics for a doctor within a specified week
     */
    ScheduleStatistics getDoctorScheduleStatistics(Integer doctorId, LocalDate weekStart);

    /**
     * Save a new schedule or update existing one
     */
    ScheduleDTO saveSchedule(ScheduleDTO scheduleDTO);

    /**
     * Delete a schedule by ID
     */
    void deleteSchedule(Integer scheduleId);

    /**
     * Get a single schedule by ID
     */
    ScheduleDTO getScheduleById(Integer scheduleId);

    /**
     * Get all schedules
     */
    List<ScheduleDTO> getAllSchedules();

    /**
     * Search schedules by keyword and date range
     */
    List<ScheduleDTO> searchSchedules(String keyword, LocalDate startDate, LocalDate endDate);

    /**
     * Get all doctors
     */
    List<Doctor> getAllDoctors();

    /**
     * Get all rooms
     */
    List<Room> getAllRooms();

    /**
     * Format a date range for display (e.g., "Jan 15 - Jan 21, 2024")
     */
    String formatWeekDateRange(LocalDate startDate);

    /**
     * Inner class for schedule statistics
     */
    class ScheduleStatistics {
        private final Integer weeklyAppointments;
        private final Integer onCallHours;
        private final Integer roomsAssigned;
        private final Integer totalHours;

        public ScheduleStatistics(Integer weeklyAppointments, Integer onCallHours, Integer roomsAssigned, Integer totalHours) {
            this.weeklyAppointments = weeklyAppointments;
            this.onCallHours = onCallHours;
            this.roomsAssigned = roomsAssigned;
            this.totalHours = totalHours;
        }

        public Integer getWeeklyAppointments() {
            return weeklyAppointments;
        }

        public Integer getOnCallHours() {
            return onCallHours;
        }

        public Integer getRoomsAssigned() {
            return roomsAssigned;
        }

        public Integer getTotalHours() {
            return totalHours;
        }
    }
}
