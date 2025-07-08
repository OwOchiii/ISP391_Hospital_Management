package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orochi.model.Receipt;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Integer> {

    /**
     * Get payment data for current date with required fields
     * Amount từ Receipt.TotalAmount, Status từ Transaction.Status
     */
    @Query(value = """
        SELECT 
            p.PatientID as patientId,
            u.FullName as fullName,
            u.PhoneNumber as phoneNumber,
            ISNULL(r.TotalAmount, 0) as totalAmount,
            ISNULL(t.Status, 'Pending') as status,
            ISNULL(r.ReceiptID, 0) as receiptId,
            ISNULL(t.TransactionID, 0) as transactionId
        FROM Patient p
        INNER JOIN Users u ON p.UserID = u.UserID
        LEFT JOIN Appointment a ON p.PatientID = a.PatientID
        LEFT JOIN [Transaction] t ON a.AppointmentID = t.AppointmentID
        LEFT JOIN Receipt r ON t.TransactionID = r.TransactionID
        WHERE (r.ReceiptID IS NOT NULL AND CAST(r.IssuedDate AS DATE) = CAST(GETDATE() AS DATE))
           OR (r.ReceiptID IS NULL)
        ORDER BY u.FullName
        """, nativeQuery = true)
    List<Map<String, Object>> getTodaysPaymentData();

    /**
     * Get total revenue for today from Receipt.TotalAmount where Transaction.Status is completed
     */
    @Query(value = """
        SELECT COALESCE(SUM(r.TotalAmount), 0) as dailyTotal
        FROM Receipt r
        INNER JOIN [Transaction] t ON r.TransactionID = t.TransactionID
        WHERE CAST(r.IssuedDate AS DATE) = CAST(GETDATE() AS DATE)
        AND (t.Status = 'Completed' OR t.Status = 'Success')
        """, nativeQuery = true)
    Double getTodaysTotalRevenue();

    /**
     * Get payment data filtered by Transaction.Status for today
     */
    @Query(value = """
        SELECT 
            p.PatientID as patientId,
            u.FullName as fullName,
            u.PhoneNumber as phoneNumber,
            r.TotalAmount as totalAmount,
            t.Status as status,
            r.ReceiptID as receiptId,
            t.TransactionID as transactionId,
            r.IssuedDate as issuedDate
        FROM Receipt r
        INNER JOIN [Transaction] t ON r.TransactionID = t.TransactionID
        INNER JOIN Appointment a ON t.AppointmentID = a.AppointmentID
        INNER JOIN Patient p ON a.PatientID = p.PatientID
        INNER JOIN Users u ON p.UserID = u.UserID
        WHERE CAST(r.IssuedDate AS DATE) = CAST(GETDATE() AS DATE)
        AND t.Status = :status
        ORDER BY r.IssuedDate DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getTodaysPaymentDataByStatus(@Param("status") String status);

    /**
     * Get payment data for a specific date range
     */
    @Query(value = """
        SELECT 
            p.PatientID as patientId,
            u.FullName as fullName,
            u.PhoneNumber as phoneNumber,
            r.TotalAmount as totalAmount,
            t.Status as status,
            r.ReceiptID as receiptId,
            t.TransactionID as transactionId,
            r.IssuedDate as issuedDate
        FROM Receipt r
        INNER JOIN [Transaction] t ON r.TransactionID = t.TransactionID
        INNER JOIN Appointment a ON t.AppointmentID = a.AppointmentID
        INNER JOIN Patient p ON a.PatientID = p.PatientID
        INNER JOIN Users u ON p.UserID = u.UserID
        WHERE r.IssuedDate BETWEEN :startDate AND :endDate
        ORDER BY r.IssuedDate DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getPaymentDataByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get all receipts with transaction data - using the exact SQL that works
     */
    @Query(value = """
        SELECT 
            p.PatientID as patientId,
            u.FullName as fullName,
            u.PhoneNumber as phoneNumber,
            r.TotalAmount as totalAmount,
            t.Status as status,
            r.ReceiptID as receiptId,
            t.TransactionID as transactionId
        FROM Receipt r
        INNER JOIN [Transaction] t ON r.TransactionID = t.TransactionID
        INNER JOIN Appointment a ON t.AppointmentID = a.AppointmentID
        INNER JOIN Patient p ON a.PatientID = p.PatientID
        INNER JOIN Users u ON p.UserID = u.UserID
        ORDER BY r.IssuedDate DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getAllReceiptsWithTransactionData();

    /**
     * Get all patients regardless of payment data (for basic patient list)
     */
    @Query(value = """
        SELECT TOP 20
            p.PatientID as patientId,
            u.FullName as fullName,
            u.PhoneNumber as phoneNumber,
            CAST(0 as decimal(10,2)) as totalAmount,
            'Pending' as status,
            0 as receiptId,
            0 as transactionId
        FROM Patient p
        INNER JOIN Users u ON p.UserID = u.UserID
        ORDER BY u.FullName
        """, nativeQuery = true)
    List<Map<String, Object>> getAllPatientsWithPaymentInfo();

    /**
     * Get receipt by transaction ID
     */
    @Query(value = """
        SELECT 
            r.ReceiptID as receiptId,
            r.TotalAmount as totalAmount,
            r.IssuedDate as issuedDate,
            t.TransactionID as transactionId,
            t.Status as status
        FROM Receipt r
        INNER JOIN [Transaction] t ON r.TransactionID = t.TransactionID
        WHERE r.TransactionID = :transactionId
        """, nativeQuery = true)
    List<Map<String, Object>> getReceiptByTransactionId(@Param("transactionId") Integer transactionId);
}
