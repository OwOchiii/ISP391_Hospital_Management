package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.repository.RevenueRepository;
import orochi.service.RevenueService;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

@Service
public class RevenueServiceImpl implements RevenueService {

    @Override
    public Double getMonthlyRevenue() {
        try {
            return 150000.00;
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public Integer getGrowthPercentage() {
        try {
            return 15;
        } catch (Exception e) {
            return 0;
        }
    }
    private final RevenueRepository repo;

    @Autowired
    public RevenueServiceImpl(RevenueRepository repo) {
        this.repo = repo;
    }

    @Override
    public Double getTotalRevenue(LocalDate from, LocalDate to) {
        return repo.getTotalRevenue(from, to);
    }

    @Override
    public List<Map<String, Object>> getRevenueByDepartment(LocalDate from, LocalDate to) {
        return repo.getRevenueByDepartment(from, to);
    }

    @Override
    public List<Map<String, Object>> getRevenueByDoctor(LocalDate from, LocalDate to) {
        return repo.getRevenueByDoctor(from, to);
    }

    @Override
    public List<Map<String, Object>> getMonthlyComparison(int year) {
        return repo.getMonthlyComparison(year);
    }

    @Override
    public List<Map<String, Object>> getQuarterlyComparison(int year) {
        return repo.getQuarterlyComparison(year);
    }

    @Override
    public List<Map<String, Object>> getReceiptDetails(LocalDate from, LocalDate to) {
        return repo.getReceiptDetails(from, to);
    }

    @Override
    public int computePeriodGrowth(LocalDate from, LocalDate to) {
        // Độ dài kỳ
        Period p = Period.between(from, to);
        LocalDate prevTo   = from.minusDays(1);
        LocalDate prevFrom = prevTo.minus(p).plusDays(1);

        double current = repo.getTotalRevenue(from, to);
        double previous = repo.getTotalRevenue(prevFrom, prevTo);

        if (previous == 0) return current == 0 ? 0 : 100;
        return (int) Math.round((current - previous) / previous * 100);
    }

    // --- Mới: tính % tăng/giảm so với cùng kỳ năm trước ---
    @Override
    public int computeYearlyGrowth(LocalDate from, LocalDate to) {
        LocalDate lastYearFrom = from.minusYears(1);
        LocalDate lastYearTo   = to.minusYears(1);

        double current = repo.getTotalRevenue(from, to);
        double previous = repo.getTotalRevenue(lastYearFrom, lastYearTo);

        if (previous == 0) return current == 0 ? 0 : 100;
        return (int) Math.round((current - previous) / previous * 100);
    }
}
