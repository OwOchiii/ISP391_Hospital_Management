package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orochi.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    /**
     * Find transactions by appointment ID ordered by payment time descending
     * This method fetches transactions without loading the full appointment entity
     * to avoid SQL grammar exceptions
     */
    @Query(value = "SELECT * FROM [Transaction] WHERE AppointmentID = :appointmentId ORDER BY TimeOfPayment DESC", nativeQuery = true)
    List<Transaction> findByAppointmentIdOrderByTimeOfPaymentDesc(@Param("appointmentId") Integer appointmentId);

    /**
     * Find transactions by appointment ID
     */
    @Query(value = "SELECT * FROM [Transaction] WHERE AppointmentID = :appointmentId", nativeQuery = true)
    List<Transaction> findByAppointmentId(@Param("appointmentId") Integer appointmentId);

    /**
     * Find transactions by user ID
     */
    @Query(value = "SELECT * FROM [Transaction] WHERE UserID = :userId", nativeQuery = true)
    List<Transaction> findByUserId(@Param("userId") Integer userId);

    /**
     * Find transactions by status
     */
    @Query(value = "SELECT * FROM [Transaction] WHERE Status = :status", nativeQuery = true)
    List<Transaction> findByStatus(@Param("status") String status);

    /**
     * Find transactions by method (payment method)
     */
    @Query(value = "SELECT * FROM [Transaction] WHERE Method = :method", nativeQuery = true)
    List<Transaction> findByMethod(@Param("method") String method);

    List<Transaction> findByUserIdOrderByTimeOfPaymentDesc(Integer userId);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
            "AND t.status IN :statuses " +
            "AND (:method IS NULL OR t.method = :method) " +
            "AND (:searchId IS NULL OR t.transactionId = :searchId OR t.appointmentId = :searchId) " +
            "AND (:start IS NULL OR t.timeOfPayment >= :start) " +
            "AND (:end IS NULL OR t.timeOfPayment <= :end)")
    Page<Transaction> findFilteredTransactions(
            @Param("userId") Integer userId,
            @Param("statuses") List<String> statuses,
            @Param("method") String method,
            @Param("searchId") Integer searchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
