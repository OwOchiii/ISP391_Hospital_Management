package orochi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AppointmentMetricServiceImpl implements AppointmentMetricService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    // Status constants that match the database constraints
    public static final String STATUS_SCHEDULED = "Scheduled";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELLED = "Cancel";
    public static final String STATUS_PENDING = "Pending";

    @Override
    public Integer getTotalAppointments() {
        return appointmentRepository.findAll().size();
    }

    @Override
    public Integer getTodayAppointments() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        return Math.toIntExact(appointmentRepository.countByDateTimeBetween(startOfDay, endOfDay));
    }

    @Override
    public Integer getPendingAppointments() {
        return Math.toIntExact(appointmentRepository.countByStatus(STATUS_PENDING));
    }

    @Override
    public Integer getGrowthPercentage() {
        // Implement your growth calculation logic here
        // This is a placeholder implementation
        return 15; // 15% growth
    }

    @Override
    public Integer getTotalAppointments(String status) {
        // Count appointments by status
        return Math.toIntExact(appointmentRepository.countByStatus(status));
    }

    @Override
    public Long getAppointmentsByStatus(String status) {
        // Get count of appointments with the given status
        return appointmentRepository.countByStatus(status);
    }
}
