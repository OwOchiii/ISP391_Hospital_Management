package orochi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentMetricServiceImpl implements AppointmentMetricService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentService appointmentService;

    // Status constants
    public static final String STATUS_SCHEDULED = "Scheduled";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELLED = "Cancel";
    public static final String STATUS_PENDING   = "Pending";

    @Override
    public Integer getTotalAppointments() {
        return appointmentRepository.findAll().size();
    }

    @Override
    public Integer getTodayAppointments() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end   = LocalDate.now().atTime(23, 59, 59);
        return Math.toIntExact(appointmentRepository.countByDateTimeBetween(start, end));
    }

    @Override
    public Integer getPendingAppointments() {
        return Math.toIntExact(appointmentRepository.countByStatus(STATUS_PENDING));
    }

    @Override
    public Integer getGrowthPercentage() {
        // placeholder
        return 15;
    }

    @Override
    public Integer getTotalAppointments(String status) {
        return Math.toIntExact(appointmentRepository.countByStatus(status));
    }

    @Override
    public Long getAppointmentsByStatus(String status) {
        return appointmentRepository.countByStatus(status);
    }

    @Override
    public long countBetween(LocalDate from, LocalDate to,
                             Integer doctorId, Integer specId,
                             String status, String search) {
        // hiện tại chỉ count theo khoảng ngày, bạn có thể mở rộng filter doctor/spec/status/search
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(23,59,59);
        return appointmentRepository.countByDateTimeBetween(start, end);
    }

    @Override
    public long countStatusBetween(String status,
                                   LocalDate from, LocalDate to,
                                   Integer doctorId, Integer specId,
                                   String search) {
        // hiện tại chỉ count theo status, bạn có thể mở rộng filter doctor/spec/search
        return appointmentRepository.countByStatus(status);
    }

    @Override
    public Map<String, Long> periodCounts(
            LocalDate from, LocalDate to,
            Integer doctorId, Integer specId,
            String status, String groupBy) {

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(23, 59, 59);

        List<Object[]> raw;
        switch(groupBy.toLowerCase()) {
            case "quarter":
                raw = "ALL".equalsIgnoreCase(status)
                        ? appointmentRepository.findQuarterlyCounts(start, end, status)
                        : appointmentRepository.findQuarterlyCountsByStatus(start, end, status);
                break;
            case "year":
                raw = "ALL".equalsIgnoreCase(status)
                        ? appointmentRepository.findYearlyCounts(start, end, status)
                        : appointmentRepository.findYearlyCountsByStatus(start, end, status);
                break;
            default: // month
                raw = appointmentRepository.findMonthlyCounts(start, end, status);
        }

        return raw.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> ((Number) row[1]).longValue(),
                        (a,b) -> b,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Long> periodCountsByStatus(
            String status,
            LocalDate from, LocalDate to,
            Integer doctorId, Integer specId,
            String groupBy) {
        // chỉ cần delegate lại
        return periodCounts(from, to, doctorId, specId, status, groupBy);
    }

    @Override
    public Map<String, Long> monthlyCounts(
            LocalDate from, LocalDate to,
            Integer doctorId, Integer specId,
            String status, String search
    ) {
        return appointmentService.fetchPeriodCounts2(
                from, to, doctorId, specId, status, "month"
        );
    }


    @Override
    public Map<String, Long> monthlyCountsByStatus(
            String status,
            LocalDate from, LocalDate to,
            Integer doctorId, Integer specId,
            String search
    ) {
        return appointmentService.fetchPeriodCounts2(
                from, to, doctorId, specId, status, "month"
        );
    }
}
