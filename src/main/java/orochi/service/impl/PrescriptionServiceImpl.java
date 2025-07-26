package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.repository.PrescriptionRepository;
import orochi.service.PrescriptionService;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Override
    public Integer getMonthlyCount() {
        try {
            // Calculate the start and end of current month
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

            // Get actual count from database
            Long count = prescriptionRepository.countByPrescriptionDateBetween(startOfMonth, endOfMonth);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
