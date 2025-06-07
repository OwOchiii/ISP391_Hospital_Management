package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.MedicalOrder;

import java.util.List;
import java.util.Optional;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Integer> {

    // Find orders by appointment ID
    List<MedicalOrder> findByAppointmentIdOrderByOrderDate(Integer appointmentId);

    // Paginated version
    Page<MedicalOrder> findByAppointmentId(Integer appointmentId, Pageable pageable);

    // Find orders created by a specific doctor with a particular status
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId AND m.status = :status ORDER BY m.orderDate")
    List<MedicalOrder> findByDoctorIdAndStatus(@Param("doctorId") Integer doctorId, @Param("status") String status);

    // Paginated version
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId AND m.status = :status")
    Page<MedicalOrder> findByDoctorIdAndStatus(@Param("doctorId") Integer doctorId, @Param("status") String status, Pageable pageable);

    // Find all orders for a specific doctor
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId ORDER BY m.orderDate")
    List<MedicalOrder> findByDoctorId(@Param("doctorId") Integer doctorId);

    // Paginated version
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId")
    Page<MedicalOrder> findByDoctorId(@Param("doctorId") Integer doctorId, Pageable pageable);

    List<MedicalOrder> findByAppointmentPatientIdOrderByOrderDateDesc(Integer patientId);

    // Optimized query to fetch medical order with all relationships in one query
    @Query("SELECT m FROM MedicalOrder m " +
           "LEFT JOIN FETCH m.appointment a " +
           "LEFT JOIN FETCH a.patient p " +
           "LEFT JOIN FETCH a.doctor d " +
           "LEFT JOIN FETCH d.user " +
           "WHERE m.orderId = :orderId")
    Optional<MedicalOrder> findByIdWithDetails(@Param("orderId") Integer orderId);

    // Optimized method to directly update order status without loading the entity
    @Modifying
    @Query("UPDATE MedicalOrder m SET m.status = :status WHERE m.orderId = :orderId")
    int updateOrderStatusById(@Param("orderId") Integer orderId, @Param("status") String status);

    // Check if a doctor is associated with any medical orders for a specific appointment
    @Query("SELECT COUNT(m) > 0 FROM MedicalOrder m WHERE m.appointment.appointmentId = :appointmentId AND m.doctorId = :doctorId")
    boolean existsByAppointmentIdAndDoctorId(@Param("appointmentId") Integer appointmentId, @Param("doctorId") Integer doctorId);

    // Alternative query that checks if a doctor has access to an appointment through medical orders
    @Query("SELECT COUNT(m) > 0 FROM MedicalOrder m JOIN m.appointment a WHERE a.appointmentId = :appointmentId AND m.doctorId = :doctorId")
    boolean checkDoctorHasAccessToAppointment(@Param("appointmentId") Integer appointmentId, @Param("doctorId") Integer doctorId);

    List<MedicalOrder> findByResultId(Integer resultId);
}

