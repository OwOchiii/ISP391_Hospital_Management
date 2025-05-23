package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import orochi.model.MedicalOrder;

import java.util.List;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Integer> {

    // Find orders by appointment ID
    List<MedicalOrder> findByAppointmentIdOrderByOrderDateDesc(Integer appointmentId);

    // Find orders created by a specific doctor
    List<MedicalOrder> findByOrderByIdOrderByOrderDateDesc(Integer doctorId);

    // Find pending orders for a specific doctor
    List<MedicalOrder> findByOrderByIdAndStatusOrderByOrderDateDesc(Integer doctorId, String status);
}