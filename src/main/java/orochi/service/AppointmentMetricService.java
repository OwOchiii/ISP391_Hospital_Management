package orochi.service;

public interface AppointmentMetricService {
    Integer getTotalAppointments();
    Integer getTodayAppointments();
    Integer getPendingAppointments();
    Integer getGrowthPercentage();
}
