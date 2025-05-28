package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.MedicalOrder;

import java.util.List;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Integer> {

    // Find orders by appointment ID
    List<MedicalOrder> findByAppointmentIdOrderByOrderDate(Integer appointmentId);

    // Find orders created by a specific doctor with a particular status
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId AND m.status = :status ORDER BY m.orderDate")
    List<MedicalOrder> findByDoctorIdAndStatus(@Param("doctorId") Integer doctorId, @Param("status") String status);

    // Find all orders for a specific doctor
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId ORDER BY m.orderDate")
    List<MedicalOrder> findByDoctorId(@Param("doctorId") Integer doctorId);
}

