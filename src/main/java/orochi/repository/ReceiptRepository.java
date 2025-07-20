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

    /**
     * Get payment history data with all required fields according to user requirements
     * Patient ID from Patient table
     * Patient Name from Users table with RoleID = 4
     * Phone from Users.PhoneNumber
     * Appointment ID and DateTime from Appointment table
     * Status from Transaction table where Status = 'Paid' OR 'Refunded'
     * Method from Transaction table
     * Amount from Receipt.TotalAmount
     */
    @Query(value = """
        SELECT 
            p.PatientID as patientId,
            u.FullName as patientName,
            u.PhoneNumber as phone,
            a.AppointmentID as appointmentId,
            a.DateTime as dateTime,
            t.Status as status,
            t.Method as method,
            r.TotalAmount as amount,
            t.TransactionID as transactionId,
            r.ReceiptID as receiptId,
            t.TimeOfPayment as timeOfPayment
        FROM Patient p
        INNER JOIN Users u ON p.UserID = u.UserID AND u.RoleID = 4
        INNER JOIN Appointment a ON p.PatientID = a.PatientID
        INNER JOIN [Transaction] t ON a.AppointmentID = t.AppointmentID
        INNER JOIN Receipt r ON t.TransactionID = r.TransactionID
        WHERE t.Status IN ('Paid', 'Refunded')
        ORDER BY a.DateTime DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getPaymentHistoryData();

    /**
     * Get payment history data with date filter - Updated query to include Refunded
     */
    @Query(value = """
        SELECT 
            p.PatientID as patientId,
            u.FullName as patientName,
            u.PhoneNumber as phone,
            a.AppointmentID as appointmentId,
            a.DateTime as dateTime,
            t.Status as status,
            t.Method as method,
            r.TotalAmount as amount,
            t.TransactionID as transactionId,
            r.ReceiptID as receiptId,
            t.TimeOfPayment as timeOfPayment
        FROM Patient p
        INNER JOIN Users u ON p.UserID = u.UserID AND u.RoleID = 4
        INNER JOIN Appointment a ON p.PatientID = a.PatientID
        INNER JOIN [Transaction] t ON a.AppointmentID = t.AppointmentID
        INNER JOIN Receipt r ON t.TransactionID = r.TransactionID
        WHERE t.Status IN ('Paid', 'Refunded')
        AND CAST(a.DateTime AS DATE) BETWEEN :fromDate AND :toDate
        ORDER BY a.DateTime DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getPaymentHistoryDataByDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    /**
     * Test query to check data availability with Status = 'Paid' or 'Refunded' filter
     */
    @Query(value = """
        SELECT 
            p.PatientID as patientId,
            u.FullName as patientName,
            u.PhoneNumber as phone,
            a.AppointmentID as appointmentId,
            a.DateTime as dateTime,
            t.Status as status,
            t.Method as method,
            r.TotalAmount as amount,
            t.TransactionID as transactionId,
            r.ReceiptID as receiptId,
            t.TimeOfPayment as timeOfPayment
        FROM Patient p
        INNER JOIN Users u ON p.UserID = u.UserID AND u.RoleID = 4
        INNER JOIN Appointment a ON p.PatientID = a.PatientID
        INNER JOIN [Transaction] t ON a.AppointmentID = t.AppointmentID
        INNER JOIN Receipt r ON t.TransactionID = r.TransactionID
        WHERE t.Status IN ('Paid', 'Refunded')
        ORDER BY a.DateTime DESC;
        """, nativeQuery = true)
    List<Map<String, Object>> getAllPaymentHistoryData();
}
