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
     * Toggle the completion status of a schedule
     * @return the new completion status (true if completed, false if not completed)
     */
    boolean toggleScheduleCompletion(Integer scheduleId, Integer doctorId);

    /**
     * Toggle the completion status of an appointment
     * @return the new completion status (true if completed, false if not completed)
     */
    boolean toggleAppointmentCompletion(Integer appointmentId, Integer doctorId);

    /**
     * Format a date range for display (e.g., "Jan 15 - Jan 21, 2024")
     */
    String formatWeekDateRange(LocalDate startDate);

    /**
     * Inner class for schedule statistics
     */
    class ScheduleStatistics {
        private int weeklyAppointments;
        private int onCallHours;
        private int roomsAssigned;
        private int totalHours;

        public int getWeeklyAppointments() {
            return weeklyAppointments;
        }

        public void setWeeklyAppointments(int weeklyAppointments) {
            this.weeklyAppointments = weeklyAppointments;
        }

        public int getOnCallHours() {
            return onCallHours;
        }

        public void setOnCallHours(int onCallHours) {
            this.onCallHours = onCallHours;
        }

        public int getRoomsAssigned() {
            return roomsAssigned;
        }

        public void setRoomsAssigned(int roomsAssigned) {
            this.roomsAssigned = roomsAssigned;
        }

        public int getTotalHours() {
            return totalHours;
        }

        public void setTotalHours(int totalHours) {
            this.totalHours = totalHours;
        }
    }
}

