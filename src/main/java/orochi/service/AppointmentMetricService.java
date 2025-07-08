package orochi.service;

import orochi.model.Appointment;

import java.time.LocalDate;
import java.util.Map;

public interface AppointmentMetricService {
    Integer getTotalAppointments();
    Integer getTodayAppointments();
    Integer getPendingAppointments();
    Integer getGrowthPercentage();

    // Additional methods needed by AdminAppointmentController
    Integer getTotalAppointments(String status);
    Long getAppointmentsByStatus(String status);

    long countBetween(LocalDate from, LocalDate to,
                      Integer doctorId, Integer specId,
                      String status, String search);
    long countStatusBetween(String status,
                            LocalDate from, LocalDate to,
                            Integer doctorId, Integer specId,
                            String search);
    Map<String, Long> periodCounts(
            LocalDate from, LocalDate to,
            Integer doctorId, Integer specId,
            String status, String groupBy
    );

    /**
     * Thống kê theo period nhưng chỉ với một status cố định
     */
    Map<String, Long> periodCountsByStatus(
            String status,
            LocalDate from, LocalDate to,
            Integer doctorId, Integer specId,
            String groupBy
    );

    Map<String, Long> monthlyCounts(
            LocalDate from, LocalDate to,
            Integer doctorId, Integer specId,
            String status, String search
    );

    Map<String, Long> monthlyCountsByStatus(
            String status,
            LocalDate from, LocalDate to,
            Integer doctorId, Integer specId,
            String search
    );
}
