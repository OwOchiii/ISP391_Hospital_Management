package orochi.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface RevenueService {
    Double getMonthlyRevenue();
    Integer getGrowthPercentage();

    Double getTotalRevenue(LocalDate from, LocalDate to);
    List<Map<String,Object>> getRevenueByDepartment(LocalDate from, LocalDate to);
    List<Map<String,Object>> getRevenueByDoctor(LocalDate from, LocalDate to);
    List<Map<String,Object>> getMonthlyComparison(int year);
    List<Map<String,Object>> getQuarterlyComparison(int year);
    List<Map<String,Object>> getReceiptDetails(LocalDate from, LocalDate to);

    int computePeriodGrowth(LocalDate from, LocalDate to);
    int computeYearlyGrowth(LocalDate from, LocalDate to);
}
