package orochi.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import orochi.model.Receipt;

@Repository
public interface RevenueRepository extends JpaRepository<Receipt, Integer> {

    // 2. Tổng doanh thu trong khoảng ngày
    @Query(value = """
      SELECT COALESCE(SUM(r.TotalAmount),0)
      FROM Receipt r
      JOIN [Transaction] t ON r.TransactionID = t.TransactionID
      WHERE t.Status = 'Paid'
        AND CAST(r.IssuedDate AS date) BETWEEN :from AND :to
      """, nativeQuery = true)
    Double getTotalRevenue(@Param("from") LocalDate from,
                           @Param("to")   LocalDate to);

    // 3. Doanh thu theo khoa
    @Query(value = """
      SELECT d.DeptName as dept, COALESCE(SUM(r.TotalAmount),0) as revenue
      FROM Receipt r
      JOIN [Transaction] t ON r.TransactionID = t.TransactionID
      JOIN Appointment a ON t.AppointmentID = a.AppointmentID
      JOIN Room rm ON a.RoomID = rm.RoomID
      JOIN Department d ON rm.DepartmentID = d.DepartmentID
      WHERE t.Status = 'Paid'
        AND CAST(r.IssuedDate AS date) BETWEEN :from AND :to
      GROUP BY d.DeptName
      """, nativeQuery = true)
    List<Map<String,Object>> getRevenueByDepartment(@Param("from") LocalDate from,
                                                    @Param("to")   LocalDate to);

    // 4. Doanh thu theo bác sĩ
    @Query(value = """
      SELECT u.FullName as doctor, COALESCE(SUM(r.TotalAmount),0) as revenue
      FROM Receipt r
      JOIN [Transaction] t ON r.TransactionID = t.TransactionID
      JOIN Appointment a ON t.AppointmentID = a.AppointmentID
      JOIN Doctor d ON a.DoctorID = d.DoctorID
      JOIN Users u ON d.UserID = u.UserID
      WHERE t.Status = 'Paid'
        AND CAST(r.IssuedDate AS date) BETWEEN :from AND :to
      GROUP BY u.FullName
      """, nativeQuery = true)
    List<Map<String,Object>> getRevenueByDoctor(@Param("from") LocalDate from,
                                                @Param("to")   LocalDate to);

    // 5. So sánh doanh thu theo tháng (trong 1 năm)
    @Query(value = """
      SELECT MONTH(r.IssuedDate) as month, COALESCE(SUM(r.TotalAmount),0) as total
      FROM Receipt r
      JOIN [Transaction] t ON r.TransactionID = t.TransactionID
      WHERE t.Status = 'Paid'
        AND YEAR(r.IssuedDate) = :year
      GROUP BY MONTH(r.IssuedDate)
      ORDER BY MONTH(r.IssuedDate)
      """, nativeQuery = true)
    List<Map<String,Object>> getMonthlyComparison(@Param("year") int year);

    // 6. So sánh doanh thu theo quý (trong 1 năm)
    @Query(value = """
      SELECT ((MONTH(r.IssuedDate)-1)/3+1) as quarter,
             COALESCE(SUM(r.TotalAmount),0) as total
      FROM Receipt r
      JOIN [Transaction] t ON r.TransactionID = t.TransactionID
      WHERE t.Status = 'Paid'
        AND YEAR(r.IssuedDate) = :year
      GROUP BY ((MONTH(r.IssuedDate)-1)/3+1)
      ORDER BY quarter
      """, nativeQuery = true)
    List<Map<String,Object>> getQuarterlyComparison(@Param("year") int year);

    // 7. Chi tiết từng khoản thu (Receipt list)
    @Query(value = """
      SELECT
        r.ReceiptID    as receiptId,
        CAST(r.IssuedDate AS date) as date,
        r.TotalAmount  as amount,
        t.Status       as status,
        uP.FullName    as patient,
        uD.FullName    as doctor,
        d.DeptName     as department
      FROM Receipt r
      JOIN [Transaction] t ON r.TransactionID = t.TransactionID
      LEFT JOIN Appointment a ON t.AppointmentID = a.AppointmentID
      LEFT JOIN Patient p ON a.PatientID = p.PatientID
      LEFT JOIN Users uP ON p.UserID = uP.UserID
      LEFT JOIN Doctor dC ON a.DoctorID = dC.DoctorID
      LEFT JOIN Users uD ON dC.UserID = uD.UserID
      LEFT JOIN Room rm ON a.RoomID = rm.RoomID
      LEFT JOIN Department d ON rm.DepartmentID = d.DepartmentID
      WHERE CAST(r.IssuedDate AS date) BETWEEN :from AND :to
      ORDER BY r.IssuedDate DESC
      """, nativeQuery = true)
    List<Map<String,Object>> getReceiptDetails(@Param("from") LocalDate from,
                                               @Param("to")   LocalDate to);
}


