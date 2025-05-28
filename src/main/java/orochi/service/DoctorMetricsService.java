package orochi.service;

public interface DoctorMetricsService {
    Integer getTotalDoctors();
    Integer getActiveDoctors();
    Integer getNewDoctorsToday();
    Integer getGrowthPercentage();

    // Additional methods specific to doctor metrics can be added here
}
