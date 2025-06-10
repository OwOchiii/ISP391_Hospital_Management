package orochi.service;

import orochi.model.Appointment;

public interface AppointmentMetricService {
    Integer getTotalAppointments();
    Integer getTodayAppointments();
    Integer getPendingAppointments();
    Integer getGrowthPercentage();

    // Additional methods needed by AdminAppointmentController
    Integer getTotalAppointments(String status);
    Long getAppointmentsByStatus(String status);
}
