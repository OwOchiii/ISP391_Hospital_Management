package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.repository.AppointmentRepository;
import orochi.service.AppointmentMetricService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class AppointmentMetricServiceImpl implements AppointmentMetricService {

    private final AppointmentRepository appointmentRepository;

    @Autowired
    public AppointmentMetricServiceImpl(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public Integer getTotalAppointments() {
        try {
            return Math.toIntExact(appointmentRepository.count());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getTodayAppointments() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusSeconds(1);

            // Assuming you'll add a method to find appointments by date range
            // If not available, you may need to add this method to the repository
            long count = appointmentRepository.findAll().stream()
                .filter(a -> a.getDateTime() != null &&
                       !a.getDateTime().isBefore(startOfDay) &&
                       !a.getDateTime().isAfter(endOfDay))
                .count();
            return Math.toIntExact(count);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getPendingAppointments() {
        try {
            // Assuming your Appointment entity has a status field and "pending" is a valid status
            long count = appointmentRepository.findAll().stream()
                .filter(a -> "pending".equalsIgnoreCase(a.getStatus()))
                .count();
            return Math.toIntExact(count);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getGrowthPercentage() {
        try {
            // Calculate growth by comparing current month appointments with previous month
            YearMonth currentMonth = YearMonth.now();
            YearMonth previousMonth = currentMonth.minusMonths(1);

            LocalDateTime currentMonthStart = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime currentMonthEnd = currentMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusSeconds(1);

            LocalDateTime previousMonthStart = previousMonth.atDay(1).atStartOfDay();
            LocalDateTime previousMonthEnd = previousMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusSeconds(1);

            long currentMonthCount = appointmentRepository.findAll().stream()
                .filter(a -> a.getDateTime() != null &&
                       !a.getDateTime().isBefore(currentMonthStart) &&
                       !a.getDateTime().isAfter(currentMonthEnd))
                .count();

            long previousMonthCount = appointmentRepository.findAll().stream()
                .filter(a -> a.getDateTime() != null &&
                       !a.getDateTime().isBefore(previousMonthStart) &&
                       !a.getDateTime().isAfter(previousMonthEnd))
                .count();

            if (previousMonthCount == 0) {
                return currentMonthCount > 0 ? 100 : 0;
            }

            return (int) ((currentMonthCount - previousMonthCount) * 100 / previousMonthCount);
        } catch (Exception e) {
            return 0;
        }
    }
}
